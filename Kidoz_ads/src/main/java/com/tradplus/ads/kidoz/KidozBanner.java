package com.tradplus.ads.kidoz;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.kidoz.sdk.api.KidozSDK;
import com.kidoz.sdk.api.ui_views.kidoz_banner.KidozBannerListener;
import com.kidoz.sdk.api.ui_views.new_kidoz_banner.KidozBannerView;
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
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;


public class KidozBanner extends TPBannerAdapter {

    private TPBannerAdImpl mTpBannerAd;
    private String appId, placementId;
    private String mAdSize;
    private String appToken;
    private KidozBannerView kidozBanner;

    private static final String TAG = "KidozBanner";

    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (serverExtrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            appToken = tpParams.get(AppKeyManager.APPTOKEN);
            Log.i(TAG, "loadBanner: appId： " + appId + "， placementId ：" + placementId + ", appToken :" + appToken);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
                return;
            }
        }


//        appId = "14452";
//        appToken = "V44ZTKg086Kc9B48AATufEs98LRcBlZv";
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (!KidozSDK.isInitialised() ) {
            KidozInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    requestBanner(activity);
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
        } else {
            requestBanner(activity);
        }

    }


    private void requestBanner(Activity activity) {
        kidozBanner = KidozSDK.getKidozBanner(activity);
//        kidozBanner.setLayoutParams(new RelativeLayout.LayoutParams(DeviceUtils.dip2px(activity, 320), DeviceUtils.dip2px(activity, 50)));
        kidozBanner.setLayoutWithoutShowing();
        kidozBanner.setKidozBannerListener(new KidozBannerListener() {
            @Override
            public void onBannerViewAdded() {
                Log.i(TAG, "onBannerViewAdded: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adShown();
            }

            @Override
            public void onBannerReady() {
                Log.i(TAG, "onBannerReady: ");
                mTpBannerAd = new TPBannerAdImpl(null, kidozBanner);
                if (kidozBanner != null) {
                    kidozBanner.show();

                    if (mLoadAdapterListener != null)
                        mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                }else {
                    TPError tpError = new TPError(SHOW_FAILED);
                    if (mLoadAdapterListener != null)
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

            }

            @Override
            public void onBannerError(String s) {
                Log.i(TAG, "onBannerError: errorMsg :" + s);
                TPError tpError = new TPError(SHOW_FAILED);

                tpError.setErrorMessage(s);
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            public void onBannerClose() {
                Log.i(TAG, "onBannerClose: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adClosed();
            }

            @Override
            public void onBannerNoOffers() {
                Log.i(TAG, "onBannerNoOffers: ");
            }
        });
        kidozBanner.load();
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (kidozBanner != null) {
            kidozBanner.setKidozBannerListener(null);
            kidozBanner.destroy();
            kidozBanner = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KIDOZ);
    }

    @Override
    public String getNetworkVersion() {
        return KidozSDK.getSDKVersion();
    }

    private boolean serverExtrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

}

