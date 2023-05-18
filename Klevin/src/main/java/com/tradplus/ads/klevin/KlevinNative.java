package com.tradplus.ads.klevin;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.tencent.klevin.KlevinManager;
import com.tencent.klevin.ads.ad.AdSize;
import com.tencent.klevin.ads.ad.AppDownloadListener;
import com.tencent.klevin.ads.ad.NativeAd;
import com.tencent.klevin.ads.ad.NativeAdRequest;
import com.tencent.klevin.ads.ad.NativeExpressAd;
import com.tencent.klevin.ads.ad.NativeExpressAdRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

public class KlevinNative extends TPNativeAdapter {

    private int mPostId, mIsTemplateRending;
    private List<NativeAd> mNativeAds; // 自渲染
    private NativeExpressAd nativeExpressAd; // 模版
    private NativeAd nativeAd; // 自渲染
    private KlevinNativeAd mKlevinNativeAd;
    private int autoPlayVideo = NativeAd.AUTO_PLAY_POLICY_WIFI; //默认值Wi-Fi下自动播放
    private boolean isVideoSoundEnable = true; // 下发 1 ；静音
    private boolean mNeedDownloadImg = false;
    private int mAdWidth;
    private int mAdHeight;
    private View mView;//模板
    private int ecpmLevel;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    public static final String TAG = "KlevinNative";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        String placementId, mAutoPlayVideo, mVideoMute, template;
        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            // 自动播放视频
            mAutoPlayVideo = tpParams.get(AppKeyManager.AUTO_PLAY_VIDEO);
            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            mVideoMute = tpParams.get(AppKeyManager.VIDEO_MUTE);
            template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);

            if (!TextUtils.isEmpty(placementId)) {
                mPostId = Integer.parseInt(placementId);
            }
            if (!TextUtils.isEmpty(mAutoPlayVideo)) {
                autoPlayVideo = Integer.parseInt(mAutoPlayVideo);
            }

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }

            if (!TextUtils.isEmpty(mVideoMute)) {
                if (!AppKeyManager.VIDEO_MUTE_YES.equals(mVideoMute)) {
                    isVideoSoundEnable = false; // 三方静音，传true
                    Log.i(TAG, "videoMute: " + isVideoSoundEnable);
                }
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {

            if (userParams.containsKey(DataKeys.AD_WIDTH)) {
                mAdWidth = (int) userParams.get(DataKeys.AD_WIDTH);
                Log.i(TAG, "Width: " + mAdWidth);
            }
            if (userParams.containsKey(DataKeys.AD_HEIGHT)) {
                mAdHeight = (int) userParams.get(DataKeys.AD_HEIGHT);
                Log.i(TAG, "Height: " + mAdHeight);
            }

            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

        if (mAdHeight == 0 || mAdWidth == 0) {
            mAdWidth = 375; //官方Demo设置
            mAdHeight = 350; //官方Demo设置
        }
//        appId = "30008";
//        mPostId = 32797;


        KlevinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative();
            }

            @Override
            public void onFailed(String code, String msg) {
                TPError tpError = new TPError(INIT_FAILED);
                tpError.setErrorCode(code);
                tpError.setErrorMessage(msg);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });


    }

    private void requestNative() {
        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            if (isC2SBidding && isBiddingLoaded && mView != null) {
                if (mLoadAdapterListener != null) {
                    mKlevinNativeAd = new KlevinNativeAd(mView, TPBaseAd.AD_TYPE_NATIVE_EXPRESS);
                    mLoadAdapterListener.loadAdapterLoaded(mKlevinNativeAd);
                }
                return;
            }

            Log.i(TAG, "request 模版: ");
            if (nativeExpressAd != null) {
                nativeExpressAd.destroy();
                nativeExpressAd = null;
            }

            NativeExpressAdRequest.Builder builder = new NativeExpressAdRequest.Builder();
            builder.setPosId(mPostId).setAdCount(1);
            builder.setMute(isVideoSoundEnable);
            NativeExpressAd.load(builder.build(), new NativeExpressAd.NativeExpressAdLoadListener() {
                @Override
                public void onAdLoadError(int err, String msg) {
                    Log.i(TAG, "onAdLoadError: errcode :" + err + ", msg :" + msg);
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(err + "");
                    tpError.setErrorMessage(msg);
                    if (mLoadAdapterListener != null)
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                @Override
                public void onAdLoaded(List<NativeExpressAd> ads) {
                    if (ads == null) return;
                    Log.i(TAG, "onAdLoaded: ");
                    nativeExpressAd = ads.get(0);
                    nativeExpressAd.setAdSize(new AdSize(mAdWidth, mAdHeight));
                    nativeExpressAd.setInteractionListener(mAdInteractionListener);
                    // 是否为视频广告
                    if (nativeExpressAd.isVideoAd()) {
                        nativeExpressAd.setVideoAdListener(mExpressVideoAdListener);
                    }
                    nativeExpressAd.setDownloadListener(new AppDownloadListener() {
                        @Override
                        public void onIdle() {

                        }

                        @Override
                        public void onDownloadStart(long l, String s, String s1) {
                            Log.i(TAG, "onDownloadStart: "+mDownloadListener);
                            if (mDownloadListener != null) {
                                mDownloadListener.onDownloadStart(l, 0, s, s1);
                            }
                        }

                        @Override
                        public void onDownloadActive(long l, long l1, String s, String s1) {
                            Log.i(TAG, "onDownloadActive: " + l + " " + l1+" mDownloadListener = "+mDownloadListener);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadUpdate(l, l1, s, s1, 0);
                        }

                        @Override
                        public void onDownloadPaused(long l, long l1, String s, String s1) {
                            Log.i(TAG, "onDownloadPaused: " + l + " " + l1);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadPause(l, l1, s, s1);
                        }

                        @Override
                        public void onDownloadFailed(long l, long l1, String s, String s1) {
                            Log.i(TAG, "onDownloadFailed: " + l + " " + l1);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadFail(l, l1, s, s1);
                        }

                        @Override
                        public void onDownloadFinished(long l, String s, String s1) {
                            Log.i(TAG, "onDownloadFinished: " + l + " " + s1);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadFinish(l, l, s, s1);
                        }

                        @Override
                        public void onInstalled(String s, String s1) {
                            Log.i(TAG, "onInstalled: " + s + " " + s1);
                            if (mDownloadListener != null)
                                mDownloadListener.onInstalled(0, 0, s, s1);
                        }
                    });
                    nativeExpressAd.render();
                }
            });
        } else {
            if (isC2SBidding && isBiddingLoaded && mKlevinNativeAd != null) {
                int mediaMode = nativeAd.getMediaMode();
                if (mediaMode == NativeAd.MEDIA_MODE_VIDEO || mediaMode == NativeAd.MEDIA_MODE_VERTICAL_VIDEO) {
                    Log.i(TAG, "自渲染素材类型 VIDEO");
                    nativeAd.setMute(isVideoSoundEnable);
                }
                downloadAndCallback(mKlevinNativeAd, mNeedDownloadImg);
                return;
            }

            Log.i(TAG, "request 自渲染: ");
            NativeAdRequest.Builder builder = new NativeAdRequest.Builder();
            builder.setPosId(mPostId).setAdCount(1);
            NativeAd.load(builder.build(), new NativeAd.NativeAdLoadListener() {
                @Override
                public void onAdLoadError(int err, String msg) {
                    //加载失败，err是错误码，msg是描述信息
                    Log.i(TAG, "onAdLoadError: errcode :" + err + ", msg :" + msg);
                    loadC2SBiddingFailed(err, msg);
                }

                @Override
                public void onAdLoaded(List<NativeAd> ads) {
                    if (ads == null) return;
                    //加载成功，参数ads为自渲染广告数组实例
                    Log.i(TAG, "onAdLoaded: ");
                    mNativeAds = ads;
                    nativeAd = ads.get(0);
                    //设置apk下载监听接口
                    nativeAd.setDownloadListener(new AppDownloadListener() {
                        @Override
                        public void onIdle() {

                        }

                        @Override
                        public void onDownloadStart(long l, String s, String s1) {
                            Log.i(TAG, "onDownloadStart: "+mDownloadListener);
                            if (mDownloadListener != null) {
                                mDownloadListener.onDownloadStart(l, 0, s, s1);
                            }
                        }

                        @Override
                        public void onDownloadActive(long l, long l1, String s, String s1) {
                            Log.i(TAG, "onDownloadActive: " + l + " " + l1+" mDownloadListener = "+mDownloadListener);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadUpdate(l, l1, s, s1, 0);
                        }

                        @Override
                        public void onDownloadPaused(long l, long l1, String s, String s1) {
                            Log.i(TAG, "onDownloadPaused: " + l + " " + l1);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadPause(l, l1, s, s1);
                        }

                        @Override
                        public void onDownloadFailed(long l, long l1, String s, String s1) {
                            Log.i(TAG, "onDownloadFailed: " + l + " " + l1);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadFail(l, l1, s, s1);
                        }

                        @Override
                        public void onDownloadFinished(long l, String s, String s1) {
                            Log.i(TAG, "onDownloadFinished: " + l + " " + s1);
                            if (mDownloadListener != null)
                                mDownloadListener.onDownloadFinish(l, l, s, s1);
                        }

                        @Override
                        public void onInstalled(String s, String s1) {
                            Log.i(TAG, "onInstalled: " + s + " " + s1);
                            if (mDownloadListener != null)
                                mDownloadListener.onInstalled(0, 0, s, s1);
                        }
                    });

                    mKlevinNativeAd = new KlevinNativeAd(nativeAd, TPBaseAd.AD_TYPE_NORMAL_NATIVE);

                    if (isC2SBidding) {
                        if (onC2STokenListener != null) {
                            ecpmLevel = nativeAd.getECPM();
                            Log.i(TAG, "原生bid price: " + ecpmLevel);
                            if (TextUtils.isEmpty(ecpmLevel + "")) {
                                onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is empty");
                                return;
                            }
                            onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                        }
                        isBiddingLoaded = true;
                        return;
                    }
                    int mediaMode = nativeAd.getMediaMode();
                    if (mediaMode == NativeAd.MEDIA_MODE_IMAGE ||
                            mediaMode == NativeAd.MEDIA_MODE_VERTICAL_IMAGE) {
                        Log.i(TAG, "自渲染素材类型IMAGE onAdLoaded");
                        downloadAndCallback(mKlevinNativeAd, mNeedDownloadImg);
                    } else if (mediaMode == NativeAd.MEDIA_MODE_VIDEO ||
                            mediaMode == NativeAd.MEDIA_MODE_VERTICAL_VIDEO) {
                        // 视频广告设置静音
                        nativeAd.setMute(isVideoSoundEnable);
                        /* 视频自动播放策略
                         * 服务器下发：0 代表wifi网络下；1，代表总是自动播放。
                         * AUTO_PLAY_POLICY_WIFI = 0; WiFi下自动播放
                         * AUTO_PLAY_POLICY_ALWAYS = 1; 视频自动播放策略，总是自动播放，无论什么网络条件
                         * AUTO_PLAY_POLICY_NEVER = 2; 视频自动播放策略，从不自动播放，无论什么网络条件
                         * */
                        nativeAd.setAutoPlayPolicy(autoPlayVideo);
                        // 视频广告设置视频播放监听
                        nativeAd.setVideoAdListener(mVideoAdListener);
                    }

                }
            });
        }
    }

    // 模版
    private final NativeExpressAd.AdInteractionListener mAdInteractionListener = new NativeExpressAd.AdInteractionListener() {
        @Override
        public void onRenderSuccess(View adView, float width, float height) {
            // 原生模版广告View渲染成功后会回调该方法，之后可把模版View添加在布局中进行显示
            if (!nativeExpressAd.isVideoAd()) {
                Log.i(TAG, "onRenderSuccess: ");
                mView = adView;
                loadExpressC2SBiddingSuccess(nativeExpressAd, adView);
            } else {
                Log.i(TAG, "is videoAd: ");
            }

        }

        @Override
        public void onRenderFailed(View adView, int error, String msg) {
            Log.i(TAG, "onRenderFailed: errcode :" + error + ", msg :" + msg);
            loadC2SBiddingFailed(error, msg);
        }

        @Override
        public void onAdShow(View adView) {
            Log.i(TAG, "onAdShow: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdViewExpanded();
            }
        }

        @Override
        public void onAdClick(View adView) {
            Log.i(TAG, "onAdClick: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdViewClick();
            }
        }

        @Override
        public void onAdClose(View adView) {
            Log.i(TAG, "onAdClose: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdViewClosed();
            }
            clean();

        }

        @Override
        public void onAdDetailClosed(int interactionType) {
        }
    };

    // 自渲染 视频监听
    private final NativeAd.VideoAdListener mVideoAdListener = new NativeAd.VideoAdListener() {
        @Override
        public void onVideoCached(NativeAd nativeAd) {
            //视频文件下载完成
            Log.i(TAG, "自渲染素材类型VIDEO onAdLoaded");
            downloadAndCallback(mKlevinNativeAd, mNeedDownloadImg);
        }

        @Override
        public void onVideoLoad(NativeAd nativeAd) {
            //播放器prepare完成
            Log.i(TAG, "onVideoLoad: ");
        }

        @Override
        public void onVideoError(int i, int i1) {
            Log.i(TAG, "onVideoError: code :" + i);
            if (mKlevinNativeAd != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(i + "");
                mKlevinNativeAd.onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoStartPlay(NativeAd nativeAd) {
            Log.i(TAG, "onVideoStartPlay: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdVideoStart();
            }

        }

        @Override
        public void onVideoPaused(NativeAd nativeAd) {
            Log.i(TAG, "onVideoPaused: ");
        }

        @Override
        public void onProgressUpdate(long l, long l1) {

        }

        @Override
        public void onVideoComplete(NativeAd nativeAd) {
            Log.i(TAG, "onVideoComplete: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdVideoEnd();
            }
        }
    };

    // 模版监听回调
    private final NativeExpressAd.VideoAdListener mExpressVideoAdListener = new NativeExpressAd.VideoAdListener() {
        @Override
        public void onVideoCached(View view) {
            //视频文件下载完成
            Log.i(TAG, "onVideoCached: VIDEO");
            mView = view;
            loadExpressC2SBiddingSuccess(nativeExpressAd, view);
        }

        @Override
        public void onVideoLoad(View view) {

        }

        @Override
        public void onVideoError(View view, int i, int i1) {
            Log.i(TAG, "onVideoError: code :" + i);
            if (mKlevinNativeAd != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(i + "");
                mKlevinNativeAd.onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoStartPlay(View view) {
            Log.i(TAG, "onVideoStartPlay: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdVideoStart();
            }
        }

        @Override
        public void onVideoPaused(View view) {

        }

        @Override
        public void onProgressUpdate(View view, long l, long l1) {

        }

        @Override
        public void onVideoComplete(View view) {
            Log.i(TAG, "onVideoComplete: ");
            if (mKlevinNativeAd != null) {
                mKlevinNativeAd.onAdVideoEnd();
            }
        }
    };

    private void loadC2SBiddingFailed(int error, String msg) {
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                onC2STokenListener.onC2SBiddingFailed(error+"",msg);
            }
            return;
        }

        TPError tpError = new TPError(NETWORK_NO_FILL);
        tpError.setErrorCode(error + "");
        tpError.setErrorMessage(msg);
        if (mLoadAdapterListener != null)
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
    }

    private void loadExpressC2SBiddingSuccess(NativeExpressAd nativeAd, View view) {
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                ecpmLevel = nativeAd.getECPM();
                Log.i(TAG, "原生bid price: " + ecpmLevel);
                if (TextUtils.isEmpty(ecpmLevel + "")) {
                    onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is empty");
                    return;
                }
                onC2STokenListener.onC2SBiddingResult(ecpmLevel);
            }
            isBiddingLoaded = true;
            return;
        }
        if (mLoadAdapterListener != null) {
            mKlevinNativeAd = new KlevinNativeAd(view, TPBaseAd.AD_TYPE_NATIVE_EXPRESS);
            mLoadAdapterListener.loadAdapterLoaded(mKlevinNativeAd);
        }
    }

    @Override
    public void clean() {
        //在页面销毁时销毁所有的广告对象
        if (mNativeAds != null) {
            for (NativeAd ad : mNativeAds) {
                ad.destroy();
            }
            mNativeAds = null;
        }

        if (nativeExpressAd != null) {
            nativeExpressAd.destroy();
            nativeExpressAd = null;
        }

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KLEVIN);
    }

    @Override
    public String getNetworkVersion() {
        return KlevinManager.getVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        TPTaskManager.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                loadCustomAd(context, localParams, tpParams);
            }
        });
    }
}
