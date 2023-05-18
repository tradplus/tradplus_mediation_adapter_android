package com.tradplus.ads.txadnet;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialMediaListener;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class TxAdnetInterstitialVideo extends TPInterstitialAdapter {
    private static final String TAG = "GDTInterstitial";
    private UnifiedInterstitialAD mUnifiedInterstitialAD;
    private String mPlacementId;
    private boolean isVideoSoundEnable = true; // true（自动播放时静音）; 1 :静音
    private boolean showPopUpWin = false; // 插屏半屏广告支持; false 有遮罩展示；true 无遮罩展示; 默认有遮罩
    private TxAdnetInterstitialCallbackRouter mCallbackRouter;
    private int fullScreenType;
    private int videoMaxTime, autoPlayVideo;
    private String payload;
    private String price;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String mAutoPlayVideo;
        String mVideoMute;
        String mVideoMaxTime;
        if (serverExtrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            price = serverExtras.get(DataKeys.BIDDING_PRICE);
            // 自动播放视频
            mAutoPlayVideo = serverExtras.get(AppKeyManager.AUTO_PLAY_VIDEO);
            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            mVideoMute = serverExtras.get(AppKeyManager.VIDEO_MUTE);
            // 视频最大时长
            mVideoMaxTime = serverExtras.get(AppKeyManager.VIDEO_MAX_TIME);
            // 是否全屏视频 是1 否0
            fullScreenType = Integer.parseInt(serverExtras.get(AppKeyManager.FULL_SCREEN_TYPE));

            Log.i(TAG, "loadCustomAd: AutoPlayVideo(自动播放) : " + mAutoPlayVideo + " , VideoMute(视频静音) :" + mVideoMute
                    + ", VideoMaxTime(视频最大时长) : " + mVideoMaxTime + "， FullScreenType(全屏视频) : " + fullScreenType);

            if (!TextUtils.isEmpty(mVideoMute)) {
                if (!mVideoMute.equals(AppKeyManager.VIDEO_MUTE_YES)) {
                    isVideoSoundEnable = false; // true（自动播放时静音）；false(自动播放有声)
                }
            }

            if (!TextUtils.isEmpty(mVideoMaxTime)) {
                videoMaxTime = Integer.parseInt(mVideoMaxTime);
            }

            if (!TextUtils.isEmpty(mAutoPlayVideo)) {
                autoPlayVideo = Integer.parseInt(mAutoPlayVideo);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(GDTConstant.GTD_POPUP)) {
                showPopUpWin = (boolean)localExtras.get(GDTConstant.GTD_POPUP);
            }
        }

        mCallbackRouter = TxAdnetInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        TencentInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                reqeustAd();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void reqeustAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (TextUtils.isEmpty(payload)) {
            mUnifiedInterstitialAD = new UnifiedInterstitialAD(activity, mPlacementId, mUnifiedInterstitialADListener);
        } else {
            mUnifiedInterstitialAD = new UnifiedInterstitialAD(activity, mPlacementId, mUnifiedInterstitialADListener, null, payload);
        }
        setVideoOption();
        // 设置监听器，监听视频广告的状态变化
        mUnifiedInterstitialAD.setMediaListener(mUnifiedInterstitialMediaListener);

        if (fullScreenType == AppKeyManager.FULL_TYPE) {
            Log.i(TAG, "loadAd 全屏视频");
            mUnifiedInterstitialAD.loadFullScreenAD();
        } else {
            Log.i(TAG, "loadAd 插屏半屏");
            mUnifiedInterstitialAD.loadAD();
        }
    }

    private void setVideoOption() {
        VideoOption.Builder builder = new VideoOption.Builder();
        //指定自动播放时是否静音，如果true（自动播放时静音）；false(自动播放有声)，默认值为true。
        Log.i(TAG, "PlacementId: " + mPlacementId + "， videoMute: " + isVideoSoundEnable);
        VideoOption option = builder.setAutoPlayMuted(isVideoSoundEnable)
                .setDetailPageMuted(isVideoSoundEnable)
                .setAutoPlayPolicy(autoPlayVideo == 1 ? VideoOption.AutoPlayPolicy.ALWAYS : VideoOption.AutoPlayPolicy.WIFI) //0代表wifi网络下；1，代表总是自动播放。
                .build();
        mUnifiedInterstitialAD.setVideoOption(option);

        if (videoMaxTime >= 5 && videoMaxTime <= 60) {
            mUnifiedInterstitialAD.setMaxVideoDuration(videoMaxTime);//设置最大时长
        }
    }


    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mUnifiedInterstitialAD != null) {
            setBidEcpm();
            if (fullScreenType == AppKeyManager.FULL_TYPE) {
                Log.i(TAG, "showAd 全屏视频 ");
                mUnifiedInterstitialAD.showFullScreenAD(activity);
            } else {
                if (showPopUpWin) {
                    Log.i(TAG, "showAd 插屏半屏（无遮罩）");
                    mUnifiedInterstitialAD.showAsPopupWindow(activity);
                }else {
                    Log.i(TAG, "showAd 插屏半屏（有遮罩）");
                    mUnifiedInterstitialAD.show(activity);
                }
            }
        } else {
            mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    // 监听视频广告的状态变化
    private final UnifiedInterstitialMediaListener mUnifiedInterstitialMediaListener = new UnifiedInterstitialMediaListener() {

        @Override
        public void onVideoInit() {

        }

        @Override
        public void onVideoLoading() {

        }

        @Override
        public void onVideoReady(long l) {

        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
            }
        }

        @Override
        public void onVideoPause() {

        }

        @Override
        public void onVideoComplete() {
            Log.i(TAG, "onVideoComplete: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
            }
        }

        @Override
        public void onVideoError(AdError adError) {
            Log.i(TAG, "onVideoError: code:" + adError.getErrorCode() + " msg: " + adError.getErrorMsg());
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(adError.getErrorCode() + "");
                tpError.setErrorMessage(adError.getErrorMsg());
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoPageOpen() {

        }

        @Override
        public void onVideoPageClose() {

        }
    };

    private final UnifiedInterstitialADListener mUnifiedInterstitialADListener = new UnifiedInterstitialADListener() {
        @Override
        public void onADReceive() {
            //onADReceive之后才可调用getECPM()
        }

        @Override
        public void onVideoCached() {
            // 视频素材加载完成，在此时调用iad.show()或iad.showAsPopupWindow()视频广告不会有进度条。
            // 2 视频；0 图片
            if (mUnifiedInterstitialAD.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    Log.i(TAG, "onVideoCached: " + mUnifiedInterstitialAD.getAdPatternType());
                    setNetworkObjectAd(mUnifiedInterstitialAD);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }
        }

        @Override
        public void onNoAD(AdError adError) {
            Log.i(TAG, "onNoAD, errorcode :" + adError.getErrorCode() + ", errormessage :" + adError.getErrorMsg());
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
            }
        }

        @Override
        public void onADOpened() {
            Log.i(TAG, "onADOpened: ");

        }

        @Override
        public void onADExposure() {
            Log.i(TAG, "onADExposure: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onADClicked() {
            Log.i(TAG, "onADClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }

        @Override
        public void onADLeftApplication() {
            Log.i(TAG, "onADLeftApplication: ");
        }

        @Override
        public void onADClosed() {
            Log.i(TAG, "onADClosed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }

        @Override
        public void onRenderSuccess() {
            //插屏半屏广告渲染成功时回调，此回调后才可以调用 show 方法
            setFirstLoadedTime();
            // 2 视频；0 图片
            if (mUnifiedInterstitialAD.getAdPatternType() != AdPatternType.NATIVE_VIDEO) {
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    Log.i(TAG, "onADReceive: " + mUnifiedInterstitialAD.getAdPatternType());
                    setNetworkObjectAd(mUnifiedInterstitialAD);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }

        }

        @Override
        public void onRenderFail() {
            //插屏半屏广告渲染失败时回调
            Log.i(TAG, "onRenderFail: ");
        }
    };


    @Override
    public void clean() {
        super.clean();
        if (mUnifiedInterstitialAD != null) {
            mUnifiedInterstitialAD.setMediaListener(null);
            mUnifiedInterstitialAD.destroy();
            mUnifiedInterstitialAD = null;
        }
    }

    @Override
    public boolean isReady() {
        Log.i(TAG, "isReady:" + mUnifiedInterstitialAD.isValid());
        // 广告是否有效，无效广告将无法展示
        if (mUnifiedInterstitialAD != null) {
            return !isAdsTimeOut() && mUnifiedInterstitialAD.isValid();
        }
        return false;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TENCENT);
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public void setNetworkExtObj(Object obj) {
        Log.i(TAG, "setNetworkExtObj: ");
        if (obj instanceof DownloadConfirmListener) {
            if (mUnifiedInterstitialAD != null) {
                mUnifiedInterstitialAD.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }
        }

    }

    private void setBidEcpm() {
        try {
            float temp = Float.parseFloat(price);
            int price = (int) temp;
            Log.i(TAG, "setBidEcpm: " + price);
            mUnifiedInterstitialAD.setBidECPM(price);
        } catch (Exception e) {

        }
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }
        String appId = tpParams.get(AppKeyManager.APP_ID);
        if (!TencentInitManager.isInited(appId)) {
            GDTAdSdk.init(context, appId);
        }
        return GDTAdSdk.getGDTAdManger().getBuyerId(null);
    }

    @Override
    public String getBiddingNetworkInfo(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        if (tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            try {
                return GDTAdSdk.getGDTAdManger().getSDKInfo(placementId);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }
}
