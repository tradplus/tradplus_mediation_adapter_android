package com.tradplus.criteo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.util.Log;


import com.criteo.publisher.Criteo;
import com.criteo.publisher.CriteoErrorCode;
import com.criteo.publisher.CriteoInterstitial;
import com.criteo.publisher.CriteoInterstitialAdListener;
import com.criteo.publisher.model.AdUnit;
import com.criteo.publisher.model.InterstitialAdUnit;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CriteoInterstitialAd extends TPInterstitialAdapter {
    public static final String TAG = "CriteoInterstitialAd";
    private CriteoInterstitial criteoInterstitial;
    private InterstitialCallbackRouter mCallbackRouter;
    private String mCriteoPublisherId;
    private String mAdUnitId;


    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (serverExtras != null && serverExtras.size() > 0) {
            mCriteoPublisherId = serverExtras.get(AppKeyManager.APP_ID);
            mAdUnitId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mAdUnitId, mLoadAdapterListener);

        final InterstitialAdUnit adUnit = new InterstitialAdUnit(mAdUnitId);
        List<AdUnit> adUnits = new ArrayList<>();
        adUnits.add(adUnit);
        CriteoInitManager.getInstance().initSDK(context, localExtras, serverExtras, adUnits, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "initSDK onSuccess: ");
                requestInterstitial(adUnit);
            }

            @Override
            public void onFailed(String code, String msg) {
                Log.i(TAG, "initSDK onFailed: msg :" + msg);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });


    }

    private void requestInterstitial(InterstitialAdUnit adUnit) {
        Log.i(TAG, "requestInterstitial: adUnit:" + adUnit + " , CriteoPublisherId :" + mCriteoPublisherId);
        if (criteoInterstitial == null)
            criteoInterstitial = new CriteoInterstitial(adUnit);

        criteoInterstitial.setCriteoInterstitialAdListener(criteoInterstitialAdListener);
        criteoInterstitial.loadAd();
    }

    final CriteoInterstitialAdListener criteoInterstitialAdListener = new CriteoInterstitialAdListener() {
        @Override
        public void onAdReceived(CriteoInterstitial interstitial) {
            Log.i(TAG, "onAdReceived: ");
            if (mCallbackRouter.getListener(mAdUnitId) != null) {
                setNetworkObjectAd(criteoInterstitial);
                mCallbackRouter.getListener(mAdUnitId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mCallbackRouter.getShowListener(mAdUnitId) != null) {
                mCallbackRouter.getShowListener(mAdUnitId).onAdClosed();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter.getShowListener(mAdUnitId) != null) {
                mCallbackRouter.getShowListener(mAdUnitId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdOpened() {
            Log.i(TAG, "onAdOpened: ");
            if (mCallbackRouter.getShowListener(mAdUnitId) != null) {
                mCallbackRouter.getShowListener(mAdUnitId).onAdShown();
            }
        }

        @Override
        public void onAdFailedToReceive(CriteoErrorCode code) {
            Log.i(TAG, "onAdFailedToReceive: code :" + code.name());
            if (mCallbackRouter.getListener(mAdUnitId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(code.name());
                mCallbackRouter.getListener(mAdUnitId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdLeftApplication() {
            Log.i(TAG, "onAdLeftApplication: ");
        }
    };

    @Override
    public void showAd() {
        Log.i(TAG, "showInterstitial: ");
        if (mShowListener != null) {
            mCallbackRouter.addShowListener(mAdUnitId, mShowListener);
        }

        if (criteoInterstitial != null) {
            criteoInterstitial.show();
        } else {
            if (mCallbackRouter.getShowListener(mAdUnitId) != null)
                mCallbackRouter.getShowListener(mAdUnitId).onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    @Override
    public boolean isReady() {
        if (criteoInterstitial != null) {
            return criteoInterstitial.isAdLoaded() && !isAdsTimeOut();
        } else {
            return false;
        }

    }

    @Override
    public void clean() {
        super.clean();
        if (criteoInterstitial != null) {
            criteoInterstitial.setCriteoInterstitialAdListener(null);
            criteoInterstitial = null;
        }

        if (mAdUnitId != null) {
            mCallbackRouter.removeListeners(mAdUnitId);
        }
    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_CRITEO);
    }

    @Override
    public String getNetworkVersion() {
        return Criteo.getVersion();
    }
}
