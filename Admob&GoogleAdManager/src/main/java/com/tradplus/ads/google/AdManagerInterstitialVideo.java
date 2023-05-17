package com.tradplus.ads.google;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AdManagerInterstitialVideo extends TPRewardAdapter {

    private String placementId, customData, userid;
    private AdManagerAdRequest request;
    private RewardedAd mRewardedAd;
    private RewardedInterstitialAd mRewardedInterstitialAd;
    private int isRewardedInterstitialAd;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private Integer mVideoMute = 0;
    private String id;
    private static final String TAG = "GAMRewardVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            isRewardedInterstitialAd = Integer.parseInt(tpParams.get(AppKeyManager.ADSOURCE_TYPE));
            id = tpParams.get(GoogleConstant.ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) userParams.get(AppKeyManager.CUSTOM_DATA);
                if (TextUtils.isEmpty(customData)) {
                    customData = "";
                }
            }
            if (userParams.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userid = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
                if (TextUtils.isEmpty(userid)) {
                    userid = "";
                }
            }

            if (userParams.containsKey(GoogleConstant.VIDEO_ADMOB_MUTE)) {
                mVideoMute = (int) userParams.get(GoogleConstant.VIDEO_ADMOB_MUTE);
            }
        }

        request = AdManagerInit.getInstance().getAdmobAdRequest(userParams,null,null);

        AdManagerInit.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                if (mVideoMute != 0) {
                    MobileAds.setAppMuted(mVideoMute == 1);
                }
                requestInterstitialVideo(context);
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

    private void requestInterstitialVideo(Context context) {

        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
            Log.i(TAG, "load RewardedInterstitialAd:");
            RewardedInterstitialAd.load(context, placementId, request, mRewardedInterstitialAdLoadCallback);
        } else {
            Log.i(TAG, "load RewardedAd:");
            RewardedAd.load(context, placementId, request, mRewardedAdLoadCallback);
        }
    }

    private final RewardedInterstitialAdLoadCallback mRewardedInterstitialAdLoadCallback = new RewardedInterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(RewardedInterstitialAd ad) {
            mRewardedInterstitialAd = ad;
            Log.i(TAG, "onAdLoaded: ");
            setFirstLoadedTime();
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(ad);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            mRewardedInterstitialAd.setFullScreenContentCallback(mFullScreenContentCallback);
        }

        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError) {
            Log.i(TAG, "onAdFailedToLoad: code: " + loadAdError.getCode() + " ,msg:" + loadAdError.getMessage());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(loadAdError.getMessage());
            tpError.setErrorCode(loadAdError.getCode() + "");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }
    };

    private final RewardedAdLoadCallback mRewardedAdLoadCallback = new RewardedAdLoadCallback() {
        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            // Handle the error.
            mRewardedAd = null;
            Log.i(TAG, "onAdFailedToLoad: code: " + loadAdError.getCode() + " ,msg:" + loadAdError.getMessage());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(loadAdError.getMessage());
            tpError.setErrorCode(loadAdError.getCode() + "");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

        }

        @Override
        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
            Log.i(TAG, "onAdLoaded: ");
            mRewardedAd = rewardedAd;
            setFirstLoadedTime();
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(rewardedAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            mRewardedAd.setFullScreenContentCallback(mFullScreenContentCallback);
        }
    };

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
            Log.i(TAG, "showAd RewardedInterstitialAd:");

            if (mRewardedInterstitialAd == null) {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(TPError.UNSPECIFIED));
                }
                return;
            }

            if (!TextUtils.isEmpty(userid)) {
                Log.i(TAG, "RewardData: userid:" + userid + ",customData : " + customData);
                ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
                        .setCustomData(TextUtils.isEmpty(customData) ? "" : customData)
                        .setUserId(userid)
                        .build();
                mRewardedInterstitialAd.setServerSideVerificationOptions(options);
            }

            mRewardedInterstitialAd.show(activity, mOnUserEarnedRewardListener);

        } else {
            Log.i(TAG, "showAd RewardedAd:");

            if (mRewardedAd == null) {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(TPError.UNSPECIFIED));
                }
                return;
            }

            if (!TextUtils.isEmpty(customData) && !TextUtils.isEmpty(userid)) {
                Log.i(TAG, "RewardData: customData : " + customData);
                ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
                        .setCustomData(customData)
                        .setUserId(userid)
                        .build();
                mRewardedAd.setServerSideVerificationOptions(options);
            }

            mRewardedAd.show(activity, mOnUserEarnedRewardListener);
        }

    }

    private final OnUserEarnedRewardListener mOnUserEarnedRewardListener = new OnUserEarnedRewardListener() {
        @Override
        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            Log.i(TAG, "onUserEarnedReward: ");
            hasGrantedReward = true;

        }
    };

    private final FullScreenContentCallback mFullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            Log.i(TAG, "onAdFailedToShowFullScreenContent: code:" + adError.getCode() + ", msg:" + adError.getMessage());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(adError.getMessage());
            tpError.setErrorCode(adError.getCode() + "");
            if (mShowListener != null) {
                mShowListener.onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdShowedFullScreenContent() {
            Log.i(TAG, "onAdShowedFullScreenContent");
            if (mShowListener != null) {
                mShowListener.onAdVideoStart();
            }
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            Log.i(TAG, "onAdDismissedFullScreenContent");
            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }

            if (hasGrantedReward || alwaysRewardUser) {
                if (mShowListener != null) {
                    mShowListener.onReward();
                }
            }

            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoClicked();
            }
        }
    };

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }


    @Override
    public void clean() {
        if (mRewardedInterstitialAd != null) {
            mRewardedInterstitialAd.setFullScreenContentCallback(null);
            mRewardedInterstitialAd = null;
        }

        if (mRewardedAd != null) {
            mRewardedAd.setFullScreenContentCallback(null);
            mRewardedAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(id);
    }


    @Override
    public String getNetworkVersion() {
        VersionInfo version = MobileAds.getVersion();
        int majorVersion = version.getMajorVersion();
        int minorVersion = version.getMinorVersion();
        int microVersion = version.getMicroVersion();
        return majorVersion + "." + minorVersion + "." + microVersion + "";
    }


}
