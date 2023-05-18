package com.tradplus.ads.toutiao;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class ToutiaoRewardVideoAdapter extends TPRewardAdapter {

    private TTAdManager adManager;
    private TTAdNative mAdNative;
    private TTRewardVideoAd mttRewardVideoAd;
    private ToutiaoInterstitialCallbackRouter mToutiaoICbR;
    private String placementId, userId, customData;
    public static final String TAG = "ToutiaoRewardVideo";
    private static final long TIMEOUT_VALUE = 30 * 1000;
    private int mIsTemplateRending;
    private boolean canAgain;
    private boolean alwaysRewardUser = false; // 默认值不开启
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private double ecpmLevel;
    private OnC2STokenListener onC2STokenListener;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        // C2S true LoadAdapterListener == null
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            String template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }

            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
                if (TextUtils.isEmpty(userId)) {
                    userId = "";
                }
            }

            if (userParams.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) userParams.get(AppKeyManager.CUSTOM_DATA);
                if (TextUtils.isEmpty(customData)) {
                    customData = "";
                }
            }

            if (userParams.containsKey(ToutiaoConstant.AD_REWARD_AGAIN)) {
                canAgain = true;
            }
        }

        mToutiaoICbR = ToutiaoInterstitialCallbackRouter.getInstance();
        mToutiaoICbR.addListener(placementId, mLoadAdapterListener);
        mLoadAdapterListener = mToutiaoICbR.getListener(placementId);

        adManager = TTAdSdk.getAdManager();
        mAdNative = adManager.createAdNative(context);


        ToutiaoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                initRewardVideo(context);
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

    private void initRewardVideo(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mToutiaoICbR != null && mToutiaoICbR.getListener(placementId) != null) {
                // 竞价成功时的上报接⼝（必传），单位是分
                mttRewardVideoAd.win(ecpmLevel);
                setNetworkObjectAd(mttRewardVideoAd);
                mToutiaoICbR.getListener(placementId).loadAdapterLoaded(null);
            }
            return;
        }

        int ori = context.getResources().getConfiguration().orientation; //获取屏幕方向
        Log.i(TAG, "RewardData: userId : " + userId + ", customData :" + customData);
        final AdSlot.Builder builder = new AdSlot.Builder()
                .setCodeId(placementId)
                .setSupportDeepLink(true)
                .setAdCount(canAgain ? 2 : 1)
                .setImageAcceptedSize(ToutiaoConstant.IMAGE_ACCEPTED_SIZE_X, ToutiaoConstant.IMAGE_ACCEPTED_SIZE_Y)
                //必传参数，表来标识应用侧唯一用户；若非服务器回调模式或不需sdk透传
                //可设置为空字符串
                .setUserID(TextUtils.isEmpty(userId) ? "" : userId)
                .setMediaExtra(TextUtils.isEmpty(customData) ? "" : customData)
                .setOrientation(ori == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? TTAdConstant.HORIZONTAL : TTAdConstant.VERTICAL);
        Log.i(TAG, "initRewardVideo: " + mIsTemplateRending);
        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES || mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_DEFAULT) {
            //个性化模板广告需要设置期望个性化模板广告的大小,单位dp,激励视频场景，只要设置的值大于0即可
            builder.setExpressViewAcceptedSize(ToutiaoConstant.EXPRESS_VIEW_ACCEPTED_SIZE, ToutiaoConstant.EXPRESS_VIEW_ACCEPTED_SIZE);
        }
        AdSlot adSlot = builder.build();//设置期望视频播放的方向，为TTAdConstant.HORIZONTAL或TTAdConstant.VERTICAL

        mAdNative.loadRewardVideoAd(adSlot, mRewardVideoAdListener);
    }


    @Override
    public void showAd() {
        if (mShowListener == null) return;
        mToutiaoICbR.addShowListener(placementId, mShowListener);

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mToutiaoICbR.getShowListener(placementId) != null) {
                mToutiaoICbR.getShowListener(placementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mttRewardVideoAd == null) {
            if (mToutiaoICbR.getShowListener(placementId) != null) {
                mToutiaoICbR.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
            return;
        }

        mttRewardVideoAd.showRewardVideoAd(activity);

    }

    /**
     * 竞价失败时的上报接⼝（必传）
     * auctionPrice 胜出者的第⼀名价格（不想上报价格传时null），单位是分
     * lossReason 竞价失败的原因（不想上报原因时传null），可参考枚举值或者媒体⾃定义回传
     * winBidder 胜出者（不想上报胜出者时传null），可参考枚举值或者媒体⾃定义回传
     * 102 bid价格低于最高价
     */
    @Override
    public void setLossNotifications(String auctionPrice, String lossReason) {
        if (mttRewardVideoAd != null) {
            try {
                mttRewardVideoAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

    }

    TTAdNative.RewardVideoAdListener mRewardVideoAdListener = new TTAdNative.RewardVideoAdListener() {
        @Override
        public void onError(int i, String s) {
            Log.i(TAG, "onError errorcode: " + i + "errormsg" + s);
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(i+"",s);
                }
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(i, s));
            }
        }

        @Override
        public void onRewardVideoAdLoad(TTRewardVideoAd ttRewardVideoAd) {
            //视频广告的素材加载完毕，比如视频url等，在此回调后，可以播放在线视频，网络不好可能出现加载缓冲，影响体验。
            Log.i(TAG, "onRewardVideoAdLoad: Timestamp :" + ttRewardVideoAd.getExpirationTimestamp());
            mttRewardVideoAd = ttRewardVideoAd;
            mttRewardVideoAd.setRewardAdInteractionListener(new ToutiaoAdsInterstitialListener(placementId, alwaysRewardUser));
            mttRewardVideoAd.setDownloadListener(downloadListener);
            mttRewardVideoAd.setRewardPlayAgainInteractionListener(new ToutiaoAdsInterstitialListener(placementId, true, alwaysRewardUser));
        }

        @Override
        public void onRewardVideoCached() {
            // 已废弃 请使用 onRewardVideoCached(TTRewardVideoAd ad) 方法

        }

        @Override
        public void onRewardVideoCached(TTRewardVideoAd ttRewardVideoAd) {
            //视频广告加载后，视频资源缓存到本地的回调，在此回调后，播放本地视频，流畅不阻塞。
            setFirstLoadedTime();
            Log.i(TAG, "onRewardVideoCached: ");


            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    Map<String, Object> mediaExtraInfo = mttRewardVideoAd.getMediaExtraInfo();
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

            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mttRewardVideoAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }
    };

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

    @Override
    public boolean isReady() {
        if (mttRewardVideoAd != null) {
            long expirationTimestamp = mttRewardVideoAd.getExpirationTimestamp();
            return (SystemClock.elapsedRealtime() < (expirationTimestamp - TIMEOUT_VALUE));
        }
        return false;
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mttRewardVideoAd != null) {
            mttRewardVideoAd.setRewardAdInteractionListener(null);
            mttRewardVideoAd.setRewardPlayAgainInteractionListener(null);
            mttRewardVideoAd = null;
        }

        if (placementId != null) {
            mToutiaoICbR.removeListeners(placementId);
        }

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

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

}