package com.tradplus.ads.yandex;

import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.util.Log;


import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.BidderTokenLoadListener;
import com.yandex.mobile.ads.common.BidderTokenLoader;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdLoader;
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration;

import java.util.Map;

public class YandexNative extends TPNativeAdapter {

    private static final String TAG = "YandexNative";
    private String mPlacementId, payload;
    private YandexNativeAd mYandexNativeAd;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        }

        YandexInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestNative(context);
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

    private void requestNative(final Context context) {
        NativeAdLoader loader = new NativeAdLoader(context);
        NativeAdRequestConfiguration nativeAdRequestConfiguration = new NativeAdRequestConfiguration.Builder(mPlacementId)
                .setShouldLoadImagesAutomatically(true)
                .setBiddingData(payload)
                .build();

        loader.setNativeAdLoadListener(new NativeAdLoadListener() {
            @Override
            public void onAdLoaded( NativeAd nativeAd) {
                Log.i(TAG, "onAdLoaded: ");
                mYandexNativeAd = new YandexNativeAd(context, nativeAd);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mYandexNativeAd);
                }
            }

            @Override
            public void onAdFailedToLoad( AdRequestError adRequestError) {
                Log.i(TAG, "onAdFailedToLoad: ");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (adRequestError != null) {
                    int code = adRequestError.getCode();
                    String description = adRequestError.getDescription();
                    tpError.setErrorMessage(description);
                    tpError.setErrorCode(code+"");
                    Log.i(TAG, "code :" + code + ", description :" + description);
                }

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }


            }
        });
        loader.loadAd(nativeAdRequestConfiguration);
    }

    @Override
    public void clean() {

    }

    @Override
    public String getNetworkName() {
        return YandexInitManager.TAG_YANDEX;
    }

    @Override
    public String getNetworkVersion() {
        return MobileAds.getLibraryVersion();
    }

    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        BidderTokenLoader.loadBidderToken(context, new BidderTokenLoadListener() {
            @Override
            public void onBidderTokenLoaded(final String bidderToken) {
                Log.i(TAG, "onBidderTokenLoaded: ");
                onS2STokenListener.onTokenResult(bidderToken, null);
            }

            @Override
            public void onBidderTokenFailedToLoad(final String failureReason) {
                Log.i(TAG, "onBidderTokenFailedToLoad: ");
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }
}
