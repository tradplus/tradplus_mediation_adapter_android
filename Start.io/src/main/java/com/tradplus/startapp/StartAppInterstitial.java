package com.tradplus.startapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.model.AdPreferences;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class StartAppInterstitial extends TPInterstitialAdapter {

    private StartAppAd interstitialAd;
    private String mPlacementId, mAdFormat;
    private StartAppInterstitialCallbackRouter mStartAppICbR;
    private int mIsVideo;
    private static final String TAG = "StartAppInterstitial";


    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mAdFormat = tpParams.get(AppKeyManager.AD_FORMAT);

            if (!TextUtils.isEmpty(mAdFormat)) {
                mIsVideo = Integer.parseInt(mAdFormat);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mStartAppICbR = StartAppInterstitialCallbackRouter.getInstance();
        mStartAppICbR.addListener(mPlacementId, mLoadAdapterListener);

        StartAppInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestAd(Context context) {
        AdPreferences prefs = new AdPreferences();
        prefs.setAdTag(mPlacementId);
        interstitialAd = new StartAppAd(context);
        StartAppAd.AdMode adMode = StartAppAd.AdMode.FULLPAGE;
        if (mIsVideo == AppKeyManager.FULL_TYPE) {
            adMode = StartAppAd.AdMode.VIDEO;
        }
        interstitialAd.loadAd(adMode, prefs, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                Log.i(TAG, "onReceiveAd: ");
                if (mStartAppICbR.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(ad);
                    mStartAppICbR.getListener(mPlacementId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
                Log.i(TAG, "onFailedToReceiveAd: ad : " + ad.getErrorMessage() + "，mPlacementId ：" + mPlacementId);
                if (mStartAppICbR.getListener(mPlacementId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(ad.getErrorMessage());
                    mStartAppICbR.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
                }
            }
        });

    }


    @Override
    public void showAd() {
        if (mShowListener != null)
            mStartAppICbR.addShowListener(mPlacementId, mShowListener);


        if (interstitialAd != null && interstitialAd.isReady() && mPlacementId != null) {
            interstitialAd.showAd(mPlacementId, new AdDisplayListener() {
                @Override
                public void adHidden(Ad ad) {
                    Log.i(TAG, "adHidden: ");
                    if (mStartAppICbR.getShowListener(mPlacementId) != null)
                        mStartAppICbR.getShowListener(mPlacementId).onAdClosed();
                }

                @Override
                public void adDisplayed(Ad ad) {
                    if (mStartAppICbR.getShowListener(mPlacementId) != null) {
                        Log.i(TAG, "adDisplayed onInterstitialShown: " + mPlacementId);
                        mStartAppICbR.getShowListener(mPlacementId).onAdShown();
                    }
                }

                @Override
                public void adClicked(Ad ad) {
                    Log.i(TAG, "adClicked: ");
                    if (mStartAppICbR.getShowListener(mPlacementId) != null)
                        mStartAppICbR.getShowListener(mPlacementId).onAdVideoClicked();
                }

                @Override
                public void adNotDisplayed(Ad ad) {
                    Log.i(TAG, "adNotDisplayed: ");
                    if (mStartAppICbR.getShowListener(mPlacementId) != null) {

                        TPError tpError = new TPError(SHOW_FAILED);
                        tpError.setErrorMessage(ad.getErrorMessage());
                        mStartAppICbR.getShowListener(mPlacementId).onAdVideoError(tpError);
                    }
                }
            });
        } else {
            if (mStartAppICbR.getShowListener(mPlacementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                mStartAppICbR.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mShowListener != null) {
            mStartAppICbR.removeListeners(mPlacementId);
        }

        if (interstitialAd != null) {
            interstitialAd.setVideoListener(null);
            interstitialAd = null;
        }
    }

    @Override
    public boolean isReady() {
        if (interstitialAd != null) {
            return interstitialAd.isReady() && !isAdsTimeOut();
        } else {
            return false;
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
