package com.tradplus.ads.mimo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.miui.zeus.mimo.sdk.BuildConfig;
import com.miui.zeus.mimo.sdk.InterstitialAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MimoInterstitial extends TPInterstitialAdapter {

    private String placementId;
    private InterstitialAd mInterstitialAd;
    private MimoInterstitialCallbackRouter mCallbackRouter;
    private static final String TAG = "MimoInterstitial";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        mCallbackRouter = MimoInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        MimoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial();
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

    private void requestInterstitial() {
        mInterstitialAd = new InterstitialAd();
        mInterstitialAd.loadAd(placementId, new InterstitialAd.InterstitialAdLoadListener() {
            @Override
            public void onAdLoadSuccess() {
                Log.i(TAG, "onAdLoadSuccess: ");
                if (mCallbackRouter.getListener(placementId) != null) {
                    setNetworkObjectAd(mInterstitialAd);
                    mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdRequestSuccess() {
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
    public boolean isReady() {
        return !isAdsTimeOut();
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

        if (mInterstitialAd == null) {
            mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }

        mInterstitialAd.show(activity, mInterstitialAdInteractionListener);
    }

    private final InterstitialAd.InterstitialAdInteractionListener mInterstitialAdInteractionListener = new InterstitialAd.InterstitialAdInteractionListener() {
        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdShow() {
            Log.i(TAG, "onAdShow: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdShown();
            }
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdClosed();
            }
        }

        @Override
        public void onRenderFail(int errorCode, String message) {
            Log.i(TAG, "onRenderFail: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(errorCode + "");
                tpError.setErrorMessage(message);
                mCallbackRouter.getShowListener(placementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoStart();
            }
        }

        @Override
        public void onVideoPause() {
            Log.i(TAG, "onVideoPause: ");
        }

        @Override
        public void onVideoResume() {
            Log.i(TAG, "onVideoResume: ");
        }

        @Override
        public void onVideoEnd() {
            Log.i(TAG, "onVideoEnd: ");
            if (mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoEnd();
            }
        }
    };

    @Override
    public void clean() {
        super.clean();
        if (mInterstitialAd != null) {
            mInterstitialAd.destroy();
            mInterstitialAd = null;
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
