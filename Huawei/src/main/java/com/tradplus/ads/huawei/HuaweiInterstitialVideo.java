package com.tradplus.ads.huawei;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdLoadListener;
import com.huawei.hms.ads.reward.RewardAdStatusListener;
import com.huawei.hms.ads.reward.RewardVerifyConfig;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;


public class HuaweiInterstitialVideo extends TPRewardAdapter {

    private String mPlacementId, userId, customData;
    private RewardAd rewardAd;
    private HuaweiInterstitialCallbackRouter mCallbackRouter;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "HuaweiRewarded";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
            customData = (String) userParams.get(AppKeyManager.CUSTOM_DATA);

            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
            if (TextUtils.isEmpty(customData)) {
                customData = "";
            }
        }

        mCallbackRouter = HuaweiInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        HuaweiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestInterstitalVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestInterstitalVideo(Context context) {

        if (rewardAd == null)
            rewardAd = new RewardAd(context, mPlacementId);

        RewardAdLoadListener listener = new RewardAdLoadListener() {
            @Override
            public void onRewardedLoaded() {
                Log.i(TAG, "onRewardedLoaded: ");
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(rewardAd);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }

            }

            @Override
            public void onRewardAdFailedToLoad(int errorCode) {
                Log.i(TAG, "onRewardAdFailedToLoad: errorCode :" + errorCode);
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(errorCode + "");
                if (mCallbackRouter.getListener(mPlacementId) != null)
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
            }
        };
        if (!TextUtils.isEmpty(userId)) {
            Log.i(TAG, "RewardData: userId : " + userId + " , customData : " + customData);
            RewardVerifyConfig config = new RewardVerifyConfig.Builder()
                    .setData(TextUtils.isEmpty(customData) ? "" : customData)
                    .setUserId(userId)
                    .build();
            rewardAd.setRewardVerifyConfig(config);
        }
        rewardAd.loadAd(new AdParam.Builder().build(), listener);
    }


    @Override
    public void showAd() {
        if (mShowListener != null)
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);


        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (rewardAd.isLoaded()) {
            rewardAd.show(activity, new RewardAdStatusListener() {
                @Override
                public void onRewardAdOpened() {
                    Log.i(TAG, "onRewardAdOpened: ");
                    if (mCallbackRouter.getShowListener(mPlacementId) != null)
                        mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();

                    if (mCallbackRouter.getShowListener(mPlacementId) != null)
                        mCallbackRouter.getShowListener(mPlacementId).onAdShown();
                }

                @Override
                public void onRewardAdFailedToShow(int errorCode) {
                    Log.i(TAG, "onRewardAdFailedToShow: errorCode : " + errorCode);
                    TPError tpError = new TPError(SHOW_FAILED);
                    tpError.setErrorCode(errorCode + "");
                    if (mCallbackRouter.getShowListener(mPlacementId) != null)
                        mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpError);

                }

                @Override
                public void onRewardAdClosed() {
                    Log.i(TAG, "onRewardAdClosed: ");
                    if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                        if (hasGrantedReward || alwaysRewardUser) {
                            mCallbackRouter.getShowListener(mPlacementId).onReward();
                        }

                        mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
                    }
                }

                @Override
                public void onRewarded(Reward reward) {
                    Log.i(TAG, "onRewarded: ");
                    hasGrantedReward = true;

                    if (mCallbackRouter.getShowListener(mPlacementId) != null)
                        mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();

                }
            });
        } else {
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
        }

    }

    @Override
    public boolean isReady() {
        if (rewardAd != null) {
            return rewardAd.isLoaded() && !isAdsTimeOut();
        }
        return false;
    }

    @Override
    public void clean() {
        super.clean();

        if (rewardAd != null) {
            rewardAd.setRewardAdListener(null);
            rewardAd.destroy();
            rewardAd = null;
        }

        if (mCallbackRouter.getShowListener(mPlacementId) != null)
            mCallbackRouter.removeListeners(mPlacementId);

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_HUAWEI);
    }

    @Override
    public String getNetworkVersion() {
        return HwAds.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

}
