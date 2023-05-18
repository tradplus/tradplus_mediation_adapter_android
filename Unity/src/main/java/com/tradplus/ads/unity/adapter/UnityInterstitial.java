package com.tradplus.ads.unity.adapter;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

public class UnityInterstitial extends TPInterstitialAdapter {

    private static final String TAG = "UnityInterstitial";
    private String appId, placementId;
    private UnityInterstitialCallbackRouter mCallbackRouter;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = UnityInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        UnityAdsInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                setFirstLoadedTime();
                UnityAds.load(placementId, mIUnityAdsLoadListener);
            }

            @Override
            public void onFailed(String code, String msg) {
                Log.i(TAG, "onFailed: msg :" + msg);
                TPError tpError = new TPError(INIT_FAILED);
                tpError.setErrorMessage(msg);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        });


    }

    @Override
    public void showAd() {
        if (mShowListener != null && placementId != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        UnityAds.show(activity, placementId, mIUnityAdsShowListener);


    }

    @Override
    public boolean isReady() {
        if (placementId != null) {
            return !isAdsTimeOut();
        } else {
            return false;
        }
    }

    IUnityAdsLoadListener mIUnityAdsLoadListener = new IUnityAdsLoadListener() {

        @Override
        public void onUnityAdsAdLoaded(String s) {
            Log.i(TAG, "onUnityAdsAdLoaded: ");
            if (mCallbackRouter.getListener(s) != null) {
                mCallbackRouter.getListener(s).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onUnityAdsFailedToLoad(String s, UnityAds.UnityAdsLoadError unityAdsLoadError, String s1) {
            Log.i(TAG, "onUnityAdsFailedToLoad: errorName :" + unityAdsLoadError.name());
            if (mCallbackRouter.getListener(s) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(unityAdsLoadError.name());
                mCallbackRouter.getListener(s).loadAdapterLoadFailed(tpError);
            }
        }
    };

    IUnityAdsShowListener mIUnityAdsShowListener = new IUnityAdsShowListener() {

        @Override
        public void onUnityAdsShowFailure(String s, UnityAds.UnityAdsShowError unityAdsShowError, String s1) {
            Log.i(TAG, "onUnityAdsShowFailure: errorName :" + unityAdsShowError.name());
            if (mCallbackRouter.getShowListener(s) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(unityAdsShowError.name());
                mCallbackRouter.getShowListener(s).onAdVideoError(tpError);
            }
        }

        @Override
        public void onUnityAdsShowStart(String s) {
            Log.i(TAG, "onUnityAdsShowStart: ");
            if (mCallbackRouter.getShowListener(s) != null) {
                mCallbackRouter.getShowListener(s).onAdShown();
            }
        }

        @Override
        public void onUnityAdsShowClick(String s) {
            Log.i(TAG, "onUnityAdsShowClick: ");
            if (mCallbackRouter.getShowListener(s) != null) {
                mCallbackRouter.getShowListener(s).onAdVideoClicked();
            }

        }

        @Override
        public void onUnityAdsShowComplete(String s, UnityAds.UnityAdsShowCompletionState unityAdsShowCompletionState) {
            Log.i(TAG, "onUnityAdsShowComplete: ");
            if (mCallbackRouter.getShowListener(s) != null) {
                mCallbackRouter.getShowListener(s).onAdClosed();
            }
        }
    };

    @Override
    public void clean() {
        super.clean();
        if (placementId != null && mCallbackRouter != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_UNITY);
    }

    @Override
    public String getNetworkVersion() {
        return UnityAds.getVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }
}
