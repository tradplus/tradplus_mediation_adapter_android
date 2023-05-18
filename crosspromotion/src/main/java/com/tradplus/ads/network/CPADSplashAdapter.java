package com.tradplus.ads.network;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.tradplus.crosspro.BuildConfig;
import com.tradplus.crosspro.network.splash.CPSplashAd;
import com.tradplus.crosspro.network.splash.CPSplashAdListener;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;

public class CPADSplashAdapter extends TPSplashAdapter {

    private String campaignId;
    private String adSourceId;
    private CPSplashAd cpSplashAd;
    private long timeoutValue = 3 * 60 * 60 * 1000; //3 小时
    private long mFirstLoadTime;
    public final static long TIME_DELTA = 30 * 1000;
    private static String TAG = "CrossPro";

    private int countdown_time;
    private int is_skipable;
    private int direction;


    @Override
    public void showAd() {
        Log.i(TAG, "showAd: ");
        if (cpSplashAd != null) {
            View view = cpSplashAd.getSplashView(new CPSplashAd.OnSplashShownListener() {

                @Override
                public void onShown() {
                    if (mShowListener != null) {
                        mShowListener.onAdShown();
                    }
                }
            });
            if (view != null && mAdContainerView != null) {
                mAdContainerView.removeAllViews();
                mAdContainerView.addView(view);
            }
        }
    }

    @Override
    public void clean() {
        if (cpSplashAd != null) {
            cpSplashAd.setCpSplashAdListener(null);
            cpSplashAd = null;
        }
    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_CPAD);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (extrasAreValid(tpParams)) {
            campaignId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            countdown_time = Integer.parseInt(tpParams.get(AppKeyManager.KEY_COUNTDOWN));
            is_skipable = Integer.parseInt(tpParams.get(AppKeyManager.KEY_SKIP));
            direction = Integer.parseInt(tpParams.get(AppKeyManager.KEY_DIRECTION));
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
                return;
            }
        }
        if (tpParams.containsKey(AppKeyManager.ADSOURCE_PLACEMENT_ID)) {
            adSourceId = tpParams.get(AppKeyManager.ADSOURCE_PLACEMENT_ID);
        }

        cpSplashAd = new CPSplashAd(context, campaignId, adSourceId, countdown_time, is_skipable, direction);
        cpSplashAd.setCpSplashAdListener(new CPSplashAdListener() {
            @Override
            public void onInterstitialLoad() {

            }

            @Override
            public void onInterstitialLoaded() {
                setTimeoutValue(cpSplashAd.getExpreTime());
                mFirstLoadTime = System.currentTimeMillis();
                mLoadAdapterListener.loadAdapterLoaded(null);
            }

            @Override
            public void onInterstitialFailed(TPError error) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(error);
                }
            }

            @Override
            public void onInterstitialShown() {

            }

            @Override
            public void onInterstitialClicked() {
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onLeaveApplication() {

            }

            @Override
            public void onInterstitialDismissed() {
                if (mAdContainerView != null) {
                    mAdContainerView.removeAllViews();
                }
                mShowListener.onAdClosed();
            }

            @Override
            public void onInterstitialRewarded(String currencyName, int amount) {

            }
        });
        cpSplashAd.load();

    }

    public void setTimeoutValue(long timeoutValue) {
        this.timeoutValue = timeoutValue;
    }

    public boolean isAdsTimeOut() {
        return System.currentTimeMillis() - mFirstLoadTime + TIME_DELTA > timeoutValue;
    }

    public boolean extrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
