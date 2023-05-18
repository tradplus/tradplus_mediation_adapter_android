package com.tradplus.ads.adfly;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.adfly.sdk.core.AdError;
import com.adfly.sdk.core.AdFlySdk;
import com.adfly.sdk.core.AdflyAd;
import com.adfly.sdk.rewardedvideo.RewardedVideoAd;
import com.adfly.sdk.rewardedvideo.RewardedVideoListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;


public class AdFlyInterstitialVideo extends TPRewardAdapter {

    private String mPlacementId;
    private InterstitialCallbackRouter mCallbackRouter;
    private RewardedVideoAd mRewardedVideoAd;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "AdFlyRewardedVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        AdFlyInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd();
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(INIT_FAILED));
            }
        });
    }

    private void requestAd() {
        mRewardedVideoAd = RewardedVideoAd.getInstance(mPlacementId);
        mRewardedVideoAd.setRewardedVideoListener(new RewardedVideoListener() {
            @Override
            public void onRewardedAdLoadSuccess(AdflyAd ad) {
                Log.i(TAG, "onRewardedAdLoadSuccess: ");
                if (mCallbackRouter.getListener(ad.getUnitId()) != null) {
                    setNetworkObjectAd(mRewardedVideoAd);
                    mCallbackRouter.getListener(ad.getUnitId()).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onRewardedAdLoadFailure(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onRewardedAdLoadFailure: ");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (adError != null) {
                    String errorMessage = adError.getErrorMessage();
                    int errorCode = adError.getErrorCode();
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(errorMessage);
                    Log.i(TAG, "errorCode :" + errorCode + " , errorMsg :" + errorMessage);
                }

                if (mCallbackRouter.getListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getListener(ad.getUnitId()).loadAdapterLoadFailed(tpError);
                }

            }

            @Override
            public void onRewardedAdShowed(AdflyAd ad) {
                Log.i(TAG, "onRewardedAdShowed: ");
                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdShown();
                }

                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoStart();
                }
            }

            @Override
            public void onRewardedAdShowError(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onRewardedAdShowError: ");
                TPError tpError = new TPError(SHOW_FAILED);
                if (adError != null) {
                    String errorMessage = adError.getErrorMessage();
                    int errorCode = adError.getErrorCode();
                    Log.i(TAG, "errorCode :" + errorCode + " , errorMsg :" + errorMessage);
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(errorMessage);
                }

                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoError(tpError);
                }
            }

            @Override
            public void onRewardedAdCompleted(AdflyAd ad) {
                Log.i(TAG, "onRewardedAdCompleted: ");
                hasGrantedReward = true;
                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoEnd();
                }

            }

            @Override
            public void onRewardedAdClosed(AdflyAd ad) {
                Log.i(TAG, "onRewardedAdClosed: ");
                if (mCallbackRouter.getShowListener(ad.getUnitId()) == null) {
                    return;
                }

                Log.i(TAG, "hasGrantedReward: " + hasGrantedReward);
                if (hasGrantedReward || alwaysRewardUser) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onReward();
                }

                mCallbackRouter.getShowListener(ad.getUnitId()).onAdClosed();

            }

            @Override
            public void onRewardedAdClick(AdflyAd ad) {
                Log.i(TAG, "onRewardedAdClick: ");
                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoClicked();
                }

            }
        });
        mRewardedVideoAd.loadAd();
    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        if (mRewardedVideoAd == null) {
            TPError tpError = new TPError(UNSPECIFIED);
            tpError.setErrorMessage("showfailed，mRewardedVideoAd == null");
            mShowListener.onAdVideoError(tpError);
            return;
        }

        mRewardedVideoAd.show();

    }

    @Override
    public boolean isReady() {
        return mRewardedVideoAd != null && mRewardedVideoAd.isReady() && !mRewardedVideoAd.isAdInvalidated() && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        if (mRewardedVideoAd != null) {
            mRewardedVideoAd.setRewardedVideoListener(null);
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }

        if (mCallbackRouter != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return "AdFlySdk";
    }

    @Override
    public String getNetworkVersion() {
        return AdFlySdk.getVersion();
    }
}
