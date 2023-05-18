package com.tradplus.ads.facebook;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.BuildConfig;
import com.facebook.ads.RewardData;
import com.facebook.ads.RewardedInterstitialAd;
import com.facebook.ads.RewardedInterstitialAdListener;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class FacebookInterstitialVideo extends TPRewardAdapter {

    private RewardedVideoAd mRewardedVideoAd;
    private String mPlacementId, payload, userId, customData;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private int isRewardedInterstitialAd;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private static final String TAG = "FacebookRewardedVideo";

    @Override
    public void loadCustomAd(final Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {

        if (extrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            if (serverExtras.containsKey(AppKeyManager.ADSOURCE_TYPE)) {
                isRewardedInterstitialAd = Integer.parseInt(serverExtras.get(AppKeyManager.ADSOURCE_TYPE));
            }

            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            userId = (String) localExtras.get(AppKeyManager.CUSTOM_USERID);
            customData = (String) localExtras.get(AppKeyManager.CUSTOM_DATA);

            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
            if (TextUtils.isEmpty(customData)) {
                customData = "";
            }
        }

        if (localExtras.size() > 0) {

            if (localExtras.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) localExtras.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                if (coppa) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
                    return;
                }
            }
        }

        FacebookInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void requestAd(Context context) {
        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
            Log.i(TAG, "load ");
            rewardedInterstitialAd = new RewardedInterstitialAd(context, mPlacementId);
            RewardedInterstitialAd.RewardedInterstitialAdLoadConfigBuilder configBuilder =
                    rewardedInterstitialAd.buildLoadAdConfig()
                            .withAdListener(rewardedInterstitialAdListener)
                            .withBid(TextUtils.isEmpty(payload) ? "" : payload);
            if (!TextUtils.isEmpty(userId)) {
                // Create the rewarded ad data
                RewardData rewardData = new RewardData(userId, TextUtils.isEmpty(customData) ? "" : customData);
                Log.i(TAG, "RewardData: userId : " + userId + " , customData : " + customData);
                configBuilder.withRewardData(rewardData);
            }
            rewardedInterstitialAd.loadAd(configBuilder.build());
        } else {
            Log.i(TAG, "load ");
            mRewardedVideoAd = new RewardedVideoAd(context, mPlacementId);
            RewardedVideoAd.RewardedVideoAdLoadConfigBuilder rewardedVideoAdLoadConfigBuilder =
                    mRewardedVideoAd.buildLoadAdConfig().withAdListener(rewardedVideoAdListener);
            rewardedVideoAdLoadConfigBuilder.withBid(TextUtils.isEmpty(payload) ? "" : payload);

            LogUtil.ownShow(TextUtils.isEmpty(payload) ? "normal load" : "bidding load");
            LogUtil.ownShow("bidding payload = " + (payload));

            if (!TextUtils.isEmpty(userId)) {
                // Create the rewarded ad data
                RewardData rewardData = new RewardData(userId, TextUtils.isEmpty(customData) ? "" : customData);
                Log.i(TAG, "RewardData: userId : " + userId + " , customData : " + customData);
                rewardedVideoAdLoadConfigBuilder.withRewardData(rewardData);
            }
            mRewardedVideoAd.loadAd(rewardedVideoAdLoadConfigBuilder.build());

        }
    }

    private final RewardedInterstitialAdListener rewardedInterstitialAdListener = new RewardedInterstitialAdListener() {
        @Override
        public void onError(Ad ad, AdError adError) {
            Log.i(TAG, "onError: errorCode : " + adError.getErrorCode() + " , errorMsg : " + adError.getErrorMessage());
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(FacebookErrorUtil.getTradPlusErrorCode(adError));
        }

        @Override
        public void onAdLoaded(Ad ad) {
            if (rewardedInterstitialAd == null) {
                return;
            }
            setFirstLoadedTime();
            Log.i(TAG, "onAdLoaded: ");
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(rewardedInterstitialAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdClicked(Ad ad) {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            Log.i(TAG, "onLoggingImpression: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
                mShowListener.onAdVideoStart();
            }
        }

        @Override
        public void onRewardedInterstitialCompleted() {
            Log.i(TAG, "onRewardedInterstitialCompleted: ");
            hasGrantedReward = true;

            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }
        }

        @Override
        public void onRewardedInterstitialClosed() {
            Log.i(TAG, "onRewardedInterstitialClosed: ");
            if (hasGrantedReward || alwaysRewardUser) {
                if (mShowListener != null) {
                    mShowListener.onReward();
                }
            }
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }
    };

    private final RewardedVideoAdListener rewardedVideoAdListener = new RewardedVideoAdListener() {
        @Override
        public void onRewardedVideoCompleted() {
            Log.i(TAG, "onRewardedVideoCompleted: ");
            hasGrantedReward = true;

            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            Log.i(TAG, "onError: errorCode : " + adError.getErrorCode() + " , errorMsg : " + adError.getErrorMessage());
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(FacebookErrorUtil.getTradPlusErrorCode(adError));
        }

        @Override
        public void onAdLoaded(Ad ad) {
            if (mRewardedVideoAd == null) {
                return;
            }
            setFirstLoadedTime();
            Log.i(TAG, "onAdLoaded: ");
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mRewardedVideoAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdClicked(Ad ad) {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            Log.i(TAG, "onLoggingImpression: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
                mShowListener.onAdVideoStart();
            }

        }

        @Override
        public void onRewardedVideoClosed() {
            Log.i(TAG, "onRewardedVideoClosed: ");
            if (hasGrantedReward || alwaysRewardUser) {
                if (mShowListener != null) {
                    mShowListener.onReward();
                }
            }
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }
    };

    @Override
    public void showAd() {
        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
            if (rewardedInterstitialAd != null && rewardedInterstitialAd.isAdLoaded()) {
                rewardedInterstitialAd.show();
            } else {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
                }
            }
        } else {
            if (mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded()) {
                mRewardedVideoAd.show();
            } else {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
                }
            }
        }

    }

    @Override
    public void clean() {
        super.clean();
        if (mRewardedVideoAd != null) {
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }

        if (rewardedInterstitialAd != null) {
            rewardedInterstitialAd.destroy();
            rewardedInterstitialAd = null;
        }

    }

    @Override
    public boolean isReady() {
        if (mRewardedVideoAd != null) {
            return !isAdsTimeOut() && !mRewardedVideoAd.isAdInvalidated();
        }
        if (rewardedInterstitialAd != null) {
            return !isAdsTimeOut() && !rewardedInterstitialAd.isAdInvalidated();
        }
        return false;
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_FACEBOOK);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getBiddingToken() {
        try {
            return BidderTokenProvider.getBidderToken(GlobalTradPlus.getInstance().getContext());
        } catch (Exception e) {

        }
        return null;
    }
}
