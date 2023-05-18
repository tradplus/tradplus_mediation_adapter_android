package com.tradplus.ads.applovin.carouselui.adapter;


import android.content.Context;
import android.util.Log;

import com.applovin.adview.AppLovinAdView;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.util.Views;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

public class AppLovinBanner extends TPBannerAdapter implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener {

    public static final String TAG = "AppLovinBanner";
    private AppLovinAdView mAppLovinBanner;
    private AppLovinSdk mAppLovinSdk;
    private String mAdSize = BANNER;
    private String zoneId;
    private TPBannerAdImpl mTpBannerAd;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            zoneId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + zoneId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + zoneId);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        AppLovinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                mAppLovinSdk = AppLovinInitManager.getInstance().getAppLovinSdk();
                looadAppLovinAds(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }


    private void looadAppLovinAds(Context context) {
        mAppLovinBanner = new AppLovinAdView(mAppLovinSdk, calculateAdSize(mAdSize), zoneId, context);
        mAppLovinBanner.setId(Views.generateViewId());
        mAppLovinBanner.setAdLoadListener(this);
        mAppLovinBanner.setAdDisplayListener(this);
        mAppLovinBanner.setAdClickListener(this);
        mAppLovinBanner.loadNextAd();
    }


    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public void clean() {
        if (mAppLovinBanner != null) {
            Views.removeFromParent(mAppLovinBanner);
            mAppLovinBanner.setAdLoadListener(null);
            mAppLovinBanner.setAdDisplayListener(null);
            mAppLovinBanner.setAdClickListener(null);
            mAppLovinBanner.destroy();
            mAppLovinBanner = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPLOVIN);
    }

    @Override
    public String getNetworkVersion() {
        return AppLovinSdk.VERSION;
    }

    @Override
    public void adReceived(AppLovinAd appLovinAd) {
        if (mAppLovinBanner == null) {
            return;
        }
        AppLovinAdSize size = mAppLovinBanner.getSize();
        if (size != null) {
            Log.i(TAG, "AppLovinAdSize: " + size);
        }
        Log.i(TAG, "adReceived: ");

        mTpBannerAd = new TPBannerAdImpl(appLovinAd, mAppLovinBanner);

        if (mLoadAdapterListener != null) {
            mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
        }


    }

    @Override
    public void failedToReceiveAd(int errorCode) {
        Log.i(TAG, "AppLovin banner ad failed to load with error code " + errorCode);
        if (mLoadAdapterListener != null)
            mLoadAdapterListener.loadAdapterLoadFailed(AppLovinErrorUtil.getTradPlusErrorCode(errorCode));

    }

    @Override
    public void adDisplayed(AppLovinAd appLovinAd) {
        Log.i(TAG, "AppLovin banner ad loaded Displayed");
        if (mTpBannerAd != null)
            mTpBannerAd.adShown();
    }

    @Override
    public void adHidden(AppLovinAd appLovinAd) {
        Log.i(TAG, "AppLovin banner ad loaded Hidden");

    }

    @Override
    public void adClicked(AppLovinAd appLovinAd) {
        Log.i(TAG, "AppLovin banner ad loaded Clicked");
        if (mTpBannerAd != null)
            mTpBannerAd.adClicked();

    }

    private AppLovinAdSize calculateAdSize(String adSize) {
        Log.i(TAG, "calculateAdSize: " + adSize);
        if (BANNER.equals(adSize)) {
            return AppLovinAdSize.BANNER; // 320 * 50
        } else if (LARGEBANNER.equals(adSize)) {
            return AppLovinAdSize.MREC; // 300 * 250
        } else if (MEDIUMRECTANGLE.equals(adSize)) {
            return AppLovinAdSize.LEADER; // 728 * 90
        }
        return AppLovinAdSize.BANNER;
    }


}
