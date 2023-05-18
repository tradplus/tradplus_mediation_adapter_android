package com.tradplus.ads.adfly;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.util.Log;

import com.adfly.sdk.core.AdError;
import com.adfly.sdk.core.AdFlySdk;
import com.adfly.sdk.core.AdflyAd;
import com.adfly.sdk.interstitial.InterstitialAd;
import com.adfly.sdk.interstitial.InterstitialAdListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class AdFlyInterstitial extends TPInterstitialAdapter {

    private String mPlacementId;
    private InterstitialCallbackRouter mCallbackRouter;
    private InterstitialAd mInterstitialAd;
    private static final String TAG = "AdFlyInterstitial";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
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
        mInterstitialAd = InterstitialAd.getInstance(mPlacementId);
        mInterstitialAd.setAdListener(new InterstitialAdListener() {
            @Override
            public void onAdLoadSuccess(AdflyAd ad) {
                Log.i(TAG, "onAdLoadSuccess: ");
                if (mCallbackRouter.getListener(ad.getUnitId()) != null) {
                    setNetworkObjectAd(mInterstitialAd);
                    mCallbackRouter.getListener(ad.getUnitId()).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdLoadFailure(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onAdLoadFailure: ");
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
            public void onAdShowed(AdflyAd ad) {
                Log.i(TAG, "onAdShowed: ");
                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdShown();
                }

                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoStart();
                }
            }

            @Override
            public void onAdShowError(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onAdShowError: ");
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
            public void onAdClosed(AdflyAd ad) {
                Log.i(TAG, "onAdClosed: ");
                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoEnd();
                }

                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdClosed();
                }
            }

            @Override
            public void onAdClick(AdflyAd ad) {
                Log.i(TAG, "onAdClick: ");
                if (mCallbackRouter.getShowListener(ad.getUnitId()) != null) {
                    mCallbackRouter.getShowListener(ad.getUnitId()).onAdVideoClicked();
                }
            }
        });
        mInterstitialAd.loadAd();
    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        if (mInterstitialAd == null) {
            TPError tpError = new TPError(UNSPECIFIED);
            tpError.setErrorMessage("showfailed，mInterstitialAd == null");
            mShowListener.onAdVideoError(tpError);
            return;
        }

        mInterstitialAd.show();
    }

    @Override
    public boolean isReady() {
        return mInterstitialAd != null && mInterstitialAd.isReady() && !mInterstitialAd.isAdInvalidated() && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setAdListener(null);
            mInterstitialAd.destroy();
            mInterstitialAd = null;
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
