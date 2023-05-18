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
    private AdsManager adsManager; // 广告加载成功后获取
    private VideoAdPlayer mVideoPlayer;
    private String mLocalLanguage;
    private boolean mAutoPlayAd = false; // 默认不自动播放，load和show分开
    //    private int savedPosition = 0; // 保存的内容位置，用于在广告中断后恢复内容。
    private GoogleMediaVideoAd mGoolgeMediaVideoAd;
    private Integer mVideoMute = 1; // 静音
    private Integer mUICountDown = 1; // 默认显示
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

//      mURl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator=";
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
            CheckLocalSetting(userParams); // 设置本地方法
            SupportPrivacyLaws(userParams, mURl); // 隐私协议
        }

        // 启动IMA SDK & 创建AdsLoader
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

        // 请求广告
        requestAds(mURl);
        // 添加 AdsLoader 监听器
        setAdsLoaderListener();

    }

    private void setAdsLoaderListener() {
        adsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                // 广告已成功加载，获取 AdsManager 实例。
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

//                        String universalAdIds = Arrays.toString(adsManager.getCurrentAd().getUniversalAdIds());
//                        Log.i(TAG, "Discarding the current ad break with universal " + "ad Ids: " + universalAdIds);
                        // 丢弃当前的广告插播并恢复内容
                        adsManager.discardAdBreak();

//                        resumeContent();

                        if (mShowListener != null) {
                            mShowListener.onAdVideoError(tpError);
                        }
                    }
                });

                //处理 IMA 广告事件
                adsManager.addAdEventListener(adEventListener);

                // 初始化AdsManager
                AdsRenderingSettings adsRenderingSettings = sdkFactory.createAdsRenderingSettings();
                adsRenderingSettings.setEnablePreloading(true);
                if (mUICountDown != 1) {
                    // 1显示，其他都隐藏倒计时
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

//                resumeContent();

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
                    // AdEventType.LOADED 将在广告准备好播放时触发。
                    // AdsManager.start() 开始播放广告。
                    // 对于 VMAP 或广告规则播放列表，此方法将被忽略，因为 SDK 将自动开始执行播放列表。
                    if (mLoadAdapterListener != null) {
                        mGoolgeMediaVideoAd = new GoogleMediaVideoAd(adsManager);
                        mLoadAdapterListener.loadAdapterLoaded(mGoolgeMediaVideoAd);
                    }
                    Log.i(TAG, "onAdEvent: LOADED");
                    break;
                case CONTENT_PAUSE_REQUESTED:
                    // 在视频广告播放前立即触发。
//                    pauseContentForAds();
                    Log.i(TAG, "onAdEvent: CONTENT_PAUSE_REQUESTED");
                    break;
                case CONTENT_RESUME_REQUESTED:
                    // 在广告完成时触发，您应该开始播放您的内容。
//                    resumeContent();
                    Log.i(TAG, "onAdEvent: CONTENT_RESUME_REQUESTED");
                    break;
                case TAPPED:
                    // 当点击视频广告的非点击部分时触发
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
                    // 广告暂停
                    if (mShowListener != null) {
                        mShowListener.onAdPause();
                    }
//                    pauseContentForAds();
                    Log.i(TAG, "onAdEvent: PAUSED");
                    break;
                case RESUMED:
                    // 广告恢复
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
                    // 广告播放完成
                    if (mShowListener != null && adShowFailed == 0) {
                        // 展示失败的情况，不打印埋点和回调
                        mShowListener.onAdVideoEnd();
                        mShowListener.onAdClosed();
                    }
                    // 将广告相关对象全部置空
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
//                    Log.i(TAG, "onAdEvent: AD_PROGRESS");
                    break;
                case AD_BREAK_FETCH_ERROR:
                    if (mShowListener != null) {
                        mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onAdEvent: AD_BREAK_FETCH_ERROR");
                    // A CONTENT_RESUME_REQUESTED event should follow to trigger content playback.
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
        // 用于自定义设置本地语言
        if (userParams.containsKey(IMAConstant.IMA_SETTING_LANGUAGE)) {
            mLocalLanguage = (String) userParams.get(IMAConstant.IMA_SETTING_LANGUAGE);
            Log.i(TAG, "LocalLanguage: " + mLocalLanguage);
        }

        // 指定是否自动播放 VMAP 和广告规则广告插播。
        if (userParams.containsKey(AppKeyManager.AUTO_PLAY_VIDEO)) {
            mAutoPlayAd = (boolean) userParams.get(AppKeyManager.AUTO_PLAY_VIDEO);
            Log.i(TAG, "AutoPlayAd: " + mAutoPlayAd);
        }

        // 1 mute , 2 video
        if (userParams.containsKey(AppKeyManager.VIDEO_MUTE)) {
            mVideoMute = (int) userParams.get(AppKeyManager.VIDEO_MUTE);
            Log.i(TAG, "VideoMute: " + (mVideoMute == 1));
        }

        // 隐藏倒计时 1 显示 其他隐藏
        if (userParams.containsKey(IMAConstant.IMA_UI_COUNTDOWN)) {
            mUICountDown = (int) userParams.get(IMAConstant.IMA_UI_COUNTDOWN);
            Log.i(TAG, "UICountDown: " + (mUICountDown == 1));
        }
    }


    private void SupportPrivacyLaws(Map<String, Object> userParams, String url) {
        // 缺少？的情况；以及？缺少字段
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
                    // 用户拒绝 || 未知 情况都不收集信息
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
            // COPPA隐私标志设置为true，以限制传输android广告标识符后才能设置
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
                // 最后一个 value是空的
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

    // 处理广告和内容之间的切换
    // 重复使用播放器来播放内容和广告
//    private void pauseContentForAds() {
//        if (videoPlayer != null) {
//            savedPosition = videoPlayer.getCurrentPosition();
//            Log.i(TAG, "pauseContentForAds:" + savedPosition);
//            videoPlayer.stopPlayback();
//        }
//    }

//    private void resumeContent() {
//        if (videoPlayer != null) {
//            videoPlayer.setOnPreparedListener((MediaPlayer mediaPlayer) -> {
//                if (savedPosition > 0) {
//                    mediaPlayer.seekTo(savedPosition);
//                }
//                mediaPlayer.start();
//            });
//            Log.i(TAG, "resumeContent:" + savedPosition);
//            videoPlayer.setOnCompletionListener(mediaPlayer -> videoAdPlayerAdpter.notifyImaOnContentCompleted());
//        }
//    }
}
