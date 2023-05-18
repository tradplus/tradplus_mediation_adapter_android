package com.tradplus.ads.adfly;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.util.Log;

import com.adfly.sdk.ads.AdView;
import com.adfly.sdk.core.AdError;
import com.adfly.sdk.core.AdFlySdk;
import com.adfly.sdk.core.AdListener;
import com.adfly.sdk.core.AdflyAd;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class AdFlyBanner extends TPBannerAdapter {

    private String mPlacementId;
    private AdView mAdView;
    private TPBannerAdImpl mTpBannerAd;
    private static final String TAG = "AdFlyBanner";

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
        // 高度50dp，30秒刷新一次，未曝光时不刷新
        mAdView = new AdView(context, mPlacementId);
        mAdView.setAutoRefresh(false);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onError(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onError:");
                TPError tpError = new TPError(SHOW_FAILED);
                if (adError != null) {
                    int errorCode = adError.getErrorCode();
                    String errorMessage = adError.getErrorMessage();
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(errorMessage);
                    Log.i(TAG, "errorCode :" + errorCode + " , errorMsg :" + errorMessage);
                }

                if (mTpBannerAd != null) {
                    mTpBannerAd.onAdShowFailed(tpError);
                }
            }

            @Override
            public void onAdLoaded(AdflyAd ad) {
                if (mAdView == null) {
                    TPError tpError = new TPError(UNSPECIFIED);
                    Log.i(TAG, "Load Failed, mAdView == null");
                    tpError.setErrorMessage("Load Failed, mAdView == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    return;
                }

                Log.i(TAG, "onAdLoaded: ");
                mTpBannerAd = new TPBannerAdImpl(ad, mAdView);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                }
            }

            @Override
            public void onAdLoadFailure(AdflyAd ad, AdError adError) {
                Log.i(TAG, "onAdLoadFailure:");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (adError != null) {
                    int errorCode = adError.getErrorCode();
                    String errorMessage = adError.getErrorMessage();
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
                if (mTpBannerAd != null)
                    mTpBannerAd.adClicked();
            }

            @Override
            public void onAdImpression(AdflyAd ad) {
                Log.i(TAG, "onAdImpression: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adShown();
            }
        });

        mAdView.loadAd();
    }

    @Override
    public void clean() {
        if (mAdView != null) {
            mAdView.setAdListener(null);
            mAdView.destroy();
            mAdView = null;
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
