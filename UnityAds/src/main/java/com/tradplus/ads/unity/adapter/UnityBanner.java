package com.tradplus.ads.unity.adapter;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.Map;


public class UnityBanner extends TPBannerAdapter {

    public static final String APP_ID_KEY = "appId";
    public static final String PLACEMENT_ID_KEY = "placementId";
    private static final String TAG = "UnityBanner";
    private BannerView mBannerView;
    private String mAppId, mPlacementId;
    private TPBannerAdImpl mTpBannerAd;


    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(APP_ID_KEY);
            mPlacementId = tpParams.get(PLACEMENT_ID_KEY);

            setAdHeightAndWidthByService(mPlacementId,tpParams);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
        }


        if (!UnityAds.isInitialized()) {
            UnityAdsInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess: ");
                    requestBanner();
                }

                @Override
                public void onFailed(String code, String msg) {
                    Log.i(TAG, "onFailed: msg :" + msg);
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            });
        } else {
            requestBanner();
        }

    }

    private void requestBanner() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        UnityBannerSize unityBannerSize = new UnityBannerSize(mAdWidth, mAdHeight);
        mBannerView = new BannerView(activity, mPlacementId, unityBannerSize);
        mBannerView.setListener(bannerListener);
        mBannerView.load();
    }

    // Listener for banner events:
    private BannerView.IListener bannerListener = new BannerView.IListener() {
        @Override
        public void onBannerLoaded(BannerView bannerAdView) {
            // Called when the banner is loaded.
            Log.i(TAG, "onBannerLoaded: ");

            mTpBannerAd = new TPBannerAdImpl(null, bannerAdView);

            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
        }

        @Override
        public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {
            Log.i(TAG, "onBannerFailedToLoad: " + errorInfo.errorMessage + ":code:" + errorInfo.errorCode);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(errorInfo.errorMessage);
                tpError.setErrorCode(errorInfo.errorCode + "");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onBannerClick(BannerView bannerAdView) {
            // Called when a banner is clicked.
            Log.i(TAG, "onBannerClick: " + bannerAdView.getPlacementId());
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }

        }

        @Override
        public void onBannerLeftApplication(BannerView bannerAdView) {
            // Called when the banner links out of the application.
            Log.i(TAG, "onBannerLeftApplication: ");
            Log.v("UnityAdsExample", "onBannerLeftApplication: " + bannerAdView.getPlacementId());
        }
    };


    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        if (mBannerView != null) {
            mBannerView.setListener(null);
            mBannerView.destroy();
            mBannerView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_UNITY);
    }

    @Override
    public String getNetworkVersion() {
        return UnityAds.getVersion();
    }
}
