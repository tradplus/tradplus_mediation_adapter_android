package com.tradplus.ads.chartboostx;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ads.Rewarded;
import com.chartboost.sdk.callbacks.RewardedCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.RewardEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
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
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChartboostInterstitialVideo extends TPRewardAdapter {

    private static final String TAG = "ChartboostRewardedVideo";
    private InterstitialCallbackRouter mCallBackRouter;
    private Rewarded chartboostRewarded;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private String location;

    @Override
    public void loadCustomAd(final Context context, final Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            location = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

//        appId = "4f7b433509b6025804000002";
//        appSignature = "dd2d41b69ac01b80f443f5b6cf06096d457f82bd";

        mCallBackRouter = InterstitialCallbackRouter.getInstance();
        mCallBackRouter.addListener(location, mLoadAdapterListener);

        TPInitMediation.InitCallback initCallback = new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial();
            }

            @Override
            public void onFailed(String code, String msg) {
                Log.i(TAG, "initSDK onFailed: msg :" + msg);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code + "");
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        };

        CBInitManager.getInstance().initSDK(context, userParams, tpParams, initCallback);
    }

    private void requestInterstitial() {
        if (chartboostRewarded != null) {
            chartboostRewarded.clearCache();
        }

        chartboostRewarded = new Rewarded(location, new RewardedCallback() {
            @Override
            public void onRewardEarned(@NonNull RewardEvent rewardEvent) {
                hasGrantedReward = true;
            }

            @Override
            public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
                Log.i(TAG, "onAdDismiss: ");
                if (mCallBackRouter.getShowListener(location) != null) {
                    mCallBackRouter.getShowListener(location).onAdVideoEnd();

                    if (hasGrantedReward || alwaysRewardUser) {
                        Log.i(TAG, "onRewardEarned: ");
                        mCallBackRouter.getShowListener(location).onReward();
                    }

                    mCallBackRouter.getShowListener(location).onAdClosed();
                }
            }

            @Override
            public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
                if (cacheError != null) {
                    Log.i(TAG, "LoadFailed: " + cacheError.toString());
                    if (mCallBackRouter.getListener(location) != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(cacheError.getCode() + "");
                        tpError.setErrorMessage(cacheError.toString());
                        mCallBackRouter.getListener(location).loadAdapterLoadFailed(tpError);
                    }
                }else {
                    Log.i(TAG, "onAdLoaded: ");
                    if (mCallBackRouter.getListener(location) != null) {
                        setNetworkObjectAd(chartboostRewarded);
                        mCallBackRouter.getListener(location).loadAdapterLoaded(null);
                    }
                }
            }

            @Override
            public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {

            }

            @Override
            public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
                if (showError != null) {
                    Log.i(TAG, "ShowError: " + showError.toString());
                    if (mCallBackRouter.getShowListener(location) != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(showError.getCode() + "");
                        tpError.setErrorMessage(showError.toString());
                        mCallBackRouter.getShowListener(location).onAdVideoError(tpError);
                    }
                }
            }

            @Override
            public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
                Log.i(TAG, "onAdClicked: ");
                if (mCallBackRouter.getShowListener(location) != null) {
                    mCallBackRouter.getShowListener(location).onAdClicked();
                }
            }

            @Override
            public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
                Log.i(TAG, "onImpressionRecorded: ");
                if (mCallBackRouter.getShowListener(location) != null) {
                    mCallBackRouter.getShowListener(location).onAdShown();
                    mCallBackRouter.getShowListener(location).onAdVideoStart();
                }
            }
        }, null);
        chartboostRewarded.cache();
    }

    @Override
    public void showAd() {
        if (mShowListener != null) {
            mCallBackRouter.addShowListener(location, mShowListener);
        }

        if (chartboostRewarded == null) {
            mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
        }

        if (chartboostRewarded.isCached()) {
            chartboostRewarded.show();
        } else {
            mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
        }

    }

    @Override
    public boolean isReady() {
        return chartboostRewarded != null && chartboostRewarded.isCached() && !isAdsTimeOut();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_CHARTBOOST);
    }

    @Override
    public String getNetworkVersion() {
        return Chartboost.getSDKVersion();
    }

}
