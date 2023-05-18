package com.tradplus.ads.ironsource;

import android.util.Log;

import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.tradplus.ads.base.util.AppKeyManager;

public class IronSourceAdsListener implements ISDemandOnlyRewardedVideoListener {
    private String placementId, mAppKey;
    private IronSourceInterstitialCallbackRouter ironSourceICbR;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardFromServer = false;
    public static final String TAG = "IronSourceRewardVideo";

    public IronSourceAdsListener(String id, String appKey, boolean alwaysReward) {
        placementId = id;
        mAppKey = appKey;
        alwaysRewardFromServer = alwaysReward;
        ironSourceICbR = IronSourceInterstitialCallbackRouter.getInstance();
    }

    @Override
    public void onRewardedVideoAdLoadSuccess(String s) {
        Log.i(TAG, "onRewardedVideoAdLoadSuccess: ");
        if (ironSourceICbR.getListener(s) != null)
            ironSourceICbR.getListener(s).loadAdapterLoaded(null);
    }

    @Override
    public void onRewardedVideoAdLoadFailed(String s, IronSourceError ironSourceError) {
        Log.i(TAG, "IronSource ad load failed , ErrorCode : " + ironSourceError.getErrorCode() + ", ErrorMessage : " + ironSourceError.getErrorMessage());
        if (ironSourceError.getErrorCode() == 508) {
            AppKeyManager.getInstance().removeAppKey(mAppKey);
        } else {
            if (ironSourceICbR.getListener(s) != null)
                ironSourceICbR.getListener(s).loadAdapterLoadFailed(IronSourceErrorUtil.getTradPlusErrorCode(ironSourceError));
        }
    }

    @Override
    public void onRewardedVideoAdOpened(String s) {
        Log.i(TAG, "onRewardedVideoAdOpened: ");
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdVideoStart();

        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdShown();
    }

    @Override
    public void onRewardedVideoAdClosed(String s) {
        Log.i(TAG, "onRewardedVideoAdClosed: ");
        if (ironSourceICbR.getShowListener(s) == null)
            return;
        if (hasGrantedReward || alwaysRewardFromServer) {
            ironSourceICbR.getShowListener(s).onReward();
        }

        ironSourceICbR.getShowListener(s).onAdClosed();
    }

    @Override
    public void onRewardedVideoAdShowFailed(String s, IronSourceError ironSourceError) {
        Log.i(TAG, "onRewardedVideoAdShowFailed: ErrorCode : " + ironSourceError.getErrorCode() + ", ErrorMessage : " + ironSourceError.getErrorMessage());
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdVideoError(IronSourceErrorUtil.getTradPlusShowFailedErrorCode(ironSourceError));

    }

    @Override
    public void onRewardedVideoAdClicked(String s) {
        Log.i(TAG, "onRewardedVideoAdClicked: ");
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdVideoClicked();
    }

    @Override
    public void onRewardedVideoAdRewarded(String s) {
        Log.i(TAG, "onRewardedVideoAdRewarded: ");
        hasGrantedReward = true;
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdVideoEnd();
    }
}
