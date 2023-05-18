package com.tradplus.ads.ironsource;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
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
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class IronSourceInterstitialVideo extends TPRewardAdapter {

    private String placementId, userId;
    private boolean alwaysReward = false;
    public static final String TAG = "IronSourceRewardVideo";
    private IronSourceInterstitialCallbackRouter ironSourceICbR;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        String appKey;
        if (extrasAreValid(tpParams)) {
            appKey = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

        } else {
            TPError tpError = new TPError(ADAPTER_CONFIGURATION_ERROR);
            tpError.setErrorMessage("Ironsource app_key or instance_id is empty");
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);

            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
        }

        if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
            alwaysReward =  (Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD)) == AppKeyManager.ENFORCE_REWARD);
        }

        if (!TextUtils.isEmpty(userId)) {
            Log.i(TAG, "RewardData: userId : " + userId);
            IronSource.setUserId(userId);
        }

        IronSourceAdsListener ironSourceAdsListener = new IronSourceAdsListener(placementId, appKey, alwaysReward);
        IronSource.setISDemandOnlyRewardedVideoListener(ironSourceAdsListener);
        ironSourceICbR = IronSourceInterstitialCallbackRouter.getInstance();
        ironSourceICbR.addListener(placementId, mLoadAdapterListener);

        IronSourceInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestInterstitalVideo();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestInterstitalVideo() {
        Activity mActivity = GlobalTradPlus.getInstance().getActivity();
        if (mActivity == null) {
            TPError tpError = new TPError(ADAPTER_ACTIVITY_ERROR);
            tpError.setErrorMessage("Ironsource context must be activity.");
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            return;
        }

        if (IronSource.isISDemandOnlyRewardedVideoAvailable(placementId)) {
            ironSourceICbR.getListener(placementId).loadAdapterLoaded(null);
        } else {
            IronSource.loadISDemandOnlyRewardedVideo(mActivity, placementId);
        }
    }


    @Override
    public void showAd() {
        if (mShowListener != null) {
            ironSourceICbR.addShowListener(placementId, mShowListener);
        }

        if (IronSource.isISDemandOnlyRewardedVideoAvailable(placementId)) {
            if (placementId != null && placementId.length() > 0) {
                IronSource.showISDemandOnlyRewardedVideo(placementId);
            }
        } else {
            if (ironSourceICbR.getShowListener(placementId) != null)
                ironSourceICbR.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
        }

    }

    @Override
    public boolean isReady() {
        return IronSource.isISDemandOnlyRewardedVideoAvailable(placementId) && !isAdsTimeOut();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_IRONSOURCE);
    }

    @Override
    public String getNetworkVersion() {
        return IronSourceUtils.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }
}
