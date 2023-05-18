package com.tradplus.ads.unity.adapter;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.unity3d.ads.UnityAds.UnityAdsShowCompletionState.COMPLETED;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.PlayerMetaData;
import com.unity3d.services.core.webview.WebView;

import java.util.Map;

public class UnityInterstitialVideo extends TPRewardAdapter {

    private String placementId, userId;
    private boolean alwaysRewardUser;
    private UnityInterstitialCallbackRouter mCallbackRouter;
    private static final String TAG = "UnityRewardedVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (mLoadAdapterListener == null) {
            return;
        }

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


        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);

                if (TextUtils.isEmpty(userId)) {
                    userId = "";
                }
            }
        }

        mCallbackRouter = UnityInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        if (Build.VERSION.SDK_INT >= 19) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        if (!TextUtils.isEmpty(userId)) {
            Log.i(TAG, "RewardData: userId : " + userId);
            PlayerMetaData playerMetaData = new PlayerMetaData(context.getApplicationContext());
            playerMetaData.setServerId(userId);
            playerMetaData.commit();
        }


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
            Log.i(TAG, "onUnityAdsShowFailure: errorName :" + unityAdsShowError.name() + " s = " + s);
            if (mCallbackRouter.getShowListener(s) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(unityAdsShowError.name());
                mCallbackRouter.getShowListener(s).onAdVideoError(tpError);
            }
        }

        @Override
        public void onUnityAdsShowStart(String s) {
            Log.i(TAG, "onUnityAdsShowStart: s " + s);
            if (mCallbackRouter.getShowListener(s) != null) {
                mCallbackRouter.getShowListener(s).onAdVideoStart();
            }

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
            Log.i(TAG, "onUnityAdsShowonReward: ");
            if (mCallbackRouter.getShowListener(s) != null) {
                mCallbackRouter.getShowListener(s).onAdVideoEnd();

                if (unityAdsShowCompletionState == COMPLETED || alwaysRewardUser) {
                    mCallbackRouter.getShowListener(s).onReward();
                }

                mCallbackRouter.getShowListener(s).onAdClosed();
            }

        }
    };

    @Override
    public void clean() {
        super.clean();
        if (placementId != null)
            mCallbackRouter.removeListeners(placementId);
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
