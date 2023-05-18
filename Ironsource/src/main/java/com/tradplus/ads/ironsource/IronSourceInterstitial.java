package com.tradplus.ads.ironsource;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class IronSourceInterstitial extends TPInterstitialAdapter {

    private IronSourceInterstitialCallbackRouter ironSourceICbR;
    private String placementId;
    public static final String TAG = "IronSourceInterstitial";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        String appKey;
        if (extrasAreValid(tpParams)) {
            appKey = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            TPError tpError = new TPError(ADAPTER_CONFIGURATION_ERROR);
            tpError.setErrorMessage("Ironsource app_key or instance_id is empty");
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            return;
        }

        IronSourceAdsInterstitialListener ironSourceAdsInterstitialListener = new IronSourceAdsInterstitialListener(placementId, appKey);
        IronSource.setISDemandOnlyInterstitialListener(ironSourceAdsInterstitialListener);

        ironSourceICbR = IronSourceInterstitialCallbackRouter.getInstance();
        ironSourceICbR.addListener(placementId, mLoadAdapterListener);

        IronSourceInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestInterstital();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void requestInterstital() {

        Activity mActivity = GlobalTradPlus.getInstance().getActivity();
        if (mActivity == null) {
            TPError tpError = new TPError(ADAPTER_ACTIVITY_ERROR);
            tpError.setErrorMessage("Ironsource context must be activity.");
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            return;
        }

        if (IronSource.isISDemandOnlyInterstitialReady(placementId)) {
            ironSourceICbR.getListener(placementId).loadAdapterLoaded(null);
        } else {
            IronSource.loadISDemandOnlyInterstitial(mActivity, placementId);
        }
    }


    @Override
    public void showAd() {
        if (mShowListener != null) {
            ironSourceICbR.addShowListener(placementId, mShowListener);
        }

        if (IronSource.isISDemandOnlyInterstitialReady(placementId)) {
            if (placementId != null && placementId.length() > 0) {
                IronSource.showISDemandOnlyInterstitial(placementId);
            }
        } else {
            if (ironSourceICbR.getShowListener(placementId) != null)
                ironSourceICbR.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
        }

    }

    @Override
    public boolean isReady() {
        return IronSource.isISDemandOnlyInterstitialReady(placementId)  && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        super.clean();
        if (placementId != null)
            ironSourceICbR.removeListeners(placementId);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_IRONSOURCE);
    }

    @Override
    public String getNetworkVersion() {
        return IronSourceUtils.getSDKVersion();
    }


    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }
}
