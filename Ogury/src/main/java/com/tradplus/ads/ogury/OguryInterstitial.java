package com.tradplus.ads.ogury;

import android.content.Context;
import android.util.Log;

import com.ogury.core.OguryError;
import com.ogury.ed.OguryInterstitialAd;
import com.ogury.ed.OguryInterstitialAdListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import io.presage.common.PresageSdk;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;


public class OguryInterstitial extends TPInterstitialAdapter {

    private String appId, mPlacementId;
    private OguryInterstitialCallbackRouter mCallbackRouter;
    private OguryInterstitialAd interstitial;
    private static final String TAG = "OguryInterstitial";

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras,
                             Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(AppKeyManager.APP_ID);
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

//        appId = "OGY-955A72153B4A";
//        mPlacementId = "302544_default";
        mCallbackRouter = OguryInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        OguryInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestInterstitial(Context context) {
        interstitial = new OguryInterstitialAd(context, mPlacementId);
        interstitial.setListener(new OguryInterstitialAdListener() {
            @Override
            public void onAdLoaded() {
                Log.i(TAG, "onAdLoaded: ");
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(interstitial);
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdDisplayed() {
                Log.i(TAG, "onAdDisplayed: ");
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdShown();
                }
            }

            @Override
            public void onAdClicked() {
                Log.i(TAG, "onAdClicked: ");
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdClicked();
                }
            }

            @Override
            public void onAdClosed() {
                Log.i(TAG, "onAdClosed: ");
                if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                    mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
                }
            }

            @Override
            public void onAdError(OguryError oguryError) {
                if (oguryError.getErrorCode() == 2006 && appId != null) {
                    AppKeyManager.getInstance().removeAppKey(appId);
                }
                Log.i(TAG, "onAdError: ErrorCode  :" + oguryError.getErrorCode() + " , Message :" + oguryError.getMessage());
                if (mCallbackRouter.getListener(mPlacementId) != null) {
                    mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(OguryErrorUtil.getTradPlusErrorCode(oguryError));
                }

            }


        });
        interstitial.load();

    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }
        if (interstitial != null) {
            if (interstitial.isLoaded()) {
                interstitial.show();
            } else {
                Log.i(TAG, "showInterstitial: optinVideo.isLoaded == false");
                if (mCallbackRouter.getShowListener(mPlacementId) != null)
                    mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));

            }
        } else {
            Log.i(TAG, "showInterstitial: optinVideo == null");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));

        }
    }

    @Override
    public boolean isReady() {
        if (interstitial == null) {
            return false;
        } else {
            return interstitial.isLoaded() && !isAdsTimeOut();
        }
    }

    @Override
    public void clean() {
        if (interstitial != null) {
            interstitial.setListener(null);
            interstitial = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_OGURY);
    }

    @Override
    public String getNetworkVersion() {
        return PresageSdk.getAdsSdkVersion();
    }

}
