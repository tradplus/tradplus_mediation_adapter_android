package com.tradplus.ads.adfly;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
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
import com.adfly.sdk.nativead.NativeAd;
import com.adfly.sdk.nativead.NativeAdListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class AdFlyNative extends TPNativeAdapter {

    private String mPlacementId;
    private AdFlyNativeAd mAdFlyNativeAd;
    private NativeAd mNativeAd;
    private static final String TAG = "AdFlyNative";

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
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        mNativeAd = new NativeAd(mPlacementId);
        mNativeAd.setAdListener(new NativeAdListener() {
            @Override
            public void onMediaDownloaded(AdflyAd ad) {
            }

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

                if (mAdFlyNativeAd != null) {
                    mAdFlyNativeAd.onAdVideoError(tpError);
                }

            }

            @Override
            public void onAdLoaded(AdflyAd ad) {
                // 在收到 onAdLoaded 回调后可以开始显示广告
                Log.i(TAG, "onAdLoaded: ");
                mAdFlyNativeAd = new AdFlyNativeAd(activity, mNativeAd);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mAdFlyNativeAd);
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
                if (mAdFlyNativeAd != null) {
                    mAdFlyNativeAd.onAdViewClicked();
                }
            }

            @Override
            public void onAdImpression(AdflyAd ad) {
                Log.i(TAG, "onAdImpression: ");
                if (mAdFlyNativeAd != null) {
                    mAdFlyNativeAd.onAdViewExpanded();
                }
            }
        });
        mNativeAd.loadAd();
    }

    @Override
    public void clean() {

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
