package com.tradplus.startapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.model.AdPreferences;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class StartAppBanner extends TPBannerAdapter {
    public static final String TAG = "StartAppBanner";
    private TPBannerAdImpl tpBannerAd;
    private Banner mBanner;
    private String mPlacementId;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        setDefaultAdViewSize(320,50);

        StartAppInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });


    }

    private void requestAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        AdPreferences adPreferences = new AdPreferences();
        adPreferences.setAdTag(mPlacementId);
        mBanner = new Banner(activity, adPreferences);
        mBanner.setBannerListener(new BannerListener() {
            @Override
            public void onReceiveAd(View view) {
                Log.i(TAG, "onReceiveAd: ");

                if (mLoadAdapterListener != null) {
                    tpBannerAd = new TPBannerAdImpl(null, mBanner);
                    mLoadAdapterListener.loadAdapterLoaded(tpBannerAd);
                }
            }

            @Override
            public void onFailedToReceiveAd(View view) {
                Log.i(TAG, "onFailedToReceiveAd: ");
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onImpression(View view) {
                Log.i(TAG, "onImpression: ");
                if (tpBannerAd != null) {
                    tpBannerAd.adShown();
                }
            }

            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: ");
                if (tpBannerAd != null) {
                    tpBannerAd.adClicked();
                }
            }
        });
        mBanner.loadAd(mAdViewWidth, mAdViewHeight);
    }


    @Override
    public void clean() {
        if (mBanner != null) {
            mBanner.setBannerListener(null);
            mBanner.removeAllViews();
            mBanner = null;
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
