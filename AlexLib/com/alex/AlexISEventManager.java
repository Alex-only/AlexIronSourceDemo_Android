package com.alex;

import android.util.Log;

import com.anythink.core.api.ATSDK;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.impressionData.ImpressionData;
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoManualListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AlexISEventManager {

    private static final String TAG = AlexISEventManager.class.getSimpleName();

    private volatile static AlexISEventManager sInstance;


    private final ConcurrentHashMap<String, ImpressionEventListener> mListenerMapForRv;
    private final ConcurrentHashMap<String, ImpressionEventListener> mListenerMapForInter;
    private final ConcurrentHashMap<String, ImpressionEventListener> mListenerMapForBanner;

    private final List<LoadEventListener> mLoadListenerMapForInter;
    private final List<LoadEventListener> mLoadListenerMapForRv;
    private final Object mLoadListenerLockForRv = new Object();
    private final Object mLoadListenerLockForInter = new Object();

    private String mRecordRewardVideoKey;
    private String mRecordInterstitialKey;


    private boolean hasInit;

    private AlexISEventManager() {
        mListenerMapForRv = new ConcurrentHashMap<>(3);
        mListenerMapForInter = new ConcurrentHashMap<>(3);
        mListenerMapForBanner = new ConcurrentHashMap<>(3);

        mLoadListenerMapForRv = new ArrayList<>(3);
        mLoadListenerMapForInter = new ArrayList<>(3);
    }

    public static AlexISEventManager getInstance() {
        if (sInstance == null) {
            synchronized (AlexISEventManager.class) {
                if (sInstance == null)
                    sInstance = new AlexISEventManager();
            }
        }
        return sInstance;
    }

    public void init() {

        if (hasInit) {
            return;
        }

        hasInit = true;


//        IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
//            @Override
//            public void onAdAvailable(AdInfo adInfo) {
//                AlexISInitManager.getInstance().saveAdInfoForRV(adInfo);
//                if (ATSDK.isNetworkLogDebug()) {
//                    Log.e(TAG, "onAdAvailable: ----------" + adInfo.toString());
//                }
//            }
//
//            @Override
//            public void onAdUnavailable() {
//                AlexISInitManager.getInstance().saveAdInfoForRV(null);
//                if (ATSDK.isNetworkLogDebug()) {
//                    Log.e(TAG, "onAdUnavailable: ----------");
//                }
//            }
//
//            @Override
//            public void onAdOpened(AdInfo adInfo) {
//                //for NPE
//                if (mRecordRewardVideoKey == null) {
//                    return;
//                }
//                ImpressionEventListener eventListener = mListenerMapForRv.get(mRecordRewardVideoKey);
//                if (eventListener != null) {
//                    eventListener.notifyPlayStart();
//                }
//            }
//
//            @Override
//            public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
//                //for NPE
//                if (mRecordRewardVideoKey == null) {
//                    return;
//                }
//                ImpressionEventListener eventListener = mListenerMapForRv.remove(mRecordRewardVideoKey);
//                if (eventListener != null) {
//                    eventListener.notifyPlayFail(ironSourceError.getErrorCode() + "", ironSourceError.getErrorMessage());
//                }
//            }
//
//            @Override
//            public void onAdClicked(Placement placement, AdInfo adInfo) {
//                //for NPE
//                if (mRecordRewardVideoKey == null) {
//                    return;
//                }
//                ImpressionEventListener eventListener = mListenerMapForRv.get(mRecordRewardVideoKey);
//                if (eventListener != null) {
//                    eventListener.notifyClick();
//                }
//            }
//
//            @Override
//            public void onAdRewarded(Placement placement, AdInfo adInfo) {
//                //for NPE
//                if (mRecordRewardVideoKey == null) {
//                    return;
//                }
//                ImpressionEventListener eventListener = mListenerMapForRv.get(mRecordRewardVideoKey);
//                if (eventListener != null) {
//                    eventListener.notifyReward();
//                }
//            }
//
//            @Override
//            public void onAdClosed(AdInfo adInfo) {
//                //for NPE
//                if (mRecordRewardVideoKey == null) {
//                    return;
//                }
//                ImpressionEventListener eventListener = mListenerMapForRv.remove(mRecordRewardVideoKey);//need remove
//                if (eventListener != null) {
//                    eventListener.notifyClose();
//                }
//            }
//        });


        IronSource.setLevelPlayRewardedVideoManualListener(new LevelPlayRewardedVideoManualListener() {

            @Override
            public void onAdReady(AdInfo adInfo) {
                if (ATSDK.isNetworkLogDebug()) {
                    Log.e(TAG, "onAdReady: ----------" + adInfo.toString());
                }
                synchronized (mLoadListenerLockForRv) {
                    Iterator<LoadEventListener> iterator = mLoadListenerMapForRv.iterator();

                    while (iterator.hasNext()) {
                        LoadEventListener eventListener = iterator.next();
                        if (eventListener != null) {
                            eventListener.notifyLoaded(adInfo);
                        }
                        iterator.remove();
                    }
                }

            }

            @Override
            public void onAdLoadFailed(IronSourceError ironSourceError) {
                synchronized (mLoadListenerLockForRv) {
                    Iterator<LoadEventListener> iterator = mLoadListenerMapForRv.iterator();

                    while (iterator.hasNext()) {
                        LoadEventListener eventListener = iterator.next();
                        if (eventListener != null) {
                            eventListener.notifyLoadFail(ironSourceError.getErrorCode() + "", ironSourceError.getErrorMessage());
                        }
                        iterator.remove();
                    }
                }
            }

            @Override
            public void onAdOpened(AdInfo adInfo) {
                //for NPE
                if (mRecordRewardVideoKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForRv.get(mRecordRewardVideoKey);
                if (eventListener != null) {
                    eventListener.notifyPlayStart();
                }
            }

            @Override
            public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
                //for NPE
                if (mRecordRewardVideoKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForRv.remove(mRecordRewardVideoKey);
                if (eventListener != null) {
                    eventListener.notifyPlayFail(ironSourceError.getErrorCode() + "", ironSourceError.getErrorMessage());
                }
            }

            @Override
            public void onAdClicked(Placement placement, AdInfo adInfo) {
                //for NPE
                if (mRecordRewardVideoKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForRv.get(mRecordRewardVideoKey);
                if (eventListener != null) {
                    eventListener.notifyClick();
                }
            }

            @Override
            public void onAdRewarded(Placement placement, AdInfo adInfo) {
                //for NPE
                if (mRecordRewardVideoKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForRv.get(mRecordRewardVideoKey);
                if (eventListener != null) {
                    eventListener.notifyPlayEnd();
                }
                if (eventListener != null) {
                    eventListener.notifyReward();
                }
            }

            @Override
            public void onAdClosed(AdInfo adInfo) {
                //for NPE
                if (mRecordRewardVideoKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForRv.remove(mRecordRewardVideoKey);//need remove
                if (eventListener != null) {
                    eventListener.notifyClose();
                }
            }
        });


        IronSource.setLevelPlayInterstitialListener(new LevelPlayInterstitialListener() {

            @Override
            public void onAdReady(AdInfo adInfo) {
                if (ATSDK.isNetworkLogDebug()) {
                    Log.e(TAG, "onAdReady: ----------" + adInfo.toString());
                }
                synchronized (mLoadListenerLockForInter) {
                    Iterator<LoadEventListener> iterator = mLoadListenerMapForInter.iterator();

                    while (iterator.hasNext()) {
                        LoadEventListener eventListener = iterator.next();
                        if (eventListener != null) {
                            eventListener.notifyLoaded(adInfo);
                        }
                        iterator.remove();
                    }
                }
            }

            @Override
            public void onAdLoadFailed(IronSourceError ironSourceError) {
                synchronized (mLoadListenerLockForInter) {
                    Iterator<LoadEventListener> iterator = mLoadListenerMapForInter.iterator();

                    while (iterator.hasNext()) {
                        LoadEventListener eventListener = iterator.next();
                        if (eventListener != null) {
                            eventListener.notifyLoadFail(ironSourceError.getErrorCode() + "", ironSourceError.getErrorMessage());
                        }
                        iterator.remove();
                    }
                }
            }

            @Override
            public void onAdOpened(AdInfo adInfo) {

            }

            @Override
            public void onAdShowSucceeded(AdInfo adInfo) {
                //for NPE
                if (mRecordInterstitialKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForInter.get(mRecordInterstitialKey);
                if (eventListener != null) {
                    eventListener.notifyShow();
                }
            }

            @Override
            public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
                //for NPE
                if (mRecordInterstitialKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForInter.get(mRecordInterstitialKey);
                if (eventListener != null) {
                    eventListener.notifyPlayFail(ironSourceError.getErrorCode() + "", ironSourceError.getErrorMessage());
                }
            }

            @Override
            public void onAdClicked(AdInfo adInfo) {
                //for NPE
                if (mRecordInterstitialKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForInter.get(mRecordInterstitialKey);
                if (eventListener != null) {
                    eventListener.notifyClick();
                }
            }

            @Override
            public void onAdClosed(AdInfo adInfo) {
                //for NPE
                if (mRecordInterstitialKey == null) {
                    return;
                }
                ImpressionEventListener eventListener = mListenerMapForInter.remove(mRecordInterstitialKey);//need remove
                if (eventListener != null) {
                    eventListener.notifyClose();
                }
            }
        });


        IronSource.addImpressionDataListener(new ImpressionDataListener() {
            @Override
            public void onImpressionSuccess(ImpressionData impressionData) {
                if (ATSDK.isNetworkLogDebug()) {
                    Log.e(TAG, "onImpressionSuccess: " + impressionData.toString());
                }

                //impression callback
                ImpressionEventListener eventListener = null;
                String adUnit = impressionData.getAdUnit();
                if ("rewarded_video".equalsIgnoreCase(adUnit)) {
                    eventListener = mListenerMapForRv.get(impressionData.getPlacement());
                } else if ("banner".equalsIgnoreCase(adUnit)) {
                    eventListener = mListenerMapForBanner.get(impressionData.getPlacement());
                } else if ("interstitial".equalsIgnoreCase(adUnit)) {
                    eventListener = mListenerMapForInter.get(impressionData.getPlacement());
                }
                if (eventListener != null) {
                    eventListener.onCallbackImpressionData(impressionData);
                } else {
                    if (ATSDK.isNetworkLogDebug()) {
                        Log.e(TAG, "onImpressionSuccess but not found placementName: " + impressionData.getPlacement());
                    }
                }

            }
        });

    }


    public void registerForRV(String key, ImpressionEventListener eventListener) {
        mRecordRewardVideoKey = key;
        mListenerMapForRv.put(key, eventListener);
    }

    public void registerForInter(String key, ImpressionEventListener eventListener) {
        mRecordInterstitialKey = key;
        mListenerMapForInter.put(key, eventListener);
    }

    public void registerForBanner(String key, ImpressionEventListener eventListener) {
        mListenerMapForBanner.put(key, eventListener);
    }

    public void addLoadListenerForRv(LoadEventListener eventListener) {
        synchronized (mLoadListenerLockForRv) {
            mLoadListenerMapForRv.add(eventListener);
        }
    }

    public void addLoadListenerForInter(LoadEventListener eventListener) {
        synchronized (mLoadListenerLockForInter) {
            mLoadListenerMapForInter.add(eventListener);
        }
    }


    public interface LoadEventListener {
        void notifyLoaded(AdInfo adInfo);

        void notifyLoadFail(String code, String msg);
    }


    public interface ImpressionEventListener {

        void onCallbackImpressionData(ImpressionData impressionData);

        void notifyShow();

        void notifyPlayStart();

        void notifyClick();

        void notifyClose();

        void notifyPlayEnd();

        void notifyReward();

        void notifyPlayFail(String code, String msg);
    }

}
