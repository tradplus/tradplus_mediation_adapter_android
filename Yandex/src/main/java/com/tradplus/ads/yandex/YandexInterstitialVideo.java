package com.tradplus.ads.yandex;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.BidderTokenLoadListener;
import com.yandex.mobile.ads.common.BidderTokenLoader;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

import java.util.Map;

public class YandexInterstitialVideo extends TPRewardAdapter {

    private String placementId, userId, customData, payload;
    private InterstitialCallbackRouter mCallbackRouter;
    private RewardedAd mRewardedAd;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private int onRewardedAdShow = 0; // 0 表示没有展示
    private static final String TAG = "YandexRewardVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);
        mLoadAdapterListener = mCallbackRouter.getListener(placementId);


        YandexInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestRewardVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });
    }

    private void requestRewardVideo(Context context) {
        mRewardedAd = new RewardedAd(context);
        mRewardedAd.setAdUnitId(placementId);
        mRewardedAd.setRewardedAdEventListener(mRewardedAdEventListener);
        // Creating an ad targeting object.
        AdRequest.Builder builder = new AdRequest.Builder();
        if (!TextUtils.isEmpty(payload)) {
            Log.i(TAG, "payload: " + payload);
            builder.setBiddingData(payload);
        }
        mRewardedAd.loadAd(builder.build());
    }

    private final RewardedAdEventListener mRewardedAdEventListener = new RewardedAdEventListener() {
        @Override
        public void onAdLoaded() {
            Log.i(TAG, "onAdLoaded: ");
            setFirstLoadedTime();
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mRewardedAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdFailedToLoad( AdRequestError adRequestError) {
            Log.i(TAG, "onAdFailedToLoad: ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            if (adRequestError != null) {
                int code = adRequestError.getCode();
                String description = adRequestError.getDescription();
                tpError.setErrorMessage(description);
                tpError.setErrorCode(code+"");
                Log.i(TAG, "code :" + code + ", description :" + description);
            }

            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }

        @Override
        public void onAdShown() {
            if (mCallbackRouter != null &&
                    mCallbackRouter.getShowListener(placementId) != null) {
                Log.i(TAG, "onAdShown: ");
                mCallbackRouter.getShowListener(placementId).onAdVideoStart();

                if (onRewardedAdShow == 0) {
                    mCallbackRouter.getShowListener(placementId).onAdShown();
                    onRewardedAdShow = 1;
                }
            }
        }

        @Override
        public void onAdDismissed() {
            if (mCallbackRouter.getShowListener(placementId) == null) {
                return;
            }
            Log.i(TAG, "onAdDismissed: ");
            mCallbackRouter.getShowListener(placementId).onAdVideoEnd();
            if (hasGrantedReward || alwaysRewardUser) {
                mCallbackRouter.getShowListener(placementId).onReward();
            }
            mCallbackRouter.getShowListener(placementId).onAdClosed();
        }

        @Override
        public void onRewarded( Reward reward) {
            Log.i(TAG, "onRewarded: ");
            hasGrantedReward = true;
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter != null && mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onLeftApplication() {

        }

        @Override
        public void onReturnedToApplication() {

        }

        @Override
        public void onImpression(ImpressionData impressionData) {

        }
    };

    @Override
    public void showAd() {
        if (mShowListener == null) return;
        mCallbackRouter.addShowListener(placementId, mShowListener);

        TPError tpErrorShow = new TPError(UNSPECIFIED);
        if (mRewardedAd == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                tpErrorShow.setErrorMessage("showFailed: RewardedAd == null");
                mCallbackRouter.getShowListener(placementId).onAdVideoError(tpErrorShow);
            }
            return;
        }

        mRewardedAd.show();
    }

    @Override
    public void clean() {
        if (mRewardedAd != null) {
            mRewardedAd.setRewardedAdEventListener(null);
            mRewardedAd.destroy();
            mRewardedAd = null;
        }

        if (placementId != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }

    @Override
    public boolean isReady() {
        return mRewardedAd != null && mRewardedAd.isLoaded() && !isAdsTimeOut();
    }

    @Override
    public String getNetworkName() {
        return YandexInitManager.TAG_YANDEX;
    }

    @Override
    public String getNetworkVersion() {
        return MobileAds.getLibraryVersion();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        BidderTokenLoader.loadBidderToken(context, new BidderTokenLoadListener() {
            @Override
            public void onBidderTokenLoaded(final String bidderToken) {
                Log.i(TAG, "onBidderTokenLoaded: ");
                onS2STokenListener.onTokenResult(bidderToken, null);
            }

            @Override
            public void onBidderTokenFailedToLoad(final String failureReason) {
                Log.i(TAG, "onBidderTokenFailedToLoad: ");
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }
}
