package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.interstitial.unitgroup.api.CustomInterstitialAdapter;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlexISInterstitialAdapter extends CustomInterstitialAdapter implements AlexISEventManager.LoadEventListener, AlexISEventManager.ImpressionEventListener {

    private static final String TAG = AlexISInterstitialAdapter.class.getSimpleName();
    String mPlacementName = "";
    ImpressionData mImpressionData;
    Map<String, Object> extraMap;

    String mILRD;
    boolean isC2SBidding;

    @Override
    public void loadCustomNetworkAd(Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        if (!(context instanceof Activity)) {
            notifyATLoadFail("", "Ironsource Mediation: context must be activity");
            return;
        }

        String mediationPlacementId = (String) serverExtra.get("plid");
        if (mediationPlacementId != null) {
            mPlacementName = mediationPlacementId;
        }

        if (TextUtils.isEmpty(mPlacementName)) {
            notifyATLoadFail("", "ironsource: plid can not be empty");
            return;
        }

        AlexISInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
                startLoadAd();
            }

            @Override
            public void onFail(String errorMsg) {
                notifyATLoadFail("", errorMsg);
            }
        });
    }

    private void startLoadAd() {
        AlexISEventManager.getInstance().addLoadListenerForInter(this);

        IronSource.loadInterstitial();
    }

    @Override
    public void destory() {

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
    public boolean isAdReady() {
        return IronSource.isInterstitialReady();
    }

    @Override
    public void show(Activity activity) {
        AlexISEventManager.getInstance().registerForInter(mPlacementName, this);

        IronSource.showInterstitial(mPlacementName);
    }

    @Override
    public void notifyLoaded(final AdInfo adInfo) {

        if (isC2SBidding && mBiddingListener != null) {
            runOnNetworkRequestThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        double price = 0d;
                        if (adInfo != null) {
                            price = adInfo.getRevenue() * 1000;//ecpm
                        }

                        if (mBiddingListener != null) {
                            mBiddingListener.onC2SBiddingResultWithCache(ATBiddingResult.success(price, UUID.randomUUID().toString(), null), null);
                            mBiddingListener = null;
                        }
                    } catch (Throwable e) {
                        if (mBiddingListener != null) {
                            mBiddingListener.onC2SBidResult(ATBiddingResult.fail("Ironsource Mediation: " + e.getMessage()));
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
    public void notifyLoadFail(String code, String msg) {
        notifyATLoadFail(code, msg);
    }


    @Override
    public void onCallbackImpressionData(ImpressionData impressionData) {
        mImpressionData = impressionData;

        fillExtraInfo(impressionData);
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
        }
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
    public void notifyShow() {
        if (mImpressListener != null) {
            mImpressListener.onInterstitialAdShow();
        }
    }

    @Override
    public void notifyPlayStart() {

    }

    @Override
    public void notifyClick() {
        if (mImpressListener != null) {
            mImpressListener.onInterstitialAdClicked();
        }
    }

    @Override
    public void notifyClose() {
        if (mImpressListener != null) {
            mImpressionData = null;
            mImpressListener.onInterstitialAdClose();
        }
    }

    @Override
    public void notifyPlayEnd() {

    }

    @Override
    public void notifyReward() {

    }

    @Override
    public void notifyPlayFail(String code, String msg) {
        Log.e(TAG, "notifyPlayFail: " + code + ", " + msg);
        if (mImpressListener != null) {
            mImpressListener.onInterstitialAdVideoError(code, msg);
        }
    }

    @Override
    public boolean startBiddingRequest(Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        mBiddingListener = biddingListener;
        isC2SBidding = true;
        loadCustomNetworkAd(context, serverExtra, localExtra);

        return true;
    }
}
