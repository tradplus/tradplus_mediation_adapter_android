package com.tradplus.ads.adcolony;


import android.content.Context;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyZone;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.FULLSIZEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

public class AdcolonyBanner extends TPBannerAdapter {
    private String appId, zoneId, zoneIds;
    private AdColonyAdOptions adOptions;
    private String mAdSize = BANNER;
    private static final String TAG = "AdcolonyBanner";
    private TPBannerAdImpl mTPBannerAd;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(AppKeyManager.APP_ID);
            zoneId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (serverExtras.containsKey(AppKeyManager.ADSIZE + zoneId)) {
                Log.i(TAG, "adsize == " + serverExtras.get(AppKeyManager.ADSIZE + zoneId));
                mAdSize = serverExtras.get(AppKeyManager.ADSIZE + zoneId);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        AdColonyInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner();
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

    private void requestBanner() {
        adOptions = new AdColonyAdOptions();
        AdColony.requestAdView(zoneId, listener, calculateAdSize(mAdSize), adOptions);
    }

    private final AdColonyAdViewListener listener = new AdColonyAdViewListener() {
        @Override
        public void onRequestFilled(AdColonyAdView adColonyAdView) {
            Log.d(TAG, "AdColony banner ad loaded successfully.");

            mTPBannerAd = new TPBannerAdImpl(null, adColonyAdView);
            mLoadAdapterListener.loadAdapterLoaded(mTPBannerAd);
        }

        @Override
        public void onRequestNotFilled(AdColonyZone zone) {
            super.onRequestNotFilled(zone);
            Log.d(TAG, "AdColony Banner ad has no filled.");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage("No Fill");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onOpened(AdColonyAdView ad) {
            super.onOpened(ad);
        }

        @Override
        public void onClosed(AdColonyAdView ad) {
            super.onClosed(ad);
            Log.d(TAG, "AdColony Banner ad closed.");
            if (mTPBannerAd != null) {
                mTPBannerAd.adClosed();
            }
        }

        @Override
        public void onClicked(AdColonyAdView ad) {
            super.onClicked(ad);
            Log.d(TAG, "AdColony Banner ad clicked.");
            if (mTPBannerAd != null) {
                mTPBannerAd.adClicked();
            }
        }

        @Override
        public void onLeftApplication(AdColonyAdView ad) {
            super.onLeftApplication(ad);
            Log.d(TAG, "AdColony Banner ad left Application.");
        }

    };


    @Override
    public void clean() {

    }


    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_ADCOLONY);
    }

    @Override
    public String getNetworkVersion() {
        return AdColony.getSDKVersion();
    }


    private AdColonyAdSize calculateAdSize(String adSize) {
        if (BANNER.equals(adSize)) {
            return AdColonyAdSize.BANNER; //320 * 50
        } else if (LARGEBANNER.equals(adSize)) {
            return AdColonyAdSize.MEDIUM_RECTANGLE; //320 x 250
        } else if (MEDIUMRECTANGLE.equals(adSize)) {
            return AdColonyAdSize.LEADERBOARD; // 728 x 90
        } else if (FULLSIZEBANNER.equals(adSize)) {
            return AdColonyAdSize.SKYSCRAPER; //160 x 600
        }
        return AdColonyAdSize.BANNER;
    }

}
