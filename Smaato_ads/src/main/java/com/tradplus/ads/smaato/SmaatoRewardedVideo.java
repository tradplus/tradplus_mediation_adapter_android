package com.tradplus.ads.smaato;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.smaato.sdk.core.SmaatoSdk;
import com.smaato.sdk.rewarded.EventListener;
import com.smaato.sdk.rewarded.RewardedError;
import com.smaato.sdk.rewarded.RewardedInterstitial;
import com.smaato.sdk.rewarded.RewardedInterstitialAd;
import com.smaato.sdk.rewarded.RewardedRequestError;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class SmaatoRewardedVideo extends TPRewardAdapter {

    private SmaatoInterstitialCallbackRouter mSmattoICBr;
    private RewardedInterstitialAd loaded;
    private String mPlacementId;
    private boolean hasGrantedReward = false;
    private static final String TAG = "SmaatoRewardedVideo";
    private boolean alwaysReward = false;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

//        mAppId = "1100047326";
//        mPlacementId = "130626428";
        if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
            alwaysReward = (Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD)) == AppKeyManager.ENFORCE_REWARD);
        }

        mSmattoICBr = SmaatoInterstitialCallbackRouter.getInstance();
        mSmattoICBr.addListener(mPlacementId, mLoadAdapterListener);

        SmaatoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitialVideo();
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

    private void requestInterstitialVideo() {
        // load rewarded ad
        RewardedInterstitial.loadAd(mPlacementId, new EventListener() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                Log.i(TAG, "onAdLoaded: ");
                if (mSmattoICBr.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(rewardedInterstitialAd);
                    mSmattoICBr.getListener(mPlacementId).loadAdapterLoaded(null);
                    loaded = rewardedInterstitialAd;
                }
            }

            @Override
            // rewarded ad failed to load
            public void onAdFailedToLoad(@NonNull RewardedRequestError rewardedRequestError) {
                Log.i(TAG, "onAdFailedToLoad: ");
                if (mSmattoICBr.getListener(mPlacementId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    if (rewardedRequestError != null) {
                        RewardedError rewardedError = rewardedRequestError.getRewardedError();
                        if (rewardedError != null && rewardedError.name() != null) {
                            tpError.setErrorMessage(rewardedError.name());
                        }
                    }

                    mSmattoICBr.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdError(@NonNull RewardedInterstitialAd rewardedInterstitialAd, @NonNull RewardedError rewardedError) {
                Log.i(TAG, "onAdError: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    TPError tpError = new TPError(SHOW_FAILED);
                    if (rewardedError != null) {
                        String name = rewardedError.name();
                        if (name != null) {
                            tpError.setErrorMessage(name);
                        }
                    }
                    mSmattoICBr.getShowListener(mPlacementId).onAdVideoError(tpError);
                }
            }

            @Override
            public void onAdClosed(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                if (mSmattoICBr.getShowListener(mPlacementId) == null) {
                    return;
                }

                Log.i(TAG, "onAdClosed: ");
                if (hasGrantedReward || alwaysReward) {
                    mSmattoICBr.getShowListener(mPlacementId).onReward();
                }

                mSmattoICBr.getShowListener(mPlacementId).onAdClosed();
            }

            @Override
            public void onAdClicked(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                Log.i(TAG, "onAdClicked: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    mSmattoICBr.getShowListener(mPlacementId).onAdVideoClicked();
                }
            }

            @Override
            public void onAdStarted(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                Log.i(TAG, "onAdStarted: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    mSmattoICBr.getShowListener(mPlacementId).onAdVideoStart();
                    mSmattoICBr.getShowListener(mPlacementId).onAdShown();
                }
            }

            @Override
            // rewarded ad finished playing and was watched all the way through
            public void onAdReward(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                Log.i(TAG, "onAdReward: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    mSmattoICBr.getShowListener(mPlacementId).onAdVideoEnd();
                }
                hasGrantedReward = true;
            }

            @Override
            // rewarded ad Time to Live expired
            public void onAdTTLExpired(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                Log.i(TAG, "onAdTTLExpired: ");

            }
        });
    }


    @Override
    public void showAd() {
        if (mSmattoICBr != null && mShowListener != null) {
            mSmattoICBr.addShowListener(mPlacementId, mShowListener);
        }

        Log.i(TAG, "showRewardVideo: ");
        if (loaded != null && loaded.isAvailableForPresentation()) {
            loaded.showAd();
        } else {
            if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                mSmattoICBr.getShowListener(mPlacementId).onAdVideoError(new TPError(NETWORK_NO_FILL));
            }
        }


    }

    @Override
    public boolean isReady() {
        if (loaded != null) {
            boolean availableForPresentation = loaded.isAvailableForPresentation();
            Log.i(TAG, "isReadyRewardVideo: " + availableForPresentation);
            return availableForPresentation && !isAdsTimeOut();
        } else {
            return false;
        }
    }

    @Override
    public void clean() {
        if (mPlacementId != null) {
            mSmattoICBr.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_SMAATO);
    }

    @Override
    public String getNetworkVersion() {
        return SmaatoSdk.getVersion();
    }

}
