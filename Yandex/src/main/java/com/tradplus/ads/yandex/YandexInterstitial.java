package com.tradplus.ads.yandex;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.BidderTokenLoadListener;
import com.yandex.mobile.ads.common.BidderTokenLoader;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;

import java.util.Map;

public class YandexInterstitial extends TPInterstitialAdapter {

    private String placementId, payload;
    private InterstitialCallbackRouter mCallbackRouter;
    private InterstitialAd mInterstitialAd;
    private int onInterstitialAdShow = 0;
    private static final String TAG = "YandexInterstitial";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);
        mLoadAdapterListener = mCallbackRouter.getListener(placementId);


        YandexInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(context);
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

    private void requestInterstitial(Context context) {
        mInterstitialAd = new InterstitialAd(context);
        mInterstitialAd.setAdUnitId(placementId);
        mInterstitialAd.setInterstitialAdEventListener(mInterstitialAdEventListener);
        // Creating an ad targeting object.
        AdRequest.Builder builder = new AdRequest.Builder();
        if (!TextUtils.isEmpty(payload)) {
            Log.i(TAG, "payload: " + payload);
            builder.setBiddingData(payload);
        }
        mInterstitialAd.loadAd(builder.build());
    }

    private final InterstitialAdEventListener mInterstitialAdEventListener = new InterstitialAdEventListener() {
        @Override
        public void onAdLoaded() {
            Log.i(TAG, "onAdLoaded: ");
            setFirstLoadedTime();
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mInterstitialAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
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

            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }

        @Override
        public void onAdShown() {
            if (mCallbackRouter != null && onInterstitialAdShow == 0 &&
                    mCallbackRouter.getShowListener(placementId) != null) {
                Log.i(TAG, "onAdShown: ");
                mCallbackRouter.getShowListener(placementId).onAdShown();
                onInterstitialAdShow = 1;
            }
        }

        @Override
        public void onAdDismissed() {
            Log.i(TAG, "onAdDismissed: ");
            if (mCallbackRouter != null && mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdClosed();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter != null && mCallbackRouter.getShowListener(placementId) != null) {
                mCallbackRouter.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onLeftApplication() {

        }

        @Override
        public void onReturnedToApplication() {

        }

        @Override
        public void onImpression(ImpressionData impressionData) {

        }
    };


    @Override
    public void showAd() {
        if (mShowListener == null) return;
        mCallbackRouter.addShowListener(placementId, mShowListener);

        TPError tpErrorShow = new TPError(UNSPECIFIED);
        if (mInterstitialAd == null) {
            if (mCallbackRouter.getShowListener(placementId) != null) {
                tpErrorShow.setErrorMessage("showFailed: InterstitialAd == null");
                mCallbackRouter.getShowListener(placementId).onAdVideoError(tpErrorShow);
            }
            return;
        }

        mInterstitialAd.show();
    }

    @Override
    public void clean() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setInterstitialAdEventListener(null);
            mInterstitialAd.destroy();
            mInterstitialAd = null;
        }

        if (placementId != null) {
            mCallbackRouter.removeListeners(placementId);
        }
    }

    @Override
    public boolean isReady() {
        return mInterstitialAd != null && mInterstitialAd.isLoaded() && !isAdsTimeOut();
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
