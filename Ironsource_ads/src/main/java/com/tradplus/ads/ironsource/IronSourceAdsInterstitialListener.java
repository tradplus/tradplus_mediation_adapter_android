package com.tradplus.ads.ironsource;

import android.util.Log;

import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;
import com.tradplus.ads.base.util.AppKeyManager;

public class IronSourceAdsInterstitialListener implements ISDemandOnlyInterstitialListener {
    private String placementId, mAppKey;
    private IronSourceInterstitialCallbackRouter ironSourceICbR;
    public static final String TAG = "IronSourceInterstitial";

    public IronSourceAdsInterstitialListener(String id, String appKey) {
        placementId = id;
        mAppKey = appKey;
        ironSourceICbR = IronSourceInterstitialCallbackRouter.getInstance();
    }

    @Override
    public void onInterstitialAdReady(String s) {
        Log.i(TAG, "onInterstitialAdReady: ");
        if (ironSourceICbR.getListener(s) != null)
            ironSourceICbR.getListener(s).loadAdapterLoaded(null);
    }

    @Override
    public void onInterstitialAdLoadFailed(String s, IronSourceError ironSourceError) {
        Log.i(TAG, "IronSource ad load failed, ErrorCode : " + ironSourceError.getErrorCode() + ", ErrorMessage : " + ironSourceError.getErrorMessage());
        if (ironSourceError.getErrorCode() == 508) {
            AppKeyManager.getInstance().removeAppKey(mAppKey);
        } else {
            if (ironSourceICbR.getListener(s) != null)
                ironSourceICbR.getListener(s).loadAdapterLoadFailed(IronSourceErrorUtil.getTradPlusErrorCode(ironSourceError));
        }
    }

    @Override
    public void onInterstitialAdOpened(String s) {
        Log.i(TAG, "onInterstitialAdOpened: ");
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdShown();
    }

    @Override
    public void onInterstitialAdClosed(String s) {
        Log.i(TAG, "onInterstitialAdClosed: ");
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdClosed();
    }

    @Override
    public void onInterstitialAdShowFailed(String s, IronSourceError ironSourceError) {
        Log.i(TAG, "IronSource ad show failed " + " , ErrorCode : " + ironSourceError.getErrorCode() + ", ErrorMessage : " + ironSourceError.getErrorMessage());
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdVideoError(IronSourceErrorUtil.getTradPlusErrorCode(ironSourceError));

    }

    @Override
    public void onInterstitialAdClicked(String s) {
        Log.i(TAG, "onInterstitialAdClicked: ");
        if (ironSourceICbR.getShowListener(s) != null)
            ironSourceICbR.getShowListener(s).onAdVideoClicked();
    }
}
