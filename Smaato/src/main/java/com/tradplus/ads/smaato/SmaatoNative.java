package com.tradplus.ads.smaato;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.RelativeLayout;


import com.smaato.sdk.nativead.NativeAd;
import com.smaato.sdk.nativead.NativeAdRequest;
import com.smaato.sdk.nativead.NativeAdError;
import com.smaato.sdk.nativead.NativeAdRenderer;
import com.smaato.sdk.core.SmaatoSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;


public class SmaatoNative extends TPNativeAdapter {


    private String mPlacementId;
    NativeAdRequest request;
    private RelativeLayout mNativeAdView;
    private SmaatoNativeAd mSmaatoNativeAd;
    private static final String TAG = "SmattoNative";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (serverExtrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }


        SmaatoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

    }

    private void requestNative(Context context) {

        mNativeAdView = new RelativeLayout(context);


        request = NativeAdRequest.builder().adSpaceId(mPlacementId).shouldReturnUrlsForImageAssets(false).build();
        // if manually rendering
        // if auto-rendering, set above value to true
        // load native ad


        NativeAd.loadAd(mNativeAdView, request, new NativeAd.Listener() {
            @Override
            public void onAdLoaded(NativeAd nativeAd, NativeAdRenderer nativeAdRenderer) {
                if (mLoadAdapterListener != null && mNativeAdView != null) {
                    mSmaatoNativeAd = new SmaatoNativeAd(context, nativeAdRenderer);
                    mLoadAdapterListener.loadAdapterLoaded(mSmaatoNativeAd);
                    Log.i(TAG, "onAdLoaded: ");
                }
            }

            @Override
            public void onAdFailedToLoad(NativeAd nativeAd, NativeAdError nativeAdError) {
                Log.i(TAG, "onAdFailedToLoad: ");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(nativeAdError.name());
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            public void onAdImpressed(NativeAd nativeAd) {
                Log.i(TAG, "onAdImpressed: ");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSmaatoNativeAd != null)
                            mSmaatoNativeAd.onAdViewExpanded();
                    }
                }, 500);

            }

            @Override
            public void onAdClicked(NativeAd nativeAd) {
                Log.i(TAG, "onAdClicked: ");
                if (mSmaatoNativeAd != null)
                    mSmaatoNativeAd.onAdViewClicked();
            }

            @Override
            public void onTtlExpired(NativeAd nativeAd) {
                Log.i(TAG, "onTtlExpired: ");
            }
        });
    }


    private boolean serverExtrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    private boolean localExtrasAreValid(final Map<String, Object> userParams) {
        return userParams.get(DataKeys.AD_WIDTH) instanceof Integer
                && userParams.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    @Override
    public void clean() {

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_SMAATO);
    }

    @Override
    public String getNetworkVersion() {
        return SmaatoSdk.getVersion();
    }


}
