package com.tradplus.ads.txadnet;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeADEventListenerWithClickInfo;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.ads.nativ.NativeExpressMediaListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NO_FILL;


public class TxAdnetNativeVideo extends TPNativeAdapter {

    private static final String TAG = "GDTNativeAd";
    private String mPlacementId;
    private int mAdWidth;
    private int mAdHeight;
    private NativeUnifiedADData mNativeUnifiedADData; // 原生自渲染
    private NativeUnifiedAD mNativeUnifiedAD; // 原生自渲染
    private TxAdnetNativeData mTXAdnetNativeData;
    private int mIsTemplateRending, autoPlayVideo, videoMaxTime;
    private boolean isVideoSoundEnable = true; // 下发 1 ；静音
    private TxAdnetNativeData mNativeData;
    private NativeExpressAD nativeExpressAD;// 原生模版
    private NativeExpressADView mNativeExpressADView; //原生模版View;
    private boolean mNeedDownloadImg = false;
    private String payload;
    private String price;
    private String secType;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        String mAutoPlayVideo;
        String mVideoMute;
        String mVideoMaxTime;
        String template;
        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            price = serverExtras.get(DataKeys.BIDDING_PRICE);
            secType = serverExtras.get(AppKeyManager.ADTYPE_SEC);
            // 自动播放视频
            mAutoPlayVideo = serverExtras.get(AppKeyManager.AUTO_PLAY_VIDEO);
            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            mVideoMute = serverExtras.get(AppKeyManager.VIDEO_MUTE);
            // 视频最大时长
            mVideoMaxTime = serverExtras.get(AppKeyManager.VIDEO_MAX_TIME);
            template = serverExtras.get(AppKeyManager.IS_TEMPLATE_RENDERING);

            Log.i(TAG, "AutoPlayVideo(自动播放) : " + mAutoPlayVideo + " , VideoMute(视频静音) :" + mVideoMute
                    + ", VideoMaxTime(视频最大时长) : " + mVideoMaxTime + ", template :" + template);

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }

            if (!TextUtils.isEmpty(mAutoPlayVideo)) {
                autoPlayVideo = Integer.parseInt(mAutoPlayVideo);
            }

            if (!TextUtils.isEmpty(mVideoMaxTime)) {
                videoMaxTime = Integer.parseInt(mVideoMaxTime);
            }

            Log.i(TAG, "videoMute: " + mVideoMute);
            if (!TextUtils.isEmpty(mVideoMute)) {
                if (!mVideoMute.equals(AppKeyManager.VIDEO_MUTE_YES)) {
                    isVideoSoundEnable = false; // 三方静音，传true
                    Log.i(TAG, "videoMute: " + isVideoSoundEnable);
                }
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;

        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(DataKeys.AD_WIDTH)) {
                mAdWidth = (int) localExtras.get(DataKeys.AD_WIDTH);
            }
            if (localExtras.containsKey(DataKeys.AD_HEIGHT)) {
                mAdHeight = (int) localExtras.get(DataKeys.AD_HEIGHT);
            }

            if (localExtras.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) localExtras.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

        if (mAdHeight == 0 || mAdWidth == 0) {
            mAdWidth = ADSize.FULL_WIDTH; //最大宽度
            mAdHeight = ADSize.AUTO_HEIGHT; //自适应高度
        }

        Log.i(TAG, "Width :" + mAdWidth + ", Height :" + mAdHeight);

        TencentInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                reqeustAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void reqeustAd(Context context) {
        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_NO || mIsTemplateRending == GDTConstant.TEMPLATE_PATCH_RENDERING_NO
                || AppKeyManager.NATIVE_TYPE_DRAWLIST.equals(secType)) {

            // 自渲染广告入口，用于初始化并加载广告
            if (TextUtils.isEmpty(payload)) {
                mNativeUnifiedAD = new NativeUnifiedAD(context, mPlacementId, mNativeADUnifiedListener);
            } else {
                mNativeUnifiedAD = new NativeUnifiedAD(context, mPlacementId, mNativeADUnifiedListener, payload);
            }

            if (videoMaxTime >= 5 && videoMaxTime <= 60) {
                mNativeUnifiedAD.setMaxVideoDuration(videoMaxTime);//设置最大时长
            }
            if (AppKeyManager.NATIVE_TYPE_DRAWLIST.equals(secType)) {
                Log.i(TAG, "请求自渲染Draw信息流");
                mNativeUnifiedAD.loadData(3);
            } else {
                Log.i(TAG, "请求自渲染");
                mNativeUnifiedAD.loadData(1);
            }

        } else if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES || mIsTemplateRending == GDTConstant.TEMPLATE_PATCH_RENDERING_YES) {
            Log.i(TAG, "请求模版");
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }
            ADSize adSize = new ADSize(mAdWidth, mAdHeight);

            if (TextUtils.isEmpty(payload)) {
                nativeExpressAD = new NativeExpressAD(activity, adSize, mPlacementId, mNativeExpressADListener);
            } else {
                nativeExpressAD = new NativeExpressAD(activity, adSize, mPlacementId, mNativeExpressADListener, payload);
            }
            nativeExpressAD.setVideoOption(getVideoOption());
            nativeExpressAD.loadAD(1);
        }

    }

    private VideoOption getVideoOption() {
        VideoOption.Builder builder = new VideoOption.Builder();
        if (autoPlayVideo == 3) {
            Log.i(TAG, "VideoOption: 手动播放");
            builder.setAutoPlayPolicy(VideoOption.AutoPlayPolicy.NEVER);
        } else if (autoPlayVideo == 2) {
            builder.setAutoPlayPolicy(VideoOption.AutoPlayPolicy.WIFI);
        } else {
            builder.setAutoPlayPolicy(VideoOption.AutoPlayPolicy.ALWAYS);//0代表wifi网络下；1，代表总是自动播放。
        }

        if (mNativeUnifiedADData != null && mNativeUnifiedADData.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
            Log.i(TAG, "VideoOption: 自渲染原生视频");
            // 在预览页点击视频播放器区域控制视频的暂停或播放,true生效
            // 同时需要设置setEnableDetailPage为false，否则点击会跳转到详情页
            // 只对自渲染视频广告生效
            builder.setEnableUserControl(autoPlayVideo == 3);
            builder.setNeedCoverImage(true); // 显示封面
        }

        builder.setAutoPlayMuted(isVideoSoundEnable); // 自动播放时为静音
        // 用户在预览页点击clickableViews或视频区域(setEnableUserControl设置为false)时是否跳转到详情页
        // 默认为true，跳转到详情页；
        // 只对自渲染视频广告生效 ———— 实际对自渲染图片也会生效
        builder.setEnableDetailPage(true);

        builder.setDetailPageMuted(isVideoSoundEnable);  // 视频详情页播放时
        if (nativeExpressAD != null && videoMaxTime >= 5 && videoMaxTime <= 60) {
            nativeExpressAD.setMaxVideoDuration(videoMaxTime);//设置最大时长
        }

        return builder.build();

    }


    // 自渲染
    private final NativeADUnifiedListener mNativeADUnifiedListener = new NativeADUnifiedListener() {
        @Override
        public void onADLoaded(List<NativeUnifiedADData> list) {
            if (list == null || list.size() <= 0) {
                return;
            }
            setBidEcpm();

            if (AppKeyManager.NATIVE_TYPE_DRAWLIST.equals(secType)) {
                renderDrawNativeData(list);
            } else {
                mNativeUnifiedADData = list.get(0);
                mNativeUnifiedADData.setNativeAdEventListener(nativeADEventListener);
                renderNativeData();
            }
        }

        @Override
        public void onNoAD(AdError adError) {
            Log.i(TAG, "onNoAD: errorCode ：" + adError.getErrorCode() + ",errorMessage : " + adError.getErrorMsg());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
            }
        }
    };

    private void renderNativeData() {
        Context context = GlobalTradPlus.getInstance().getContext();
        if (context == null) {
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                tpError.setErrorMessage("context == null");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
            return;
        }
        mTXAdnetNativeData = new TxAdnetNativeData(mNativeUnifiedADData, context, isVideoSoundEnable);
        mTXAdnetNativeData.setVideoOption(getVideoOption());
        mTXAdnetNativeData.setRenderType(mIsTemplateRending);
        downloadAndCallback(mTXAdnetNativeData, mNeedDownloadImg);
    }

    private void renderDrawNativeData(List<NativeUnifiedADData> list) {
        Context context = GlobalTradPlus.getInstance().getContext();
        if (context == null) {
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                tpError.setErrorMessage("context == null");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
            return;
        }

        mTXAdnetNativeData = new TxAdnetNativeData(list, context, isVideoSoundEnable);
        mTXAdnetNativeData.setVideoOption(getVideoOption());
        mTXAdnetNativeData.setRenderType(mIsTemplateRending);
        mLoadAdapterListener.loadAdapterLoaded(mTXAdnetNativeData);
    }

    // 自渲染:设置广告点击曝光等事件监听者
    final NativeADEventListenerWithClickInfo nativeADEventListener = new NativeADEventListenerWithClickInfo() {
        @Override
        public void onADExposed() {
            Log.i(TAG, "广告曝光");
            if (mTXAdnetNativeData != null) {
                mTXAdnetNativeData.adShown();
            }
        }

        @Override
        public void onADClicked(View view) {
            Log.i(TAG, "onADClicked: ");
            if (mTXAdnetNativeData != null)
                mTXAdnetNativeData.adClicked();
        }

        @Override
        public void onADError(AdError error) {
            Log.d(TAG, "错误回调 error code :" + error.getErrorCode()
                    + "  error msg: " + error.getErrorMsg());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(error));
            }
        }

        @Override
        public void onADStatusChanged() {

        }
    };


    // 模版
    private final NativeExpressAD.NativeExpressADListener mNativeExpressADListener = new NativeExpressAD.NativeExpressADListener() {
        @Override
        public void onNoAD(AdError adError) {
            Log.i(TAG, "onNoAD: errorCode ：" + adError.getErrorCode() + ",errorMessage : " + adError.getErrorMsg());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
            }
        }

        @Override
        public void onADLoaded(List<NativeExpressADView> list) {

            // 释放前一个展示的NativeExpressADView的资源
            if (mNativeExpressADView != null) {
                mNativeExpressADView.destroy();
            }

            setBidEcpm();

            // 广告数据加载成功，返回了可以用来展示广告的 NativeExpressADView，但是想让广告曝光还需要调用 NativeExpressADView 的 render 方法
            mNativeExpressADView = list.get(0);
            if (mNativeExpressADView.getBoundData().getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
                mNativeExpressADView.setMediaListener(mediaListener);
//                //预加载视频素材，加载成功会回调mediaListener的onVideoCached方法，失败的话回调onVideoError方法errorCode为702。
                mNativeExpressADView.preloadVideo();
            } else {
                Log.i(TAG, "onADLoaded: 模版图片");
                // 广告可见才会产生曝光，否则将无法产生收益。
                mNativeExpressADView.render();
            }

        }

        @Override
        public void onRenderFail(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onRenderFail: ");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NO_FILL));
            }
        }

        @Override
        public void onRenderSuccess(NativeExpressADView nativeExpressADView) {
            if (mNativeExpressADView.getBoundData().getAdPatternType() != AdPatternType.NATIVE_VIDEO) {
                Log.i(TAG, "onRenderSuccess: 模版图片");
                mNativeExpressADView = nativeExpressADView;

                Context context = GlobalTradPlus.getInstance().getContext();
                if (context == null) {
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                        tpError.setErrorMessage("context == null");
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                if (mLoadAdapterListener != null) {
                    mNativeData = new TxAdnetNativeData(context, mNativeExpressADView);
                    mNativeData.setRenderType(mIsTemplateRending);
                    mLoadAdapterListener.loadAdapterLoaded(mNativeData);
                }
            }
        }

        @Override
        public void onADExposure(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onADExposure: ");
            if (mNativeData != null) {
                mNativeData.adShown();
            }
        }

        @Override
        public void onADClicked(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onADClicked: ");
            if (mNativeData != null) {
                mNativeData.adClicked();
            }
        }

        @Override
        public void onADClosed(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onADClosed: ");
            if (mNativeData != null) {
                mNativeData.adClosed();
            }
        }

        @Override
        public void onADLeftApplication(NativeExpressADView nativeExpressADView) {

        }

    };

    private final NativeExpressMediaListener mediaListener = new NativeExpressMediaListener() {
        @Override
        public void onVideoInit(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoInit: ");
        }

        @Override
        public void onVideoLoading(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoLoading");
        }

        @Override
        public void onVideoCached(NativeExpressADView adView) {
            Log.i(TAG, "onVideoCached 模版视频");
            mNativeExpressADView = adView;
            mNativeExpressADView.render();

            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }


            if (mLoadAdapterListener != null) {
                mNativeData = new TxAdnetNativeData(context, mNativeExpressADView);
                mNativeData.setRenderType(mIsTemplateRending);
                mLoadAdapterListener.loadAdapterLoaded(mNativeData);
            }
        }

        @Override
        public void onVideoReady(NativeExpressADView nativeExpressADView, long l) {
            Log.i(TAG, "onVideoReady");
        }

        @Override
        public void onVideoStart(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoStart: ");
            if (mNativeData != null) {
                mNativeData.onAdVideoStart();
            }
        }

        @Override
        public void onVideoPause(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoPause: ");
        }

        @Override
        public void onVideoComplete(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoComplete: ");
            if (mNativeData != null) {
                mNativeData.onAdVideoEnd();
            }
        }

        @Override
        public void onVideoError(NativeExpressADView nativeExpressADView, AdError adError) {
            Log.i(TAG, "onVideoError");
        }

        @Override
        public void onVideoPageOpen(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoPageOpen");
        }

        @Override
        public void onVideoPageClose(NativeExpressADView nativeExpressADView) {
            Log.i(TAG, "onVideoPageClose");
        }
    };


    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mNativeExpressADView != null) {
            mNativeExpressADView.setMediaListener(null);
            mNativeExpressADView.destroy();
            mNativeExpressADView = null;
        }

        if (mNativeUnifiedADData != null) {
            mNativeUnifiedADData.setNativeAdEventListener(null);
            mNativeUnifiedADData.destroy();
            mNativeUnifiedADData = null;
        }

    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TENCENT);
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    private void setBidEcpm() {
        try {
            float temp = Float.parseFloat(price);
            int price = (int) temp;
            Log.i(TAG, "setBidEcpm: " + price);
            if (mNativeExpressADView != null) {
                mNativeExpressADView.setBidECPM(price);
            }
            if (mNativeUnifiedADData != null) {
                mNativeUnifiedADData.setBidECPM(price);
            }
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
