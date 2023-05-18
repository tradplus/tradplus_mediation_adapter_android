package com.tradplus.ads.mimo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.miui.zeus.mimo.sdk.BuildConfig;
import com.miui.zeus.mimo.sdk.RewardVideoAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MimoInterstitialVideo extends TPRewardAdapter {

    private String placementId;
    private RewardVideoAd mRewardVideoAd;
    private MimoInterstitialCallbackRouter mCallbackRouter;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "MimoInterstitialVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }



        mCallbackRouter = MimoInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        MimoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitialVideo();
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

    private void requestInterstitialVideo() {
        mRewardVideoAd = new RewardVideoAd();
        mRewardVideoAd.loadAd(placementId, new RewardVideoAd.RewardVideoLoadListener() {
            @Override
            public void onAdRequestSuccess() {
                Log.i(TAG, "onAdRequestSuccess");

            }

            @Override
            public void onAdLoadSuccess() {
                Log.i(TAG, "onAdLoadSuccess");
                if (mCallbackRouter.getListener(placementId) != null) {
                    setNetworkObjectAd(mRewardVideoAd);
                    mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdLoadFailed(int errorCode, String errorMsg) {
                Log.i(TAG, "onAdLoadFailed errorCode=" + errorCode + ",errorMsg=" + errorMsg);
                if (mCallbackRouter.getListener(placementId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(errorMsg);
                    mCallbackRouter.getListener(placementId).loadAdapterLoadFailed(tpError);
                }
            }
        });
    }

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mShowListener != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        if (mRewardVideoAd == null) {
            mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }

        mRewardVideoAd.showAd(activity, mRewardVideoInteractionListener);
    }


    private final RewardVideoAd.RewardVideoInteractionListener mRewardVideoInteractionListener = new RewardVideoAd.RewardVideoInteractionListener() {
        @Override
        public void onAdPresent() {
            Log.i(TAG, "onAdPresent");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdShown();
            }

        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdDismissed() {
            Log.i(TAG, "onAdDismissed");
            if (mCallbackRouter.getShowListener(placementId) == null) {
                return;
            }

            if (hasGrantedReward || alwaysRewardUser) {
                mCallbackRouter.getShowListener(placementId).onReward();
            }

            mCallbackRouter.getShowListener(placementId).onAdClosed();
        }

        @Override
        public void onAdFailed(String message) {
            Log.i(TAG, "onAdFailed");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage(message);
                mCallbackRouter.getShowListener(placementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoStart();
            }
        }

        @Override
        public void onVideoPause() {
            Log.i(TAG, "onVideoPause");
        }

        @Override
        public void onVideoSkip() {
            Log.i(TAG, "onVideoSkip: ");
            alwaysRewardUser = false;
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onRewardSkip();
            }
        }

        @Override
        public void onVideoComplete() {
            Log.i(TAG, "onVideoComplete");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoEnd();
            }
        }

        @Override
        public void onPicAdEnd() {
            Log.i(TAG, "onPicAdEnd");
        }

        @Override
        public void onReward() {
            Log.i(TAG, "onReward");
            hasGrantedReward = true;
        }
    };

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    @Override
    public void clean() {
        super.clean();
        if (mRewardVideoAd != null) {
            mRewardVideoAd.recycle();
            mRewardVideoAd = null;
        }

        if (placementId != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MIMO);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
