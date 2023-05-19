package com.tradplus.startapp;

import android.content.Context;
import android.util.Log;

import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class StartAppNative extends TPNativeAdapter {
    public static final String TAG = "StartAppNative";
    private StartAppNativeAd mNativeView;
    private String mPlacementId;
    private TPNativeAdView mTpNativeAdView;
    private StartappNativeAd mStartAppNativeAd;
    private int mSecondaryImageSize = 2; //icon for image size 150px X 150px
    private int mPrimaryImageSize = 4; //image for image size 1200px X 628px
    private boolean mNeedDownloadImg = false;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }

            if (userParams.containsKey(AppKeyManager.START_NATIVE_SECONDARY_IMAGE_SIZE)) {
                mSecondaryImageSize = (int) userParams.get(AppKeyManager.START_NATIVE_SECONDARY_IMAGE_SIZE);
                Log.i(TAG, "userParams SecondaryImageSize: " + mSecondaryImageSize);
            }

            if (userParams.containsKey(AppKeyManager.START_NATIVE_PRIMARY_IMAGE_SIZE)) {
                mPrimaryImageSize = (int) userParams.get(AppKeyManager.START_NATIVE_PRIMARY_IMAGE_SIZE);
                Log.i(TAG, "userParams PrimaryImageSize: " + mPrimaryImageSize);
            }
        }


        StartAppInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestAd(final Context context) {
        mNativeView = new StartAppNativeAd(context);
        NativeAdPreferences nativeAdPreferences = new NativeAdPreferences().setAdsNumber(3)
                .setAutoBitmapDownload(true)
                .setSecondaryImageSize(mSecondaryImageSize)// 72*72px
                .setPrimaryImageSize(mPrimaryImageSize);// 340*340px
        nativeAdPreferences.setAdTag(mPlacementId);
        mNativeView.loadAd(nativeAdPreferences, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                Log.d(TAG, "onReceiveAd: ");

                final ArrayList<NativeAdDetails> nativeAds = mNativeView.getNativeAds();

                if (nativeAds != null && nativeAds.size() > 0) {

                    final NativeAdDetails nativeAdDetails = nativeAds.get(0);
                    mStartAppNativeAd = new StartappNativeAd(context, nativeAdDetails);
                    downloadAndCallback(mStartAppNativeAd, mNeedDownloadImg);
                }
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
                Log.d(TAG, "onFailedToReceiveAd: " + ad.getErrorMessage());
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(ad.getErrorMessage());
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

            }
        });
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mNativeView != null) {
            mNativeView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_STARTAPP);
    }

    @Override
    public String getNetworkVersion() {
        return StartAppSDK.getVersion();
    }

}
