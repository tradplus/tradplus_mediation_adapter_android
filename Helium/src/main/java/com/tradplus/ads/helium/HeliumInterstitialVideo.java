package com.tradplus.ads.helium;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.heliumsdk.HeliumSdk;
import com.chartboost.heliumsdk.ad.HeliumFullscreenAdListener;
import com.chartboost.heliumsdk.ad.HeliumRewardedAd;
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

public class HeliumInterstitialVideo extends TPRewardAdapter {


    private String mPlacementId;
    private HeliumRewardedAd mHeliumReward;
    private HeliumInterstitialCallbackRouter mCallbackRouter;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "HeliumRewardVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null && onC2STokenListener == null) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
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



        mCallbackRouter = HeliumInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);
        mLoadAdapterListener = mCallbackRouter.getListener(mPlacementId);

        HeliumInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {

            @Override
            public void onSuccess() {
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

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });

    }

    private void requestInterstitialVideo(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mHeliumReward != null && mLoadAdapterListener != null) {
                setNetworkObjectAd(mHeliumReward);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            return;
        }

        // clear loaded ads on existing placements
        if (mHeliumReward != null) {
            mHeliumReward.clearLoaded();
        }

        mHeliumReward = new HeliumRewardedAd(context, mPlacementId, new HeliumFullscreenAdListener() {
            @Override
            public void onAdCached(@NonNull String placementName,
                                   @NonNull String loadId,
                                   @NonNull Map<String, String> winningBidInfo,
                                   @Nullable ChartboostMediationAdException error) {
                if (error != null) {
                    String message = error.getMessage();
                    if (isC2SBidding) {
                        if (onC2STokenListener != null) {
                            onC2STokenListener.onC2SBiddingFailed("", message);
                        }
                        return;
                    }

                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(message);
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                Log.i(TAG, "onAdCached: " + placementName + "::" + winningBidInfo.toString());
                String price = winningBidInfo.get("price");
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        Log.i(TAG, "RewardVideo bid price: " + price);
                        if (TextUtils.isEmpty(price)) {
                            onC2STokenListener.onC2SBiddingFailed("", "price is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(Double.parseDouble(price));
                    }
                    isBiddingLoaded = true;
                }
            }

            @Override
            public void onAdShown(@NonNull String placementName, @Nullable ChartboostMediationAdException error) {
                Log.i(TAG, "onAdShown: ");
                if (mShowListener == null) return;

                if (error != null) {
                    String message = error.getMessage();
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(message);
                    mShowListener.onAdVideoError(tpError);
                    Log.i(TAG, "onAdShown error: " + message);
                    return;
                }

                mShowListener.onAdShown();
                mShowListener.onAdVideoStart();
            }

            @Override
            public void onAdClicked(@NonNull String placementName) {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onAdClosed(@NonNull String placementName, @Nullable ChartboostMediationAdException error) {
                Log.i(TAG, "onAdClosed: ");
                if (mShowListener == null) return;

                if (hasGrantedReward || alwaysRewardUser) {
                    Log.i(TAG, "didAdReward: ");
                    mShowListener.onReward();
                }

                if (error != null) {
                    String message = error.getMessage();
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(message);
                    mShowListener.onAdVideoError(tpError);
                    Log.i(TAG, "onAdClosed error: " + message);
                    return;
                }

                mShowListener.onAdVideoEnd();
                mShowListener.onAdClosed();
            }

            @Override
            public void onAdRewarded(@NonNull String placementName) {
                Log.i(TAG, "onAdRewarded: ");
                hasGrantedReward = true;
            }

            @Override
            public void onAdImpressionRecorded(@NonNull String placementName) {

            }
        });
        mHeliumReward.load();
    }


    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
            mShowListener = mCallbackRouter.getShowListener(mPlacementId);
        }

        if (mHeliumReward == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
            }
            return;
        }

        if (mHeliumReward.readyToShow()) {
            mHeliumReward.show();
        } else {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(NETWORK_NO_FILL));
            }
        }


    }

    @Override
    public boolean isReady() {
        return mHeliumReward != null && mHeliumReward.readyToShow() && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        if (mHeliumReward != null) {
            mHeliumReward.destroy();
            mHeliumReward = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_HELIUM);
    }

    @Override
    public String getNetworkVersion() {
        return HeliumSdk.getVersion();
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }
}
