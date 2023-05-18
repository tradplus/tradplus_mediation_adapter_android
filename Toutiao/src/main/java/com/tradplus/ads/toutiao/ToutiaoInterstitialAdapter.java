package com.tradplus.ads.toutiao;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class ToutiaoInterstitialAdapter extends TPInterstitialAdapter {

    private TTAdManager adManager;
    private TTAdNative mInterstitial;
    private static final long TIMEOUT_VALUE = 30 * 1000;
    private ToutiaoInterstitialCallbackRouter mToutiaoICbR;
    private static final String TAG = "ToutiaoInterstitial";
    private String placementId;
    private int mIsTemplateRending, mAdsizeRatio;
    private int mInterstitialType;
    private Integer direction;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private double ecpmLevel;
    private OnC2STokenListener onC2STokenListener;

    @Override
    public void loadCustomAd(final Context context, final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        // C2S true LoadAdapterListener == null
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            mInterstitialType = Integer.parseInt(serverExtras.get(AppKeyManager.FULL_SCREEN_TYPE));
            direction = Integer.valueOf(serverExtras.get(AppKeyManager.DIRECTION));
            String adsizeRatio = serverExtras.get(ToutiaoConstant.ADSIZE_RATIO + placementId);
            String template = serverExtras.get(AppKeyManager.IS_TEMPLATE_RENDERING);

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }
            if (!TextUtils.isEmpty(adsizeRatio)) {
                Log.i(TAG, "adsizeRatio: " + adsizeRatio + " ， placementId : " + placementId);
                mAdsizeRatio = Integer.parseInt(adsizeRatio);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("", ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }


        mToutiaoICbR = ToutiaoInterstitialCallbackRouter.getInstance();
        mToutiaoICbR.addListener(placementId, mLoadAdapterListener);


        adManager = TTAdSdk.getAdManager();
        mInterstitial = adManager.createAdNative(context);

        ToutiaoInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });


    }

    private void requestInterstitial(Context context) {
        Log.i(TAG, "full_screen_video: " + mInterstitialType);

        if (isC2SBidding && isBiddingLoaded) {
            if (mToutiaoICbR != null && mToutiaoICbR.getListener(placementId) != null) {
                mFullScreenVideoAd.win(ecpmLevel);
                setNetworkObjectAd(mFullScreenVideoAd);
                mToutiaoICbR.getListener(placementId).loadAdapterLoaded(null);
            }
            return;
        }

        if (mInterstitialType == AppKeyManager.FULL_TYPE) {
            // 全屏插屏 支持模版渲染和自渲染
            intFullScreen(context);
            // V4900起废弃API
//        } else if (mInterstitialType == AppKeyManager.INTERACTION_TYPE) {
//            // 插屏广告指支持模版渲染
////            initInterstitial(context);
        } else {
//            mInterstitialType == AppKeyManager.INTERSTITIAL_TYPE
            // 新插屏广告 支持模版
            initNewInterstitial(context);
        }
    }

    private void initNewInterstitial(Context context) {
        final AdSlot.Builder builder = new AdSlot.Builder()
                .setCodeId(placementId)
                .setSupportDeepLink(true)
                .setOrientation(direction == 2 ? TTAdConstant.HORIZONTAL : TTAdConstant.VERTICAL);

        //模板广告需要设置期望个性化模板广告的大小,单位dp,激励视频场景，只要设置的值大于0即可
        if (mAdsizeRatio == 1) {
            builder.setExpressViewAcceptedSize(ToutiaoConstant.EXPRESSVIEW_WIDTH1, ToutiaoConstant.EXPRESSVIEW_HEIGHT1);
            Log.i(TAG, "初始化新插屏广告 initInterstitial ，尺寸选择 1:1");
        } else if (mAdsizeRatio == 2) {
            builder.setExpressViewAcceptedSize(ToutiaoConstant.EXPRESSVIEW_WIDTH3, ToutiaoConstant.EXPRESSVIEW_HEIGHT3);
            Log.i(TAG, "初始化新插屏广告 initInterstitial ，尺寸选择 3:2");
        } else if (mAdsizeRatio == 3) {
            builder.setExpressViewAcceptedSize(ToutiaoConstant.EXPRESSVIEW_WIDTH2, ToutiaoConstant.EXPRESSVIEW_HEIGHT2);
            Log.i(TAG, "初始化新插屏广告 initInterstitial ，尺寸选择 2:3");
        }

        AdSlot adSlot = builder.build();

        mInterstitial.loadFullScreenVideoAd(adSlot, fullScreenVideoAdListener);

    }

    private void intFullScreen(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        final AdSlot.Builder builder = new AdSlot.Builder()
                .setCodeId(placementId)
                .setSupportDeepLink(true)
                .setOrientation(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? TTAdConstant.HORIZONTAL : TTAdConstant.VERTICAL);
        Log.i(TAG, "loadInterstitial: " + mIsTemplateRending);
        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            builder.setExpressViewAcceptedSize(ToutiaoConstant.EXPRESS_VIEW_ACCEPTED_SIZE, ToutiaoConstant.EXPRESS_VIEW_ACCEPTED_SIZE);
        }
        AdSlot adSlot = builder.build();

        if (mInterstitial != null) {
            mInterstitial.loadFullScreenVideoAd(adSlot, fullScreenVideoAdListener);
        }
    }

    private TTFullScreenVideoAd mFullScreenVideoAd;

    TTAdNative.FullScreenVideoAdListener fullScreenVideoAdListener = new TTAdNative.FullScreenVideoAdListener() {
        @Override
        public void onError(int errorCode, String errorMsg) {
            Log.i(TAG, "onError: " + errorCode + ":msg:" + errorMsg);

            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(errorCode + "", errorMsg);
                }
                return;
            }

            if (mToutiaoICbR.getListener(placementId) != null) {
                mToutiaoICbR.getListener(placementId).loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
            }
        }

        @Override
        public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ttFullScreenVideoAd) {
            // 广告物料加载完成的回调
            Log.i(TAG, "onFullScreenVideoAdLoad: Timestamp :" + ttFullScreenVideoAd.getExpirationTimestamp());
            mFullScreenVideoAd = ttFullScreenVideoAd;

            if (ttFullScreenVideoAd != null) {
                ttFullScreenVideoAd.setFullScreenVideoAdInteractionListener(fullScreenVideoAdInteractionListener);
                ttFullScreenVideoAd.setDownloadListener(downloadListener);
            }
            setFirstLoadedTime();
        }

        @Override
        public void onFullScreenVideoCached() {
            // 已废弃 请使用 onFullScreenVideoCached(TTFullScreenVideoAd ad) 方法
        }

        @Override
        public void onFullScreenVideoCached(TTFullScreenVideoAd ttFullScreenVideoAd) {
            //广告物料加载完成的回调
            Log.i(TAG, "onFullScreenVideoCached: ");
            // 广告视频/图片加载完成的回调，接入方可以在这个回调后展示广告
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    Map<String, Object> mediaExtraInfo = mFullScreenVideoAd.getMediaExtraInfo();
                    Integer price = (Integer)mediaExtraInfo.get("price");
                    Log.i(TAG, "price: "  + price);
                    if (price == null) {
                        onC2STokenListener.onC2SBiddingFailed("","price == null");
                        return;
                    }
                    ecpmLevel = price.doubleValue();
                    onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                }
                isBiddingLoaded = true;
                return;
            }

            if (mToutiaoICbR.getListener(placementId) != null) {
                setNetworkObjectAd(mFullScreenVideoAd);
                mToutiaoICbR.getListener(placementId).loadAdapterLoaded(null);
            }
        }

    };


    /**
     * 竞价失败时的上报接⼝（必传）
     * auctionPrice 胜出者的第⼀名价格（不想上报价格传时null），单位是分
     * lossReason 竞价失败的原因（不想上报原因时传null），可参考枚举值或者媒体⾃定义回传
     * winBidder 胜出者（不想上报胜出者时传null），可参考枚举值或者媒体⾃定义回传
     * 102 bid价格低于最高价
     */
    @Override
    public void setLossNotifications(String auctionPrice, String lossReason) {
        if (mFullScreenVideoAd != null) {
            try {
                mFullScreenVideoAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private boolean isDownLoadStart;
    private TTAppDownloadListener downloadListener = new TTAppDownloadListener() {
        @Override
        public void onIdle() {

        }

        @Override
        public void onDownloadActive(long l, long l1, String s, String s1) {
            if (mDownloadListener != null && !isDownLoadStart) {
                isDownLoadStart = true;
                mDownloadListener.onDownloadStart(l, l1, s, s1);
            }
            Log.i(TAG, "onDownloadActive: " + l + " " + l1);
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
    };

    TTFullScreenVideoAd.FullScreenVideoAdInteractionListener fullScreenVideoAdInteractionListener = new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
        @Override
        public void onAdShow() {
            if (mToutiaoICbR.getShowListener(placementId) == null) {
                return;
            }

            Log.i(TAG, "onAdShow: ");
            mToutiaoICbR.getShowListener(placementId).onAdVideoStart();
            mToutiaoICbR.getShowListener(placementId).onAdShown();

        }

        @Override
        public void onAdVideoBarClick() {
            Log.i(TAG, "onAdVideoBarClick: ");
            if (mToutiaoICbR.getShowListener(placementId) != null) {
                mToutiaoICbR.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdClose() {
            Log.i(TAG, "onAdClose: ");
            if (mToutiaoICbR.getShowListener(placementId) != null) {
                mToutiaoICbR.getShowListener(placementId).onAdClosed();
            }
        }

        @Override
        public void onVideoComplete() {
            Log.i(TAG, "onVideoComplete: ");
            if (mToutiaoICbR.getShowListener(placementId) != null) {
                mToutiaoICbR.getShowListener(placementId).onAdVideoEnd();
            }
        }

        @Override
        public void onSkippedVideo() {
            Log.i(TAG, "onSkippedVideo: ");
        }
    };

    @Override
    public void showAd() {
        if (mShowListener == null) return;
        mToutiaoICbR.addShowListener(placementId, mShowListener);

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mToutiaoICbR.getShowListener(placementId) == null) return;

        if (mFullScreenVideoAd == null) {
            mToutiaoICbR.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }

        // 全屏插屏和新插屏API一样
        mFullScreenVideoAd.showFullScreenVideoAd(activity);
    }

    @Override
    public void clean() {
        super.clean();
        if (mFullScreenVideoAd != null) {
            mFullScreenVideoAd.setFullScreenVideoAdInteractionListener(null);
            mFullScreenVideoAd = null;
        }

        if (placementId != null) {
            mToutiaoICbR.removeListeners(placementId);
        }
    }

    @Override
    public boolean isReady() {
        if (mFullScreenVideoAd != null) {
            long expirationTimestamp = mFullScreenVideoAd.getExpirationTimestamp();
            return (SystemClock.elapsedRealtime() < (expirationTimestamp - TIMEOUT_VALUE));
        }
        return false;

    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_PANGLECN);
    }

    @Override
    public String getNetworkVersion() {
        if (adManager != null) {
            return adManager.getSDKVersion();
        }
        return null;
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }
}