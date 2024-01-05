package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.MediationInitCallback;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.integration.IntegrationHelper;

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
        if (!(context instanceof Activity)) {
            if (callback != null) {
                callback.onFail("Ironsource: please make sure to initialize the ironSource SDK with an Activity context");
            }

            return;
        }

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
            IntegrationHelper.validateIntegration(((Activity) context));
        }

        IronSource.init((Activity) context, appKey);
        hasInit = true;

        callbackResult(true, "");
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
}
