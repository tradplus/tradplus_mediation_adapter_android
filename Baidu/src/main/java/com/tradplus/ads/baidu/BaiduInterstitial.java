package com.tradplus.ads.baidu;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.CACHE_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RelativeLayout;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.ExpressInterstitialAd;
import com.baidu.mobads.sdk.api.ExpressInterstitialListener;
import com.baidu.mobads.sdk.api.FullScreenVideoAd;
import com.baidu.mobads.sdk.api.InterstitialAd;
import com.baidu.mobads.sdk.api.InterstitialAdListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class BaiduInterstitial extends TPInterstitialAdapter {

    private String mPlacementId, mAppId;
    private BaiduInterstitialCallbackRouter mCallbackRouter;
    private FullScreenVideoAd mFullScreenVideoAd; // 全屏插屏
    private InterstitialAd mInterstitialAd; // 插屏广告
    private ExpressInterstitialAd mExpressInterstitialAd; //新模版插屏
    private int mInterstitialType;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "BaiduInterstitial";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mAppId = tpParams.get(AppKeyManager.APP_ID);
            mInterstitialType = Integer.parseInt(tpParams.get(AppKeyManager.FULL_SCREEN_TYPE));
        } else {
            loadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = BaiduInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        BaiduInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestInterstitial(context);
            }


            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void loadFailed(TPError tpError) {
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                if (tpError != null) {
                    onC2STokenListener.onC2SBiddingFailed(tpError.getErrorCode(),tpError.getEmsg());
                }else {
                    onC2STokenListener.onC2SBiddingFailed("","");
                }
            }
        } else {
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
            }
        }
    }

    private void loadSuccess() {
        setFirstLoadedTime();
        // 非C2Sloaded 或者 是C2S，loaded成功的
        if (!isC2SBidding || isBiddingLoaded) {
            if (mCallbackRouter != null && mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mInterstitialType == AppKeyManager.FULL_TYPE ? mFullScreenVideoAd == null : mExpressInterstitialAd);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        }

        if (onC2STokenListener != null) {
            String ecpmLevel = null;
            if (mInterstitialType == AppKeyManager.FULL_TYPE && mFullScreenVideoAd != null) {
                ecpmLevel = mFullScreenVideoAd.getECPMLevel();
                Log.i(TAG, "全屏视频 bid price: " + ecpmLevel);
            } else if (mExpressInterstitialAd != null) {
                ecpmLevel = mExpressInterstitialAd.getECPMLevel();
                Log.i(TAG, "新模版插屏 bid price: " + ecpmLevel);
            }

            if (TextUtils.isEmpty(ecpmLevel)) {
                loadFailed(null);
                return;
            }
            onC2STokenListener.onC2SBiddingResult(Double.parseDouble(ecpmLevel));
        }
        isBiddingLoaded = true;
    }

    private void requestInterstitial(Context context) {
        if (mInterstitialType == AppKeyManager.FULL_TYPE) {
            // 全屏视频
            intFullScreen(context);
        } else if (mInterstitialType == AppKeyManager.INTERACTION_TYPE) {
            // 插屏 —— 不支持C2S
            intInterstitial();
        } else {
            // 支持新模版插屏 下发 "full_screen_video": 3
            initExpressInterstitial(context);
        }
    }

    // 全屏视频
    private void intFullScreen(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            loadSuccess();
            return;
        }

        if (mFullScreenVideoAd == null) {
            //useSurfaceView : 是否使用SurfaceView，默认使用TextureView
            mFullScreenVideoAd = new FullScreenVideoAd(context, mPlacementId, mFullScreenVideoAdListener, false);
        }
        Log.i(TAG, "FullScreenVideoAd: 请求全屏视频");
        mFullScreenVideoAd.load();
    }

    // 新模版插屏
    private void initExpressInterstitial(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            loadSuccess();
            return;
        }

        mExpressInterstitialAd = new ExpressInterstitialAd(context, mPlacementId);
        mExpressInterstitialAd.setLoadListener(mExpressInterstitialListener);
        /** 这些为下载弹窗以及四要素的回调设置，无特别需要可以不进行设置 **/
        //  mExpressInterstitialAd.setDownloadListener(adDownloadListener);
        // 设置下载弹窗，默认为false
        mExpressInterstitialAd.setDialogFrame(true);
        Log.i(TAG, "ExpressInterstitial: 请求新模版插屏");
        mExpressInterstitialAd.load();
    }

    private void intInterstitial() {
        Log.i(TAG, "intInterstitial: 请求插屏广告");
        Activity mActivity = GlobalTradPlus.getInstance().getActivity();
        if (mActivity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        // 视频插屏广告：初始化展示布局
        final RelativeLayout parentLayout = new RelativeLayout(mActivity);
        RelativeLayout.LayoutParams parentLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        parentLayout.setLayoutParams(parentLayoutParams);

        RelativeLayout mVideoAdLayout = new RelativeLayout(mActivity);
        RelativeLayout.LayoutParams reLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        reLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        parentLayout.addView(mVideoAdLayout, reLayoutParams);

        if (mInterstitialAd == null) {
            mInterstitialAd = new InterstitialAd(mActivity, mPlacementId);
        }
        mInterstitialAd.setListener(new InterstitialAdListener() {
            @Override
            public void onAdReady() {
                Log.i(TAG, "onAdReady: ");
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(mInterstitialAd);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdPresent() {
                Log.i(TAG, "onAdPresent: ");
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdShown();
                }
            }

            @Override
            public void onAdClick(InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdClick: ");
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
                }
            }

            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed: ");
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
                }
            }

            @Override
            public void onAdFailed(String s) {
                Log.i(TAG, "onAdFailed: " + s);
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                loadFailed(tpError);
            }
        });

        mInterstitialAd.loadAd();
    }

    @Override
    public void showAd() {
        if (mShowListener != null)
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);


        if (mInterstitialType == AppKeyManager.FULL_TYPE) {
            if (mFullScreenVideoAd == null) {
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
                }
                return;
            }

            Log.i(TAG, "showAd: FullScreenVideo全屏视频");
            mFullScreenVideoAd.show();

        } else if (mInterstitialType == AppKeyManager.INTERACTION_TYPE) {
            if (mInterstitialAd == null) {
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
                }
                return;
            }

            Log.i(TAG, "showAd: Interstitial插屏广告");
            mInterstitialAd.showAd();
        } else {
            if (mExpressInterstitialAd == null) {
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
                }
                return;
            }


            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }

            Log.i(TAG, "showAd: ExpressInterstitial新模版插屏");
            mExpressInterstitialAd.show(activity);
        }
    }


    @Override
    public boolean isReady() {
        if (mInterstitialType == AppKeyManager.FULL_TYPE) {
            return mFullScreenVideoAd != null && mFullScreenVideoAd.isReady() && !isAdsTimeOut();
        } else if (mInterstitialType == AppKeyManager.INTERACTION_TYPE) {
            return mInterstitialAd != null && mInterstitialAd.isAdReady() && !isAdsTimeOut();
        } else {
            // 通过isReady接口来判断广告是否有效且缓存成功
            return mExpressInterstitialAd != null && mExpressInterstitialAd.isReady() && !isAdsTimeOut();
        }
    }


    // 新模版插屏
    private final ExpressInterstitialListener mExpressInterstitialListener = new ExpressInterstitialListener() {
        @Override
        public void onADLoaded() {
            // 广告在回调onADLoaded后，才可调用show接口展示插屏广告
            if (mExpressInterstitialAd.isReady()) {
                loadSuccess();
            }
            Log.i(TAG, "onADLoaded: " + mExpressInterstitialAd.isReady());
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdClose() {
            Log.i(TAG, "onAdClose");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }

        @Override
        public void onAdFailed(int errorCode, String message) {
            Log.i(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(errorCode + "");
            tpError.setErrorMessage(message);
            loadFailed(tpError);
        }

        @Override
        public void onNoAd(int errorCode, String message) {
            Log.i(TAG, "onNoAd reason:" + message + "errorCode:" + errorCode);
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(errorCode + "");
            tpError.setErrorMessage(message);
            loadFailed(tpError);
        }

        @Override
        public void onADExposed() {
            Log.i(TAG, "onADExposed");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onADExposureFailed() {
            Log.i(TAG, "onADExposureFailed");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }

        @Override
        public void onAdCacheSuccess() {
            Log.i(TAG, "onAdCacheSuccess :");
            loadSuccess();
        }

        @Override
        public void onAdCacheFailed() {
            Log.i(TAG, "onAdCacheFailed: ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage("onVideoDownloadFailed 视频缓存失败");
            loadFailed(tpError);
        }

        @Override
        public void onVideoDownloadSuccess() {
            // 【废弃】插屏接口：ExpressInterstitialListeneronAd废弃onVideoDownloadSuccess回调和onVideoDownloadFailed回调
        }

        @Override
        public void onVideoDownloadFailed() {
            // 【废弃】

        }

        @Override
        public void onLpClosed() {
            Log.i(TAG, "onLpClosed");
        }
    };

    // 全屏视频监听
    FullScreenVideoAd.FullScreenVideoAdListener mFullScreenVideoAdListener = new FullScreenVideoAd.FullScreenVideoAdListener() {
        @Override
        public void onAdShow() {
            Log.i(TAG, "onAdShow: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
            }

            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdClose(float v) {
            // 用户关闭了广告
            // 说明：关闭按钮在mssp上可以动态配置，媒体通过mssp配置，可以选择广告一开始就展示关闭按钮，还是播放结束展示关闭按钮
            // 建议：收到该回调之后，可以重新load下一条广告,最好限制load次数（4-5次即可）
            Log.i(TAG, "onAdClose: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }

        @Override
        public void onAdFailed(String s) {
            // 广告失败回调 原因：广告内容填充为空；网络原因请求广告超时
            // 建议：收到该回调之后，可以重新load下一条广告，最好限制load次数（4-5次即可）
            Log.i(TAG, "onAdFailed: " + s);
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onVideoDownloadSuccess() {
            // 视频缓存成功
            // 说明：如果想一定走本地播放，那么收到该回调之后，可以调用show
            Log.i(TAG, "onVideoDownloadSuccess: 视频缓存成功");
            loadSuccess();
        }

        @Override
        public void onVideoDownloadFailed() {
            Log.i(TAG, "onVideoDownloadFailed: 视频缓存失败");
            loadFailed(new TPError(CACHE_FAILED));
        }

        @Override
        public void playCompletion() {
            Log.i(TAG, "playCompletion: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
            }
        }

        @Override
        public void onAdSkip(float v) {
            Log.i(TAG, "onAdSkip: ");

        }

        @Override
        public void onAdLoaded() {
            Log.i(TAG, "onAdLoaded: ");
        }
    };

    @Override
    public void clean() {
        super.clean();
        if (mFullScreenVideoAd != null) {
            mFullScreenVideoAd = null;
        }

        if (mFullScreenVideoAdListener != null) {
            mFullScreenVideoAdListener = null;
        }

        if (mInterstitialAd != null) {
            mInterstitialAd.setListener(null);
            mInterstitialAd.destroy();
            mInterstitialAd = null;
        }

        if (mExpressInterstitialAd != null) {
            mExpressInterstitialAd.setLoadListener(null);
            mExpressInterstitialAd.destroy();
            mExpressInterstitialAd = null;
        }

        if (mPlacementId != null)
            mCallbackRouter.removeListeners(mPlacementId);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_BAIDU);
    }

    @Override
    public String getNetworkVersion() {
        return AdSettings.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

}


