package com.tradplus.ads.applovin.carouselui.adapter;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppLovinInterstitial extends TPInterstitialAdapter implements
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

    public static final String TAG = "AppLovinInterstitial";
    private AppLovinSdk mAppLovinSdk;
    private AppLovinAd mAppLovinAd;
    private AppLovinInterstitialAdDialog mAppLovinInterstitialAdDialog;
    private String zoneId, payload;
    private AppLovinInterstitialCallbackRouter mAppLovinICBR;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            zoneId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = (String) userParams.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mAppLovinICBR = AppLovinInterstitialCallbackRouter.getInstance();
        mAppLovinICBR.addListener(zoneId, mLoadAdapterListener);


        AppLovinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                mAppLovinSdk = AppLovinInitManager.getInstance().getAppLovinSdk();
                loadWithSdkInitialized(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }


    protected void loadWithSdkInitialized(Context context) {
        mAppLovinInterstitialAdDialog = AppLovinInterstitialAd.create(mAppLovinSdk, context);
        mAppLovinInterstitialAdDialog.setAdDisplayListener(this);
        mAppLovinInterstitialAdDialog.setAdClickListener(this);
        mAppLovinInterstitialAdDialog.setAdVideoPlaybackListener(this);
        if (TextUtils.isEmpty(payload)) {
            mAppLovinSdk.getAdService().loadNextAdForZoneId(zoneId, this);
        } else {
            mAppLovinSdk.getAdService()
                    .loadNextAdForAdToken(payload, this);
        }
    }

    @Override
    public void showAd() {
        if (mShowListener != null) {
            mAppLovinICBR.addShowListener(zoneId, mShowListener);
        }

        if (mAppLovinAd != null && mAppLovinInterstitialAdDialog != null) {
            mAppLovinInterstitialAdDialog.showAndRender(mAppLovinAd);
        } else {
            if (zoneId != null && zoneId.length() > 0 && mAppLovinICBR.getShowListener(zoneId) != null) {
                mAppLovinICBR.getShowListener(zoneId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    @Override
    public void clean() {
        super.clean();
        if (mAppLovinInterstitialAdDialog != null) {
            mAppLovinInterstitialAdDialog.setAdDisplayListener(null);
            mAppLovinInterstitialAdDialog.setAdClickListener(null);
            mAppLovinInterstitialAdDialog.setAdVideoPlaybackListener(null);
            mAppLovinInterstitialAdDialog = null;
        }
        if (zoneId != null)
            mAppLovinICBR.removeListeners(zoneId);

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPLOVIN);
    }

    @Override
    public String getNetworkVersion() {
        return AppLovinSdk.VERSION;
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public void adReceived(AppLovinAd appLovinAd) {
        if (mAppLovinInterstitialAdDialog == null) {
            return;
        }
        Log.i(TAG, "adReceived:");
        mAppLovinAd = appLovinAd;
        if (mAppLovinICBR.getListener(appLovinAd.getZoneId()) != null) {
            setNetworkObjectAd(mAppLovinAd);
            mAppLovinICBR.getListener(appLovinAd.getZoneId()).loadAdapterLoaded(null);
        }

    }

    @Override
    public void failedToReceiveAd(final int errorCode) {
        Log.i(TAG, "failedToReceiveAd: AppLovin interstitial ad failed to load with error code " + errorCode);
        if (mAppLovinICBR.getListener(zoneId) != null) {
            mAppLovinICBR.getListener(zoneId).loadAdapterLoadFailed(AppLovinErrorUtil.getTradPlusErrorCode(errorCode));
        }
    }

    @Override
    public void adDisplayed(AppLovinAd appLovinAd) {
        Log.i(TAG, "adDisplayed:");
        if (mAppLovinICBR.getShowListener(appLovinAd.getZoneId()) != null) {
            mAppLovinICBR.getShowListener(appLovinAd.getZoneId()).onAdShown();
        }
    }

    @Override
    public void adHidden(AppLovinAd appLovinAd) {
        Log.i(TAG, "adHidden:");
        if (mAppLovinICBR.getShowListener(zoneId) != null) {
            mAppLovinICBR.getShowListener(zoneId).onAdClosed();
        }
    }

    @Override
    public void adClicked(AppLovinAd appLovinAd) {
        Log.i(TAG, "adClicked:");
        if (mAppLovinICBR.getShowListener(zoneId) != null) {
            mAppLovinICBR.getShowListener(zoneId).onAdVideoClicked();
        }
    }

    @Override
    public void videoPlaybackBegan(AppLovinAd appLovinAd) {
        Log.i(TAG, "videoPlaybackBegan: ");
        if (mAppLovinICBR.getShowListener(appLovinAd.getZoneId()) != null) {
            mAppLovinICBR.getShowListener(appLovinAd.getZoneId()).onAdVideoStart();
        }

    }

    @Override
    public void videoPlaybackEnded(AppLovinAd appLovinAd, double v, boolean b) {
        Log.i(TAG, "videoPlaybackEnded: ");
        if (mAppLovinICBR.getShowListener(appLovinAd.getZoneId()) != null) {
            mAppLovinICBR.getShowListener(appLovinAd.getZoneId()).onAdVideoEnd();
        }

    }

}
