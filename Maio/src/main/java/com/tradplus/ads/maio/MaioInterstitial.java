package com.tradplus.ads.maio;

import android.app.Activity;
import android.content.Context;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import jp.maio.sdk.android.MaioAds;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class MaioInterstitial extends TPInterstitialAdapter {

    private String mPlacementId;
    private MaioInterstitialCallbackRouter maioInterstitialCallbackRouter;
    private static final String TAG = "MaioInterstitial";

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras,
                             Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

//        appId = "m34feaf3f599ab8cb25a8a840dd4ff2e4";
//        mPlacementId = "zc25c58a915fc377f96dd881dc1efbc5f";
        maioInterstitialCallbackRouter = MaioInterstitialCallbackRouter.getInstance();
        maioInterstitialCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        MaioInitManager.getInstance().initSDK(activity, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }


    @Override
    public void showAd() {
        if (maioInterstitialCallbackRouter != null && mShowListener != null) {
            maioInterstitialCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }
        if (MaioAds.canShow(mPlacementId)) {
            MaioAds.show(mPlacementId);
        } else {
            if (maioInterstitialCallbackRouter.getShowListener(mPlacementId) != null) {
                maioInterstitialCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        return MaioAds.canShow(mPlacementId);
    }


    @Override
    public void clean() {
        if (mPlacementId != null) {
            maioInterstitialCallbackRouter.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MAIO);
    }

    @Override
    public String getNetworkVersion() {
        return MaioAds.getSdkVersion();
    }
}
