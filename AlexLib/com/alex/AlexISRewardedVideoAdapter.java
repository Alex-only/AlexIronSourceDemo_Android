package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.rewardvideo.unitgroup.api.CustomRewardVideoAdapter;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlexISRewardedVideoAdapter extends CustomRewardVideoAdapter implements AlexISEventManager.LoadEventListener, AlexISEventManager.ImpressionEventListener {

    private static final String TAG = AlexISRewardedVideoAdapter.class.getSimpleName();
    String mPlacementName = "";
    ImpressionData mImpressionData;

    Map<String, Object> extraMap;

    String mILRD;
    boolean isC2SBidding;


    @Override
    public void loadCustomNetworkAd(Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra) {

        String mediationPlacementId = (String) serverExtra.get("plid");
        if (mediationPlacementId != null) {
            mPlacementName = mediationPlacementId;
        }


        if (TextUtils.isEmpty(mPlacementName)) {
            notifyATLoadFail("", "Ironsource Mediation: plid can not be empty");
            return;
        }

        if (localExtra != null) {
            try {
                String userId = localExtra.get(ATAdConst.KEY.USER_ID).toString();

                IronSource.setDynamicUserId(userId);
            } catch (Throwable e) {

            }
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
        AlexISEventManager.getInstance().addLoadListenerForRv(this);

        IronSource.loadRewardedVideo();
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
        return IronSource.isRewardedVideoAvailable();
    }

    @Override
    public void show(Activity activity) {
        AlexISEventManager.getInstance().registerForRV(mPlacementName, this);

        IronSource.showRewardedVideo(mPlacementName);
    }

    @Override
    public void notifyLoaded(AdInfo adInfo) {
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

    }

    @Override
    public void notifyPlayStart() {
        if (mImpressionListener != null) {
            mImpressionListener.onRewardedVideoAdPlayStart();
        }
    }

    @Override
    public void notifyClick() {
        if (mImpressionListener != null) {
            mImpressionListener.onRewardedVideoAdPlayClicked();
        }
    }

    @Override
    public void notifyClose() {
        if (mImpressionListener != null) {
            mImpressionData = null;
            mImpressionListener.onRewardedVideoAdClosed();
        }
    }

    @Override
    public void notifyPlayEnd() {
        if (mImpressionListener != null) {
            mImpressionListener.onRewardedVideoAdPlayEnd();
        }
    }

    @Override
    public void notifyReward() {
        if (mImpressionListener != null) {
            mImpressionListener.onReward();
        }
    }

    @Override
    public void notifyPlayFail(String code, String msg) {
        Log.e(TAG, "notifyPlayFail: " + code + ", " + msg);
        if (mImpressionListener != null) {
            mImpressionListener.onRewardedVideoAdPlayFailed(code, msg);
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
