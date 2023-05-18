package com.tradplus.appnext;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

import android.content.Context;
import android.util.Log;

import com.appnext.banners.BannerAdRequest;
import com.appnext.banners.BannerListener;
import com.appnext.banners.BannerSize;
import com.appnext.banners.BannerView;
import com.appnext.core.AppnextAdCreativeType;
import com.appnext.core.AppnextError;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppNextBanner extends TPBannerAdapter {
    public static final String TAG = "AppNextBanner";
    private String mPID;
    private String mAdsize = "1";
    private BannerView bannerView;
    private TPBannerAdImpl mTpBannerAd;

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        if (serverExtras != null && serverExtras.size() > 0) {
            mPID = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (serverExtras.containsKey(AppKeyManager.ADSIZE + mPID)) {
                mAdsize = serverExtras.get(AppKeyManager.ADSIZE + mPID);
            }
        }
//        mPID="f47aaeb4-302c-4733-a13a-22a2bb721aae";


        AppNextInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestBanner(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });


    }

    private void requestBanner(Context context) {
        bannerView = new BannerView(context);
        bannerView.setPlacementId(mPID);
        bannerView.setBannerSize(calculateAdSize(mAdsize));
        bannerView.setBannerListener(bannerListener);
        bannerView.loadAd(new BannerAdRequest());

    }

    final BannerListener bannerListener = new BannerListener() {
        @Override
        public void onAdClicked() {
            super.onAdClicked();
            Log.i(TAG, "onAdClicked: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }
        }

        @Override
        public void onAdLoaded(String s, AppnextAdCreativeType appnextAdCreativeType) {
            super.onAdLoaded(s, appnextAdCreativeType);
            Log.i(TAG, "onAdLoaded: ");
            mTpBannerAd = new TPBannerAdImpl(null, bannerView);
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }
        }

        @Override
        public void onError(AppnextError appnextError) {
            super.onError(appnextError);
            Log.i(TAG, "onError msg: " + appnextError.getErrorMessage());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(appnextError.getErrorMessage());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void adImpression() {
            super.adImpression();
            Log.i(TAG, "adImpression: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adShown();
            }
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        if (bannerView != null) {
            bannerView.setBannerListener(null);
            bannerView.destroy();
            bannerView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPNEXT);
    }

    @Override
    public String getNetworkVersion() {
        return "2.6.5.473";
    }


    private BannerSize calculateAdSize(String adSize) {
        Log.i(TAG, "calculateAdSize: " + adSize);
        switch (adSize) {
            case BANNER:
                return BannerSize.BANNER;  //320X50
            case LARGEBANNER:
                return BannerSize.LARGE_BANNER; //320X100
            case MEDIUMRECTANGLE:
                return BannerSize.MEDIUM_RECTANGLE; //320X250
        }
        return BannerSize.BANNER;
    }
}
