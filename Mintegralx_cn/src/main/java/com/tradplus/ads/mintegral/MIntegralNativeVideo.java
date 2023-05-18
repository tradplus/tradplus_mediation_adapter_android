package com.tradplus.ads.mintegral;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.AutoPlayMode;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.Frame;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBMultiStateEnum;
import com.mbridge.msdk.out.MBNativeAdvancedHandler;
import com.mbridge.msdk.out.MBNativeHandler;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.NativeAdvancedAdListener;
import com.mbridge.msdk.out.NativeListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.DeviceUtils;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

import static com.mbridge.msdk.MBridgeConstans.NATIVE_VIDEO_SUPPORT;
import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class MIntegralNativeVideo extends TPNativeAdapter {
    private static final String TAG = "MTGCNNative";
    private boolean videoSupport = true;//support native video
    private MBNativeHandler mMtgNativeHandler;
    private MBBidNativeHandler mMtgBidNativeHandler;
    private MBNativeAdvancedHandler mMBNativeAdvancedHandler;//自动渲染（模版）Native
    private ViewGroup mAdvancedNativeView; //自动渲染 获取模版视图的view
    private int mAdWidth;
    private int mAdHeight;
    private String mPlacementId;
    private String mUnitId;
    private MIntegralNativeAd mMIntegralNativeAd;
    private String payload;
    private int mAutoPlayVideo;
    private int mVideoMute = 1; // 默认静音播放
    private int mIsclosable;
    private int mIsTemplateRending;
    private boolean mNeedDownloadImg;
    private int adNum = 1; // 默认请求1个

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            mUnitId = serverExtras.get(AppKeyManager.UNIT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            // 自动渲染（模版）1 ；自定义渲染（自渲染） 2
            mIsTemplateRending = Integer.parseInt(serverExtras.get(AppKeyManager.IS_TEMPLATE_RENDERING));
            // 自动播放视频
            mAutoPlayVideo = Integer.parseInt(serverExtras.get(AppKeyManager.AUTO_PLAY_VIDEO));
            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            mVideoMute = Integer.parseInt(serverExtras.get(AppKeyManager.VIDEO_MUTE));
            // 关闭按钮展示设置
            mIsclosable = Integer.parseInt(serverExtras.get(MTGConstant.IS_CLOSABLE));
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

            if (localExtras.containsKey(MTGConstant.MTG_NATIVE_ADNUM)) {
                adNum = (int) localExtras.get(MTGConstant.MTG_NATIVE_ADNUM);
            }

        }

        if (mAdWidth <= 0 && mAdHeight <= 0) {
            if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
                // 官方建议宽高比例
                mAdWidth = 320;
                mAdHeight = 250;
            } else {
                mAdWidth = AppKeyManager.NATIVE_DEFAULT_WIDTH;
                mAdHeight = AppKeyManager.NATIVE_DEFAULT_HEIGHT;
            }
        }

        Log.i(TAG, "AdWidth : " + mAdWidth + ", AdHeight : " + mAdHeight);

        MintegralInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });


    }

    private void requestNative(Context context) {
        final Map<String, Object> properties = MBNativeHandler.getNativeProperties(mPlacementId, mUnitId);

        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            // 自动渲染
            Log.i(TAG, "load MBNativeAdvancedHandler : 自动渲染");
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }
            mMBNativeAdvancedHandler = new MBNativeAdvancedHandler(activity, mPlacementId, mUnitId);
            // 推荐: 320 x 250 比例
            mMBNativeAdvancedHandler.setNativeViewSize(DeviceUtils.dip2px(activity, mAdWidth), DeviceUtils.dip2px(activity, mAdHeight));
            // 设置关闭按钮的状态 is_closable
            // mbThreeState negative will hide close button,positive will display close button, other we will Decide whether to display based on the material
            mMBNativeAdvancedHandler.setCloseButtonState(mIsclosable == 1 ? MBMultiStateEnum.positive : MBMultiStateEnum.negative);
            // 默认静音 video_mute 1 静音
            mMBNativeAdvancedHandler.setPlayMuteState(mVideoMute != AppKeyManager.VIDEO_MUTE_NO ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.REWARD_VIDEO_PLAY_NOT_MUTE);

            // 1 总是 ；2 仅Wi-Fi ；3 手动播放
            if (mAutoPlayVideo == AppKeyManager.AUTO_PLAYVIDEO_CLICK) {
                mMBNativeAdvancedHandler.autoLoopPlay(AutoPlayMode.PLAY_WHEN_USER_CLICK);
            } else if (mAutoPlayVideo == AppKeyManager.AUTO_PLAYVIDEO_WIFI) {
                mMBNativeAdvancedHandler.autoLoopPlay(AutoPlayMode.PLAY_WHEN_NETWORK_IS_WIFI);
            } else {
                // 三方默认
                mMBNativeAdvancedHandler.autoLoopPlay(AutoPlayMode.PLAY_WHEN_NETWORK_IS_AVAILABLE);
            }
            mAdvancedNativeView = mMBNativeAdvancedHandler.getAdViewGroup();
            mMBNativeAdvancedHandler.setAdListener(mNativeAdvancedAdListener);
            Log.i(TAG, "payload: " + payload);
            if (!TextUtils.isEmpty(payload)) {
                mMBNativeAdvancedHandler.loadByToken(payload);
            } else {
                mMBNativeAdvancedHandler.load();
            }

        } else {
            // 自定义渲染

            //期望获取的广告数量
            properties.put(MBridgeConstans.PROPERTIES_AD_NUM, adNum);
            //SDK内部判断了这个属性：获取原生广告视频时长（三方建议加上）
            properties.put(MBridgeConstans.NATIVE_VIDEO_WIDTH, mAdWidth);
            properties.put(MBridgeConstans.NATIVE_VIDEO_HEIGHT, mAdHeight);
            properties.put(NATIVE_VIDEO_SUPPORT, videoSupport);

            if (!TextUtils.isEmpty(payload)) {
                Log.i(TAG, "load MBBidNativeHandler: Bidding 自定义渲染 ");
                mMtgBidNativeHandler = new MBBidNativeHandler(properties, context);
                mMtgBidNativeHandler.setAdListener(nativeAdListener);
                mMtgBidNativeHandler.setTrackingListener(nativeTrackingListener);
                mMtgBidNativeHandler.bidLoad(payload);
            } else {
                Log.i(TAG, "load MBNativeHandler : 自定义渲染");
                mMtgNativeHandler = new MBNativeHandler(properties, context);
                mMtgNativeHandler.setAdListener(nativeAdListener);
                mMtgNativeHandler.setTrackingListener(nativeTrackingListener);
                //优先从快速读取广告，如果有广告，则直接返回；如果没有，则请求发送广告获取广告提示
                mMtgNativeHandler.load();
            }
        }
    }

    // 自动渲染（模版） 监听
    private final NativeAdvancedAdListener mNativeAdvancedAdListener = new NativeAdvancedAdListener() {
        @Override
        public void onLoadFailed(MBridgeIds mBridgeIds, String s) {
            Log.i(TAG, "onLoadFailed: msg:" + s);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

        }

        @Override
        public void onLoadSuccessed(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLoadSuccessed: ");
            if (mAdvancedNativeView == null || mAdvancedNativeView.getParent() != null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(UNSPECIFIED);
                    tpError.setErrorMessage("advancedNativeView.getParent() != null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }

            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }

            mMIntegralNativeAd = new MIntegralNativeAd(mMBNativeAdvancedHandler, mAdvancedNativeView, context);
            mMIntegralNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_EXPRESS);

            // onLoadSuccessed后调用，开发者可以通过这个API将广告ID记录并反馈到Mintegral
            if (!TextUtils.isEmpty(mMBNativeAdvancedHandler.getRequestId())) {
                Log.i(TAG, "广告ID RequestId: " + mMBNativeAdvancedHandler.getRequestId());
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mMIntegralNativeAd);
            }


        }

        @Override
        public void onLogImpression(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLogImpression: ");
            if (mMIntegralNativeAd != null) mMIntegralNativeAd.onAdShown();
        }

        @Override
        public void onClick(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onClick: ");
            if (mMIntegralNativeAd != null) mMIntegralNativeAd.onAdClicked();
        }

        @Override
        public void onLeaveApp(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLeaveApp: 离开app");
        }

        @Override
        public void showFullScreen(MBridgeIds mBridgeIds) {
            Log.i(TAG, "showFullScreen: 进入全屏 （只有走mraid协议的素材才会有这个回调）");
        }

        @Override
        public void closeFullScreen(MBridgeIds mBridgeIds) {
            Log.i(TAG, "closeFullScreen: 退出全屏 （只有走mraid协议的素材才会有这个回调）");
        }

        @Override
        public void onClose(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onClose: ");
            if (mMIntegralNativeAd != null) mMIntegralNativeAd.onAdClosed();
        }
    };

    NativeListener.NativeAdListener nativeAdListener = new NativeListener.NativeAdListener() {
        @Override
        public void onAdLoaded(List<Campaign> list, int i) {
            Log.i(TAG, "onAdLoaded: ");
            if (list == null || list.size() <= 0) {
                return;
            }
            Campaign campaign = list.get(0);

            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }

            // 1 静音 ；2 播放
            // 自定义渲染，true 开启 视频声音 ；false 关闭 视频声音
            Log.i(TAG, "VideoMute: " + mVideoMute);
            if (TextUtils.isEmpty(payload)) {
                mMIntegralNativeAd = new MIntegralNativeAd(campaign, context, mMtgNativeHandler, mVideoMute == AppKeyManager.VIDEO_MUTE_NO);
            } else {
                mMIntegralNativeAd = new MIntegralNativeAd(campaign, context, mMtgBidNativeHandler, mVideoMute == AppKeyManager.VIDEO_MUTE_NO);

            }
            mMIntegralNativeAd.setRenderType(TPBaseAd.AD_TYPE_NORMAL_NATIVE);

            downloadAndCallback(mMIntegralNativeAd, mNeedDownloadImg);
        }


        @Override
        public void onAdLoadError(String s) {
            Log.i(TAG, "onAdLoadError: " + s);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdClick(Campaign campaign) {
            //广告点击时调用
            Log.i(TAG, "onAdClick: " + campaign.toString());
            if (mMIntegralNativeAd != null) {
                mMIntegralNativeAd.onAdClicked();
            }
        }

        @Override
        public void onAdFramesLoaded(List<Frame> list) {
            //点击广告时调用（可以忽略）
        }

        @Override
        public void onLoggingImpression(int i) {
            Log.i(TAG, "onLoggingImpression: ");
            //广告展示时调用
            if (mMIntegralNativeAd != null) mMIntegralNativeAd.onAdShown();
        }
    };

    // 自定义渲染 监听
    NativeListener.TrackingExListener nativeTrackingListener = new NativeListener.TrackingExListener() {
        @Override
        public void onLeaveApp() {

        }

        @Override
        public boolean onInterceptDefaultLoadingDialog() {
            return false;
        }

        @Override
        public void onShowLoading(Campaign campaign) {

        }

        @Override
        public void onDismissLoading(Campaign campaign) {

        }

        @Override
        public void onStartRedirection(Campaign campaign, String s) {

        }

        @Override
        public void onFinishRedirection(Campaign campaign, String s) {

        }

        @Override
        public void onRedirectionFailed(Campaign campaign, String s) {

        }

        @Override
        public void onDownloadStart(Campaign campaign) {

        }

        @Override
        public void onDownloadFinish(Campaign campaign) {

        }

        @Override
        public void onDownloadProgress(int i) {

        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mMtgNativeHandler != null) {
            mMtgNativeHandler.setAdListener(null);
            mMtgNativeHandler.release();
            mMtgNativeHandler = null;
        }

        if (mAdvancedNativeView != null) {
            mAdvancedNativeView.removeAllViews();
            mAdvancedNativeView = null;
        }

        if (mMBNativeAdvancedHandler != null) {
            mMBNativeAdvancedHandler.setAdListener(null);
            mMBNativeAdvancedHandler.release();
            mMBNativeAdvancedHandler = null;
        }

        if (mMtgBidNativeHandler != null) {
            mMtgBidNativeHandler.setAdListener(null);
            mMtgBidNativeHandler.bidRelease();
            mMtgBidNativeHandler = null;
        }
    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MTG);
    }

    @Override
    public String getNetworkVersion() {
        return MBConfiguration.SDK_VERSION;
    }

    @Override
    public void getBiddingToken(final Context context, final Map<String, String> tpParams, final Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        TPTaskManager.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean initSuccess = true;
                String appKey = tpParams.get(AppKeyManager.APP_KEY);
                String appId = tpParams.get(AppKeyManager.APP_ID);

                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    initSuccess = MintegralInitManager.isInited(appKey + appId);
                }

                final boolean finalInitSuccess = initSuccess;
                MintegralInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
                    @Override
                    public void onSuccess() {
                        String token = BidManager.getBuyerUid(context);
                        if (!finalInitSuccess) {
                            // 第一次初始化 250
                            MintegralInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                        }

                        if (onS2STokenListener != null) {
                            onS2STokenListener.onTokenResult(token, null);
                        }
                    }

                    @Override
                    public void onFailed(String code, String msg) {
                        MintegralInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                        if (onS2STokenListener != null) {
                            onS2STokenListener.onTokenResult("", null);
                        }
                    }
                });
            }
        });
    }
}
