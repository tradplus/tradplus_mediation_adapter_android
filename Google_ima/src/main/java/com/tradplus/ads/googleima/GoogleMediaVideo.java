package com.tradplus.ads.googleima;

import static com.google.ads.interactivemedia.v3.api.UiElement.COUNTDOWN;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONTAINER_EMPTY;
import static com.tradplus.ads.base.common.TPError.CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.googleima.IMAConstant.IMA_CUSTOM_PARAMS;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.interactivemedia.v3.api.AdError;
import com.google.ads.interactivemedia.v3.api.AdProgressInfo;
import com.google.ads.interactivemedia.v3.api.UiElement;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.adapter.TPBaseAdapter;
import com.tradplus.ads.base.adapter.mediavideo.TPMediaVideoAdapter;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.common.util.LogUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GoogleMediaVideo extends TPMediaVideoAdapter {

    private static final String TAG = "GoogleMediaVideo";
    private ImaSdkFactory sdkFactory;
    private AdsLoader adsLoader;
    private AdsManager adsManager;
    private VideoAdPlayer mVideoPlayer;
    private String mLocalLanguage;
    private boolean mAutoPlayAd = false;
    private GoogleMediaVideoAd mGoolgeMediaVideoAd;
    private Integer mVideoMute = 1;
    private Integer mUICountDown = 1;
    private String mURl;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (mAdContainerView == null) {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONTAINER_EMPTY));
            return;
        }

        if (!(mVideoObject instanceof VideoAdPlayer)) {
            TPError tpError = new TPError(CONFIGURATION_ERROR);
            tpError.setErrorMessage("VideoObject not instanceof VideoAdPlayer");
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            String placemntId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (TextUtils.isEmpty(placemntId)) {
                TPError tpError = new TPError(CONFIGURATION_ERROR);
                tpError.setErrorMessage("placemntId is Empty");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                return;
            }

            mURl = placemntId;
        }

        if (userParams != null && userParams.size() > 0) {
            CheckLocalSetting(userParams);
            SupportPrivacyLaws(userParams, mURl);
        }

        CreateAdsLoader(context);
    }

    private void CreateAdsLoader(Context context) {
        mVideoPlayer = (VideoAdPlayer) mVideoObject;
        AdDisplayContainer adDisplayContainer = ImaSdkFactory.createAdDisplayContainer(mAdContainerView, mVideoPlayer);
        sdkFactory = ImaSdkFactory.getInstance();
        ImaSdkSettings imaSdkSettings = sdkFactory.createImaSdkSettings();
        imaSdkSettings.setDebugMode(TestDeviceUtil.getInstance().isNeedTestDevice());
        if (!TextUtils.isEmpty(mLocalLanguage)) {
            imaSdkSettings.setLanguage(mLocalLanguage);
        }
        imaSdkSettings.setAutoPlayAdBreaks(mAutoPlayAd);
        adsLoader = sdkFactory.createAdsLoader(context, imaSdkSettings, adDisplayContainer);

        requestAds(mURl);
        setAdsLoaderListener();

    }

    private void setAdsLoaderListener() {
        adsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                adsManager = adsManagerLoadedEvent.getAdsManager();

                if (adsManager == null) {
                    TPError tpError = new TPError(UNSPECIFIED);
                    tpError.setErrorMessage("onAdsManagerLoaded,but adsManager == null");
                    Log.i(TAG, "onAdsManagerLoaded,but adsManager == null");
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                Log.i(TAG, "onAdsManagerLoaded: ");
                adsManager.addAdErrorListener(new AdErrorEvent.AdErrorListener() {
                    @Override
                    public void onAdError(AdErrorEvent adErrorEvent) {
                        Log.i(TAG, "ShowFailed adsManager onAdError: ");
                        adShowFailed = 1;
                        TPError tpError = new TPError(SHOW_FAILED);
                        if (adErrorEvent != null) {
                            AdError error = adErrorEvent.getError();
                            int errorCode = error.getErrorCodeNumber();
                            String message = error.getMessage();
                            tpError.setErrorCode(errorCode + "");
                            tpError.setErrorMessage(message);
                            Log.i(TAG, "errorCode: " + errorCode + ", message: " + message);
                        }

                        adsManager.discardAdBreak();


                        if (mShowListener != null) {
                            mShowListener.onAdVideoError(tpError);
                        }
                    }
                });

                adsManager.addAdEventListener(adEventListener);

                AdsRenderingSettings adsRenderingSettings = sdkFactory.createAdsRenderingSettings();
                adsRenderingSettings.setEnablePreloading(true);
                if (mUICountDown != 1) {
                    Set<UiElement> uiElements = new HashSet<>();
                    uiElements.add(COUNTDOWN);
                    adsRenderingSettings.setUiElements(uiElements);
                }
                adsManager.init(adsRenderingSettings);
            }
        });
        adsLoader.addAdErrorListener(new AdErrorEvent.AdErrorListener() {
            @Override
            public void onAdError(AdErrorEvent adErrorEvent) {
                Log.i(TAG, "LoadFailed adsLoader onAdError: ");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (adErrorEvent != null) {
                    AdError error = adErrorEvent.getError();
                    int errorCode = error.getErrorCodeNumber();
                    String message = error.getMessage();
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(message);
                    Log.i(TAG, "errorCode: " + errorCode + ", message: " + message);
                }


                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });
    }

    private void requestAds(String adTagUrl) {
        if (adsManager != null) {
            adsManager.destroy();
        }
        AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdWillPlayMuted(mVideoMute == 1);
        request.setContentProgressProvider(new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (mVideoPlayer == null) return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                try {
                    return mVideoPlayer.getAdProgress();
                } catch (Throwable throwable) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
            }
        });


        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
    }

    int adShowFailed = 0;
    protected AdEvent.AdEventListener adEventListener = new AdEvent.AdEventListener() {
        @Override
        public void onAdEvent(AdEvent adEvent) {
            if (adEvent.getType() != AdEvent.AdEventType.AD_PROGRESS) {
                LogUtil.ownShow("onAdEvent === " + adEvent.getType(), "TradPlusAd");
            }

            switch (adEvent.getType()) {
                case LOADED:
                    if (mLoadAdapterListener != null) {
                        mGoolgeMediaVideoAd = new GoogleMediaVideoAd(adsManager);
                        mLoadAdapterListener.loadAdapterLoaded(mGoolgeMediaVideoAd);
                    }
                    Log.i(TAG, "onAdEvent: LOADED");
                    break;
                case CONTENT_PAUSE_REQUESTED:
//                    pauseContentForAds();
                    Log.i(TAG, "onAdEvent: CONTENT_PAUSE_REQUESTED");
                    break;
                case CONTENT_RESUME_REQUESTED:
//                    resumeContent();
                    Log.i(TAG, "onAdEvent: CONTENT_RESUME_REQUESTED");
                    break;
                case TAPPED:
                    if (mShowListener != null) {
                        mShowListener.onAdTapped();
                    }
                    Log.i(TAG, "onAdEvent: TAPPED");
                    break;
                case STARTED:
                    // Fired when an ad starts playing.
                    if (mShowListener != null) {
                        mShowListener.onAdVideoStart();
                        mShowListener.onAdShown();
                    }
                    Log.i(TAG, "onAdEvent: STARTED");
                    break;
                case PAUSED:
                    if (mShowListener != null) {
                        mShowListener.onAdPause();
                    }
                    Log.i(TAG, "onAdEvent: PAUSED");
                    break;
                case RESUMED:
                    if (mShowListener != null) {
                        mShowListener.onAdResume();
                    }
                    Log.i(TAG, "onAdEvent: RESUMED");
                    break;
                case SKIPPED:
                    if (mShowListener != null) {
                        mShowListener.onAdSkiped();
                    }
                    Log.i(TAG, "onAdEvent: SKIPPED");
                    break;
                case CLICKED:
                    if (mShowListener != null) {
                        mShowListener.onAdClicked();
                    }
                    Log.i(TAG, "onAdEvent: CLICKED");
                    break;
                case ALL_ADS_COMPLETED:
                    if (mShowListener != null && adShowFailed == 0) {
                        mShowListener.onAdVideoEnd();
                        mShowListener.onAdClosed();
                    }
                    Log.i(TAG, "onAdEvent: ALL_ADS_COMPLETED");
                    break;
                case AD_PROGRESS:

                    AdProgressInfo adProgressInfo = adsManager.getAdProgressInfo();
                    double adBreakDuration = 0;
                    if (adProgressInfo != null) {
                        adBreakDuration = adProgressInfo.getAdBreakDuration();
                    }

                    float currentTime = 0;
                    VideoProgressUpdate adProgress = adsManager.getAdProgress();
                    if (adProgress != null) {
                        currentTime = adProgress.getCurrentTime();
                    }

                    if (mShowListener != null) {
                        mShowListener.onAdProgress(currentTime, adBreakDuration);
                    }
                    break;
                case AD_BREAK_FETCH_ERROR:
                    if (mShowListener != null) {
                        mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdEvent: AD_BREAK_FETCH_ERROR");
                    break;
            }

        }
    };


    @Override
    public boolean isReady() {
        return adsManager != null;
    }

    @Override
    public String getNetworkName() {
        return "Google Ad Manager";
    }

    @Override
    public String getNetworkVersion() {
        return "3.29.0";
    }

    private void CheckLocalSetting(Map<String, Object> userParams) {
        if (userParams.containsKey(IMAConstant.IMA_SETTING_LANGUAGE)) {
            mLocalLanguage = (String) userParams.get(IMAConstant.IMA_SETTING_LANGUAGE);
            Log.i(TAG, "LocalLanguage: " + mLocalLanguage);
        }

        if (userParams.containsKey(AppKeyManager.AUTO_PLAY_VIDEO)) {
            mAutoPlayAd = (boolean) userParams.get(AppKeyManager.AUTO_PLAY_VIDEO);
            Log.i(TAG, "AutoPlayAd: " + mAutoPlayAd);
        }

        // 1 mute , 2 video
        if (userParams.containsKey(AppKeyManager.VIDEO_MUTE)) {
            mVideoMute = (int) userParams.get(AppKeyManager.VIDEO_MUTE);
            Log.i(TAG, "VideoMute: " + (mVideoMute == 1));
        }

        if (userParams.containsKey(IMAConstant.IMA_UI_COUNTDOWN)) {
            mUICountDown = (int) userParams.get(IMAConstant.IMA_UI_COUNTDOWN);
            Log.i(TAG, "UICountDown: " + (mUICountDown == 1));
        }
    }


    private void SupportPrivacyLaws(Map<String, Object> userParams, String url) {
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        String hostUrl = "";
        try {
            String[] split = url.split("\\?");
            if (split.length >= 2) {
                hostUrl = split[0];
                stringObjectHashMap = splitUrl(split[1], stringObjectHashMap);
            } else if (split.length >= 1) {
                hostUrl = split[0];
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        Object custom_params = userParams.get(IMA_CUSTOM_PARAMS);
        if(custom_params != null){
            try {
                Map<String, String> ima_custom_params = (Map<String, String>) custom_params;
                for (Map.Entry<String, String> entry : ima_custom_params.entrySet()) {
                    String mapKey = entry.getKey().trim();
                    String mapValue = entry.getValue().trim();
                    stringObjectHashMap.put(mapKey, mapValue);
                }
            }catch (Throwable throwable){

            }
        }


        // CCPA
        if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
            boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
            Log.i("privacylaws", "ccpa: " + ccpa);
            if (ccpa) {
                stringObjectHashMap.put("rdp", 1);
            }
        }

        if (userParams.containsKey(AppKeyManager.IS_UE)) {
            // GDPR
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT)) {
                boolean need_set_gdpr = false;
                int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent == TradPlus.NONPERSONALIZED || consent == TradPlus.UNKNOWN) {
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + "(true不收集)" + ":isUe:" + isEu);
                if (need_set_gdpr && isEu) {
                    stringObjectHashMap.put("npa", 1);
                }
            }

            // GDPR Child
            if (userParams.containsKey(AppKeyManager.KEY_GDPR_CHILD)) {
                boolean need_set_gdpr = (boolean) userParams.get(AppKeyManager.KEY_GDPR_CHILD);
                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPRChild: " + need_set_gdpr + "(true不收集)" + ":isUe:" + isEu);
                if (need_set_gdpr && isEu) {
                    stringObjectHashMap.put("tfua", 1);
                }
            }
        }

        // COPPA
        if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
            boolean isChildDirected = false;
            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                isChildDirected = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + isChildDirected);
                if (isChildDirected) {
                    stringObjectHashMap.put("tfcd", 1);
                }
            }
        }


        if (!TextUtils.isEmpty(hostUrl)) {
            SpliceUrl(hostUrl, stringObjectHashMap);
        }

        Log.i(TAG, "mURl: " + mURl);

    }

    private void SpliceUrl(String hostUrl, HashMap<String, Object> stringObjectHashMap) {
        String splitUrl = "";
        try {
            for (HashMap.Entry<String, Object> entry : stringObjectHashMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (TextUtils.isEmpty(splitUrl)) {
                    splitUrl = key + "=" + value;
                } else {
                    splitUrl = splitUrl + "&" + key + "=" + value;
                }
                LogUtil.ownShow("splitUrl: " + splitUrl, "TradPlusAd");
            }
            mURl = hostUrl + "?" + splitUrl;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, Object> splitUrl(String url, HashMap<String, Object> stringObjectHashMap) {
        String[] splitValue = url.split("&");
        for (int i = 0; i < splitValue.length; i++) {
            String[] splitSimple = splitValue[i].split("=");
            String key = "";
            String value = "";
            if (splitSimple.length >= 2) {
                key = splitSimple[0];
                value = splitSimple[1];
            } else if (splitSimple.length >= 1) {
                key = splitSimple[0];
            }
            stringObjectHashMap.put(key, value);
            LogUtil.ownShow("objectObjectHashMap === " + stringObjectHashMap, "TradPlusAd");
        }
        return stringObjectHashMap;
    }


    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }

        if (adsLoader != null) {
            adsLoader.removeAdsLoadedListener(null);
            adsLoader.removeAdErrorListener(null);
            adsLoader.release();
            adsLoader = null;
        }
    }


}
