package com.alex;

import static com.anythink.core.api.ATInitMediation.getStringFromMap;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.anythink.banner.unitgroup.api.CustomBannerAdapter;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlexISBannerAdapter extends CustomBannerAdapter implements AlexISEventManager.LoadEventListener, AlexISEventManager.ImpressionEventListener {

    private static final String TAG = AlexISBannerAdapter.class.getSimpleName();

    private static final Object mLock = new Object();
    String mPlacementName = "";
    ImpressionData mImpressionData;
    Map<String, Object> extraMap;

    String mILRD;
    boolean isC2SBidding;
    IronSourceBannerLayout ironSourceBannerLayout;

    @Override
    public View getBannerView() {
        AlexISEventManager.getInstance().registerForBanner(mPlacementName, this);
        return ironSourceBannerLayout;
    }

    @Override
    public void loadCustomNetworkAd(Context context, Map<String, Object> serverExtra,
                                    Map<String, Object> localExtra) {
        if (!(context instanceof Activity)) {
            notifyATLoadFail("", "Ironsource Mediation: context must be activity");
            return;
        }
        String mediationPlacementId = (String) serverExtra.get("plid");
        if (mediationPlacementId != null) {
            mPlacementName = mediationPlacementId;
        }

        if (TextUtils.isEmpty(mPlacementName)) {
            notifyATLoadFail("", "Ironsource Mediation: plid can not be empty");
            return;
        }

        AlexISInitManager.getInstance()
                .initSDK(context, serverExtra, new MediationInitCallback() {
                    @Override
                    public void onSuccess() {
                        synchronized (mLock) {
                            //When a ISMediation Banner is loading or showing, other ISMediation Banners cannot initiate load
                            if (AlexISInitManager.getInstance().needInterceptBannerLoad()) {
                                notifyATLoadFail("",
                                        "Ironsource Mediation: banner has been loading or showing");
                                return;
                            }
                            AlexISInitManager.getInstance().setNeedInterceptBannerLoad(true);
                            startLoadAd((Activity) context, serverExtra);
                        }
                    }

                    @Override
                    public void onFail(String errorMsg) {
                        notifyATLoadFail("", errorMsg);
                    }
                });
    }

    private void startLoadAd(Activity activity, Map<String, Object> serverExtras) {
        // choose banner size

        String size = getStringFromMap(serverExtras, "size");
        ISBannerSize bannerSize;
        switch (size) {
            case "320x90":
                bannerSize = ISBannerSize.LARGE;
                break;
            case "300x250":
                bannerSize = ISBannerSize.RECTANGLE;
                break;
            case "smart":
                bannerSize = ISBannerSize.SMART;
                break;
            case "320x50":
            default:
                bannerSize = ISBannerSize.BANNER;
                break;
        }

        // instantiate IronSourceBanner object, using the IronSource.createBanner API
        ironSourceBannerLayout = IronSource.createBanner(activity, bannerSize);

        ironSourceBannerLayout.setLevelPlayBannerListener(new LevelPlayBannerListener() {
            @Override
            public void onAdLoaded(AdInfo adInfo) {
                if (isC2SBidding && mBiddingListener != null) {
                    runOnNetworkRequestThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
//                        ImpressionData impressionData = ISMediationUtil.getImpressionData(ISMediationUtil.INTERSTITIAL);
                                double price = 0d;
                                if (adInfo != null) {
                                    price = adInfo.getRevenue() * 1000;//ecpm
                                }

                                if (mBiddingListener != null) {
                                    mBiddingListener.onC2SBiddingResultWithCache(
                                            ATBiddingResult.success(price,
                                                    UUID.randomUUID().toString(), null), null);
                                    mBiddingListener = null;
                                }
                            } catch (Throwable e) {
                                if (mBiddingListener != null) {
                                    mBiddingListener.onC2SBidResult(ATBiddingResult.fail(
                                            "Ironsource Mediation: " + e.getMessage()));
                                    mBiddingListener = null;
                                }
                            }
                        }
                    });
                } else {
                    //should not executed
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded();
                    }
                }
            }

            @Override
            public void onAdLoadFailed(IronSourceError ironSourceError) {
                AlexISInitManager.getInstance().setNeedInterceptBannerLoad(false);
                notifyLoadFail(
                        "" + ironSourceError.getErrorCode() + AlexISBannerAdapter.this.hashCode(),
                        ironSourceError.getErrorMessage());
            }

            @Override
            public void onAdClicked(AdInfo adInfo) {
                if (mImpressionEventListener != null) {
                    mImpressionEventListener.onBannerAdClicked();
                }
            }

            @Override
            public void onAdLeftApplication(AdInfo adInfo) {
            }

            @Override
            public void onAdScreenPresented(AdInfo adInfo) {
            }

            @Override
            public void onAdScreenDismissed(AdInfo adInfo) {
            }
        });
        // load ad into the created banner
        IronSource.loadBanner(ironSourceBannerLayout, mPlacementName);
    }

    @Override
    public void destory() {
        if (mImpressionData != null) {
            AlexISInitManager.getInstance().setNeedInterceptBannerLoad(false);
            IronSource.destroyBanner(ironSourceBannerLayout);
        }
    }

    @Override
    public String getNetworkPlacementId() {
        return mPlacementName;
    }

    @Override
    public String getNetworkSDKVersion() {
        return AlexISInitManager.getInstance().getNetworkVersion();
    }

    @Override
    public String getNetworkName() {
        return AlexISInitManager.getInstance().getNetworkName();
    }

    @Override
    public void notifyLoaded(AdInfo adInfo) {
    }

    @Override
    public void notifyLoadFail(String code, String msg) {
    }

    @Override
    public void onCallbackImpressionData(ImpressionData impressionData) {
        //IronSourceMediation Banner Will "Auto Refresh(can't control)" and trigger this callback
        mImpressionData = impressionData;

        fillExtraInfo(impressionData);

        if (mImpressionEventListener != null) {
            mImpressionEventListener.onBannerAdShow();
        }
    }

    @Override
    public void notifyShow() {

    }

    @Override
    public void notifyPlayStart() {

    }

    @Override
    public void notifyClick() {
    }

    @Override
    public void notifyClose() {
    }

    @Override
    public void notifyPlayEnd() {
    }

    @Override
    public void notifyReward() {
    }

    @Override
    public void notifyPlayFail(String code, String msg) {
    }

    @Override
    public Map<String, Object> getNetworkInfoMap() {
        return extraMap;
    }

    @Override
    public String getILRD() {
        return mILRD;
    }

    @Override
    public boolean startBiddingRequest(Context context, Map<String, Object> serverExtra,
                                       Map<String, Object> localExtra,
                                       ATBiddingListener biddingListener) {
        mBiddingListener = biddingListener;
        isC2SBidding = true;
        loadCustomNetworkAd(context, serverExtra, localExtra);

        return true;
    }

    private void fillExtraInfo(ImpressionData impressionData) {
        if (impressionData != null) {
            mILRD = impressionData.toString();
        }

        if (mILRD != null) {
//            mILRD = mILRD.replace("${PLACEMENT_NAME}", mPlacementName);
            if (extraMap == null) {
                extraMap = new HashMap<>(3);
            }

            extraMap.put(AlexISConst.KEY_IMPRESSION_DATA, mILRD);

            AlexISInitManager.getInstance().fillAdInfo(mPlacementName, extraMap, impressionData);
        }
    }
}
