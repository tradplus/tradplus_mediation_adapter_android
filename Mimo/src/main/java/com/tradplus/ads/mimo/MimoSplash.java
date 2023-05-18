package com.tradplus.ads.mimo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.util.Log;

import com.miui.zeus.mimo.sdk.BuildConfig;
import com.miui.zeus.mimo.sdk.SplashAd;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MimoSplash extends TPSplashAdapter {

    private SplashAd mSplashAd;
    private String placementId;
    private static final String TAG = "MimoSplash";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


//        placementId = "94f4805a2d50ba6e853340f9035fda18"; //横屏
//        placementId = "b373ee903da0c6fc9c9da202df95a500"; //竖屏
        MimoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestSplash();
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

    private void requestSplash() {
        mSplashAd = new SplashAd();

        if (mAdContainerView == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
            }
            return;
        }

        mSplashAd.loadAndShow(mAdContainerView, placementId, mSplashAdListener);

    }

    private boolean isReady = true;
    private final SplashAd.SplashAdListener mSplashAdListener = new SplashAd.SplashAdListener() {
        @Override
        public void onAdShow() {
            Log.i(TAG, "onAdShow");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onAdDismissed() {
            Log.i(TAG, "onAdDismissed");
            if (mShowListener != null && isReady) {
                isReady = false;
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdLoadFailed(int errorCode, String errorMessage) {
            Log.i(TAG, "onAdLoadFailed errorCode=" + errorCode + ",errorMessage=" + errorMessage);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(errorCode + "");
                tpError.setErrorMessage(errorMessage);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdLoaded() {
            Log.i(TAG, "onAdLoaded");
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mSplashAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdRenderFailed() {
            Log.i(TAG, "onAdRenderFailed");
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    };

    @Override
    public void showAd() {
    }

    @Override
    public void clean() {
        try {
            if (mSplashAd != null) {
                mSplashAd.destroy();
                mSplashAd = null;
                mAdContainerView.removeAllViews();
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy Exception:" + e.getMessage());
        }
    }

    @Override
    public boolean isReady() {
        return isReady;
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
