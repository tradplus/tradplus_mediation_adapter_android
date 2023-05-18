package com.tradplus.ads.adfly;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.adfly.sdk.core.AdError;
import com.adfly.sdk.core.AdFlySdk;
import com.adfly.sdk.core.AdflyAd;
import com.adfly.sdk.splash.SplashAd;
import com.adfly.sdk.splash.SplashAdListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class AdFlySplash extends TPSplashAdapter {

    private String mPlacementId;
    private SplashAd mSplashAd;
    private static final String TAG = "AdFlySplash";

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

        AdFlyInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);


            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(INIT_FAILED));
            }
        });
    }

    private void requestAd(Context context) {
        mSplashAd = SplashAd.getInstance(mPlacementId);
        mSplashAd.setAdListener(new SplashAdListener() {
            @Override
            public void onError(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onError: ");
                TPError tpError = new TPError(SHOW_FAILED);
                if (adError != null) {
                    String errorMessage = adError.getErrorMessage();
                    int errorCode = adError.getErrorCode();
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(errorMessage);
                    Log.i(TAG, "errorCode :" + errorCode + " , errorMsg :" + errorMessage);
                }

                if (mShowListener != null) {
                    mShowListener.onAdVideoError(tpError);
                }
            }

            @Override
            public void onAdLoaded(AdflyAd ad) {
                // 这里可以开始显示广告
                Log.i(TAG, "onAdLoaded: ");
                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(ad);
                    mLoadAdapterListener.loadAdapterLoaded(null);
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

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdClicked(AdflyAd ad) {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onAdImpression(AdflyAd ad) {
                Log.i(TAG, "onAdImpression: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
            }

            @Override
            public void onAdClosed(AdflyAd ad) {
                Log.i(TAG, "onAdClosed: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }
        });
        mSplashAd.loadAd();

    }

    @Override
    public boolean isReady() {
        return mSplashAd != null && mSplashAd.isReady() && !mSplashAd.isAdInvalidated();
    }


    @Override
    public void showAd() {
        if (mAdContainerView == null) {
            if (mShowListener != null) {
                TPError tpError = new TPError(TPError.UNSPECIFIED);
                tpError.setErrorMessage("ShowFailed, mAdContainerView == null");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                Log.i(TAG, "ShowFailed: activity == null");
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        mSplashAd.show(activity, mAdContainerView);

    }


    @Override
    public void clean() {
        if (mSplashAd != null) {
            mSplashAd.setAdListener(null);
            mSplashAd.destroy();
            mSplashAd = null;
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
