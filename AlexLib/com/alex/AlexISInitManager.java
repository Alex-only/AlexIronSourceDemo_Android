package com.alex;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.MediationInitCallback;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.integration.IntegrationHelper;
import com.ironsource.mediationsdk.sdk.InitializationListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlexISInitManager extends ATInitMediation {

    boolean isLogDebug;
    boolean hasInit;
    private boolean mIsIniting;

    private List<MediationInitCallback> mListeners;
    private final Object mLock = new Object();
    private volatile static AlexISInitManager sInstance;
    private AdInfo adInfo;

    private Boolean mBannerHasBeenLoaded = false;

    public static AlexISInitManager getInstance() {
        if (sInstance == null) {
            synchronized (AlexISInitManager.class) {
                if (sInstance == null)
                    sInstance = new AlexISInitManager();
            }
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> serviceExtras, MediationInitCallback callback) {

        if (hasInit) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }


        synchronized (mLock) {

            if (mListeners == null) {
                mListeners = new ArrayList<>();
            }

            if (callback != null) {
                mListeners.add(callback);
            }

            if (mIsIniting) {
                return;
            }
        }

        mIsIniting = true;


        String appKey = "";
        try {
            appKey = (String) serviceExtras.get("sdk_key");
        } catch (Throwable e) {

        }

        if (TextUtils.isEmpty(appKey)) {
            callbackResult(false, "Ironsource: sdk_key can not be empty");
            return;
        }

        if (ATSDK.isNetworkLogDebug()) {
            IronSource.setAdaptersDebug(true);
        }


        AlexISEventManager.getInstance().init();


        if (isLogDebug) {
            IntegrationHelper.validateIntegration(context);
        }

        IronSource.init(context, appKey, new InitializationListener() {
            @Override
            public void onInitializationComplete() {
                Log.i("AlexISInitManager", "onInitializationComplete");
                hasInit = true;

                callbackResult(true, "");
            }
        });
    }


    private void callbackResult(boolean success, String errorMsg) {
        synchronized (mLock) {
            int size = mListeners.size();
            MediationInitCallback initListener;
            for (int i = 0; i < size; i++) {
                initListener = mListeners.get(i);
                if (initListener != null) {
                    if (success) {
                        initListener.onSuccess();
                    } else {
                        initListener.onFail(errorMsg);
                    }
                }
            }
            mIsIniting = false;
            mListeners.clear();
        }
    }


    @Override
    public String getNetworkName() {
        return "Ironsource Mediation";
    }

    @Override
    public String getNetworkVersion() {
        return AlexISConst.getNetworkVersion();
    }

    @Override
    public String getNetworkSDKClass() {
        return "com.ironsource.mediationsdk.IronSource";
    }


    synchronized void saveAdInfoForRV(AdInfo adInfo) {
        this.adInfo = adInfo;
    }

    synchronized AdInfo popAdInfoForRv() {
        AdInfo result = this.adInfo;
        this.adInfo = null;

        return result;
    }

    /**
     * ISMediation Banner only needs one banner ad to initiate loading or display, so it needs to add a method to judge
     * @param loaded
     */
    public void setNeedInterceptBannerLoad(boolean loaded) {
        synchronized (mBannerHasBeenLoaded) {
            mBannerHasBeenLoaded = loaded;
        }
    }

    protected boolean needInterceptBannerLoad() {
        synchronized (mBannerHasBeenLoaded) {
            return mBannerHasBeenLoaded;
        }
    }

    void fillAdInfo(String placementId, Map<String, Object> extraMap, ImpressionData impressionData) {
        if (extraMap == null || impressionData == null) {
            return;
        }

        JSONObject allData = impressionData.getAllData();
        if (allData != null) {
            extraMap.put(AlexISConst.KEY_AD_INFO, allData.toString());
        }

        extraMap.put(AlexISConst.KEY_REVENUE, impressionData.getRevenue());
        extraMap.put(AlexISConst.KEY_AD_UNIT_ID, placementId);
        extraMap.put(AlexISConst.KEY_FORMAT, impressionData.getAdUnit());
        extraMap.put(AlexISConst.KEY_NETWORK_NAME, impressionData.getAdNetwork());
        extraMap.put(AlexISConst.KEY_PLACEMENT, impressionData.getPlacement());
        extraMap.put(AlexISConst.KEY_COUNTRY_CODE, impressionData.getCountry());

        extraMap.put(AlexISConst.KEY_NETWORK_PLACEMENT_ID, impressionData.getInstanceId());
    }

}
