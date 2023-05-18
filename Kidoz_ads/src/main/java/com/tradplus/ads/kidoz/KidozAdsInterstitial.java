package com.tradplus.ads.kidoz;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.kidoz.sdk.api.KidozInterstitial;
import com.kidoz.sdk.api.KidozSDK;
import com.kidoz.sdk.api.ui_views.interstitial.BaseInterstitial;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class KidozAdsInterstitial extends TPInterstitialAdapter {

    private String appId, appToken, placementId;
    private KidozInterstitialCallbackRouter mKidozICaR;
    private KidozInterstitial mKidozInterstitial;
    private static final String TAG = "KidozInterstitial";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            appToken = tpParams.get(AppKeyManager.APPTOKEN);
            Log.i(TAG, "loadInterstitial: appId： " + appId + "， placementId ：" + placementId + ", appToken :" + appToken);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

//        appId = "14452";
//        appToken = "V44ZTKg086Kc9B48AATufEs98LRcBlZv";

        mKidozICaR = KidozInterstitialCallbackRouter.getInstance();
        mKidozICaR.addListener(placementId, mLoadAdapterListener);

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (!KidozSDK.isInitialised()) {
            KidozInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    requestInerstitial(activity);
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
            requestInerstitial(activity);
        }
    }


    private void requestInerstitial(Activity activity) {
        mKidozInterstitial = new KidozInterstitial(activity, KidozInterstitial.AD_TYPE.INTERSTITIAL);
        mKidozInterstitial.setOnInterstitialEventListener(new BaseInterstitial.IOnInterstitialEventListener() {
            @Override
            public void onClosed() {
                Log.i(TAG, "onClosed: ");
                if (mKidozICaR.getShowListener(placementId) != null)
                    mKidozICaR.getShowListener(placementId).onAdClosed();
            }

            @Override
            public void onOpened() {
                Log.i(TAG, "onOpened: ");
                if (mKidozICaR.getShowListener(placementId) != null)
                    mKidozICaR.getShowListener(placementId).onAdShown();
            }

            @Override
            public void onReady() {
                Log.i(TAG, "onReady: ");
                if (mKidozICaR.getListener(placementId) != null) {
                    setNetworkObjectAd(mKidozInterstitial);
                    mKidozICaR.getListener(placementId).loadAdapterLoaded(null);
                }

            }

            @Override
            public void onLoadFailed() {
                Log.i(TAG, "onLoadFailed: ");
                if (mKidozICaR.getListener(placementId) != null)
                    mKidozICaR.getListener(placementId).loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
            }

            @Override
            public void onNoOffers() {
                Log.i(TAG, "onNoOffers: ");
            }
        });
        mKidozInterstitial.loadAd();
    }

    @Override
    public void showAd() {
        if (mShowListener != null)
            mKidozICaR.addShowListener(placementId, mShowListener);


        if (mKidozInterstitial != null && mKidozInterstitial.isLoaded()) {
            mKidozInterstitial.show();
        } else {
            if (mKidozICaR.getShowListener(placementId) != null)
                mKidozICaR.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));

        }
    }

    @Override
    public boolean isReady() {
        if (mKidozInterstitial != null) {
            return mKidozInterstitial.isLoaded() && !isAdsTimeOut();
        } else {
            return false;
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mKidozInterstitial != null) {
            mKidozInterstitial.setOnInterstitialEventListener(null);
            mKidozInterstitial = null;
        }

        if (placementId != null)
            mKidozICaR.removeListeners(placementId);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KIDOZ);
    }

    @Override
    public String getNetworkVersion() {
        return KidozSDK.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

}
