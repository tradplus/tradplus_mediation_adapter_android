package com.tradplus.ads.huawei;

import android.content.Context;
import android.util.Log;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.banner.BannerView;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusDataConstants;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class HuaweiBanner extends TPBannerAdapter {

    private String mPlacementId;
    private String mAdSize = TradPlusDataConstants.BANNER;
    private BannerView bannerView;
    private TPBannerAdImpl mTPBannerAd;
    private int isAdLeave = 0;
    private static final String TAG = "HuaweiBanner";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + mPlacementId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
            }
            Log.i(TAG, "BannerSize: " + mAdSize);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }


        HuaweiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
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
        bannerView.setAdId(mPlacementId);
        bannerView.setBannerRefresh(0);
        bannerView.setBannerAdSize(calculateAdSize(mAdSize));

        AdListener adListener = new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.i(TAG, "onAdLoaded: ");
                mTPBannerAd = new TPBannerAdImpl(null, bannerView);
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoaded(mTPBannerAd);
            }

            @Override
            public void onAdFailed(int errorCode) {
                Log.i(TAG, "onAdFailed: errorCode : " + errorCode);
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(errorCode + "");
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            public void onAdOpened() {
                Log.i(TAG, "onAdOpened: ");
            }

            @Override
            public void onAdClicked() {
                Log.i(TAG, "onAdClicked: ");
                if (mTPBannerAd != null)
                    mTPBannerAd.adClicked();
            }

            @Override
            public void onAdLeave() {
                Log.i(TAG, "onAdLeave: ");
                isAdLeave = 1;
            }

            @Override
            public void onAdClosed() {
                Log.i(TAG, "onAdClosed: ");
                if (mTPBannerAd != null && (isAdLeave == 0 || isAdLeave == 2)) {
                    mTPBannerAd.adClosed();
                }
                isAdLeave = 2;
            }

            @Override
            public void onAdImpression() {
                Log.i(TAG, "onAdImpression: ");
                if (mTPBannerAd != null && isAdLeave == 0) {
                    mTPBannerAd.adShown();
                }

            }
        };

        bannerView.setAdListener(adListener);
        bannerView.loadAd(new AdParam.Builder().build());

    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (bannerView != null) {
            bannerView.setAdListener(null);
            bannerView.destroy();
            bannerView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_HUAWEI);
    }

    @Override
    public String getNetworkVersion() {
        return HwAds.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (mPlacementId != null && mPlacementId.length() > 0);
    }

    private BannerAdSize calculateAdSize(String adSize) {
        Log.i(TAG, "calculateAdSize: " + adSize);
        if (TradPlusDataConstants.BANNER.equals(adSize)) {
            return BannerAdSize.BANNER_SIZE_320_50;
        } else if (TradPlusDataConstants.LARGEBANNER.equals(adSize)) {
            return BannerAdSize.BANNER_SIZE_320_100;
        } else if (TradPlusDataConstants.MEDIUMRECTANGLE.equals(adSize)) {
            return BannerAdSize.BANNER_SIZE_300_250;
        } else if (TradPlusDataConstants.FULLSIZEBANNER.equals(adSize)) {
            return BannerAdSize.BANNER_SIZE_360_57;
        } else if (TradPlusDataConstants.LEADERBOAD.equals(adSize)) {
            return BannerAdSize.BANNER_SIZE_360_144;
        } else if (TradPlusDataConstants.DEVICE_ID_EMULATOR.equals(adSize)) {
            return BannerAdSize.BANNER_SIZE_SMART;
        }
        return BannerAdSize.BANNER_SIZE_320_50;
    }

}
