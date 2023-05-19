package com.tradplus.ads.adcolony;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AdcolonyInterstitial extends TPInterstitialAdapter {
    private String zoneId, zoneIds;
    private AdColonyInterstitial mAdColonyInterstitial;
    private static final String TAG = "AdColonyInterstital";
    private AdcolonyInterstitialCallbackRouter mCallbackRouter;

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras,
                             Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            zoneId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mCallbackRouter = AdcolonyInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(zoneId, mLoadAdapterListener);

        AdColonyInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                AdColony.requestInterstitial(zoneId, listener);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(zoneId, mShowListener);
        }
        if (hasAdAvailable()) {
            mAdColonyInterstitial.show();
        } else {
            if (mCallbackRouter.getShowListener(mAdColonyInterstitial.getZoneID()) != null)
                mCallbackRouter.getShowListener(mAdColonyInterstitial.getZoneID()).onAdVideoError(new TPError(SHOW_FAILED));
        }

    }


    protected boolean hasAdAvailable() {
        return mAdColonyInterstitial != null && !mAdColonyInterstitial.isExpired();
    }

    @Override
    public boolean isReady() {
        if (mAdColonyInterstitial == null) {
            return false;
        } else {
            return !mAdColonyInterstitial.isExpired() && !isAdsTimeOut();
        }
    }

    private final AdColonyInterstitialListener listener = new AdColonyInterstitialListener() {
        @Override
        public void onRequestFilled(AdColonyInterstitial ad) {
            mAdColonyInterstitial = ad;
            Log.i(TAG, "onRequestFilled: ");
            if (mCallbackRouter.getListener(ad.getZoneID()) != null) {
                setNetworkObjectAd(ad);
                mCallbackRouter.getListener(ad.getZoneID()).loadAdapterLoaded(null);
            }

        }


        @Override
        public void onRequestNotFilled(AdColonyZone zone) {
            Log.i(TAG, "onRequestNotFilled: ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage("No Fill");
            if (mCallbackRouter.getListener(zone.getZoneID()) != null)
                mCallbackRouter.getListener(zone.getZoneID()).loadAdapterLoadFailed(tpError);
        }


        @Override
        public void onOpened(AdColonyInterstitial ad) {
            Log.i(TAG, "onOpened: ");
            if (mCallbackRouter.getShowListener(ad.getZoneID()) != null) {
                mCallbackRouter.getShowListener(ad.getZoneID()).onAdShown();
            }

        }


        @Override
        public void onExpiring(AdColonyInterstitial ad) {
        }

        @Override
        public void onClosed(AdColonyInterstitial ad) {
            Log.i(TAG, "onClosed: ");
            if (mCallbackRouter.getShowListener(ad.getZoneID()) != null) {
                mCallbackRouter.getShowListener(ad.getZoneID()).onAdClosed();
            }
        }
    };


    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID) && serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_ADCOLONY);
    }

    @Override
    public String getNetworkVersion() {
        return AdColony.getSDKVersion();
    }

}
