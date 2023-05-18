package com.tradplus.ads.facebook;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.BuildConfig;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;


public class FacebookInterstitial extends TPInterstitialAdapter {

    private InterstitialAd mFacebookInterstitial;
    private String payload;
    private String placementId;
    private static final String TAG = "FacebookInterstitial";

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_FACEBOOK);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }


    @Override
    public void loadCustomAd(final Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {

        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        if (localExtras.size() > 0) {
//            Log.i(TAG, "suportGDPR ccpa: " + localExtras.get(AppKeyManager.KEY_CCPA) + ":COPPA:" + localExtras.get(AppKeyManager.KEY_COPPA));
//            if (localExtras.containsKey(AppKeyManager.KEY_CCPA)) {
//                boolean cppa = (boolean) localExtras.get(AppKeyManager.KEY_CCPA);
//                if (cppa) {
//                    AdSettings.setDataProcessingOptions(new String[]{});
//                } else {
//                    AdSettings.setDataProcessingOptions(new String[]{"LDU"}, 1, 1000);
//                }
//
//            } else {
//                AdSettings.setDataProcessingOptions(new String[]{"LDU"}, 0, 0);
//            }

            if (localExtras.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) localExtras.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                if (coppa) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
                    return;
                }
            }
        }

        FacebookInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void requestAd(Context context) {
        mFacebookInterstitial = new InterstitialAd(context, placementId);
        InterstitialAd.InterstitialAdLoadConfigBuilder interstitialAdLoadConfigBuilder =
                mFacebookInterstitial.buildLoadAdConfig().withAdListener(interstitialAdListener);
        interstitialAdLoadConfigBuilder.withBid(TextUtils.isEmpty(payload) ? "" : payload);
        mFacebookInterstitial.loadAd(interstitialAdLoadConfigBuilder.build());
    }

    private final InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
        @Override
        public void onError(Ad ad, AdError adError) {
            Log.i(TAG, "Facebook interstitial ad load failed " + " ,ErrorCode : " + adError.getErrorCode() +
                    ", ErrorMessage : " + adError.getErrorMessage());

            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(FacebookErrorUtil.getTradPlusErrorCode(adError));

        }

        @Override
        public void onAdLoaded(Ad ad) {
            if (mFacebookInterstitial == null) {
                return;
            }
            setFirstLoadedTime();
            Log.i(TAG, "Facebook interstitial ad loaded successfully.");
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mFacebookInterstitial);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdClicked(Ad ad) {
            Log.i(TAG, "Facebook interstitial ad clicked.");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            Log.i(TAG, "Facebook interstitial ad onLoggingImpression.");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onInterstitialDisplayed(Ad ad) {
            Log.i(TAG, "Showing Facebook interstitial ad.");

        }

        @Override
        public void onInterstitialDismissed(Ad ad) {
            Log.i(TAG, "Facebook interstitial ad dismissed.");
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }
    };

    @Override
    public void showAd() {
        String msg;

        if (mFacebookInterstitial != null && mFacebookInterstitial.isAdLoaded()) {
            mFacebookInterstitial.show();
            msg = "isAdLoaded";
        } else {
            Log.i(TAG, "Tried to show a Facebook interstitial ad before it finished loading. Please try again.");
            if (mShowListener != null) {
//                onError(mFacebookInterstitial, AdError.INTERNAL_ERROR);
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            } else {
                Log.i(TAG, "Interstitial listener not instantiated. Please load interstitial again.");
            }
            msg = "noAdLoaded";
        }

    }

    @Override
    public void clean() {
        super.clean();
        if (mFacebookInterstitial != null) {
            mFacebookInterstitial.destroy();
            mFacebookInterstitial = null;
        }

    }

    @Override
    public boolean isReady() {
        if (mFacebookInterstitial != null) {
            return !isAdsTimeOut() && !mFacebookInterstitial.isAdInvalidated();
        }
        return false;
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getBiddingToken() {
        try {
            return BidderTokenProvider.getBidderToken(GlobalTradPlus.getInstance().getContext());
        } catch (Exception e) {

        }
        return null;
    }
}
