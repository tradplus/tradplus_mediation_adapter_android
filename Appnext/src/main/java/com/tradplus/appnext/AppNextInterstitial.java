package com.tradplus.appnext;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.util.Log;

import com.appnext.ads.interstitial.Interstitial;
import com.appnext.core.AppnextAdCreativeType;
import com.appnext.core.callbacks.OnAdClicked;
import com.appnext.core.callbacks.OnAdClosed;
import com.appnext.core.callbacks.OnAdError;
import com.appnext.core.callbacks.OnAdLoaded;
import com.appnext.core.callbacks.OnAdOpened;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AppNextInterstitial extends TPInterstitialAdapter implements OnAdError, OnAdClicked, OnAdClosed, OnAdLoaded, OnAdOpened {
    public static final String TAG = "AppNextInterstitial";
    private String mPID;
    private Interstitial mInterstitial;
    private AppNextInterstitialCallbackRouter mAppNextInterstitialCallbackRouter;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Log.i(TAG, "loadInterstitial: ");
        if (mLoadAdapterListener == null) {
            return;
        }
        if (serverExtras != null && serverExtras.size() > 0) {
            mPID = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        }
        // TODO: 2021/2/4 目前不支持全屏插屏，激励
//        mPID = "103029bd-5625-4ba2-9293-8a29461b8692";
        mAppNextInterstitialCallbackRouter = AppNextInterstitialCallbackRouter.getInstance();
        mAppNextInterstitialCallbackRouter.addListener(mPID, mLoadAdapterListener);
        mLoadAdapterListener = mAppNextInterstitialCallbackRouter.getListener(mPID);

        AppNextInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestInterstitial(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }

    private void requestInterstitial(Context context) {
        if (mInterstitial == null)
            mInterstitial = new Interstitial(context, mPID);

        mInterstitial.loadAd();
        mInterstitial.setOnAdErrorCallback(this);
        mInterstitial.setOnAdClickedCallback(this);
        mInterstitial.setOnAdClosedCallback(this);
        mInterstitial.setOnAdLoadedCallback(this);
        mInterstitial.setOnAdOpenedCallback(this);
    }

    @Override
    public void showAd() {
        Log.i(TAG, "showInterstitial: ");
        if (mAppNextInterstitialCallbackRouter != null && mShowListener != null) {
            mAppNextInterstitialCallbackRouter.addShowListener(mPID, mShowListener);
        }
        if (mInterstitial != null) {
            mInterstitial.showAd();
        }
    }

    @Override
    public boolean isReady() {
        if (mInterstitial != null) {
            return mInterstitial.isAdLoaded() && !isAdsTimeOut();
        }
        return false;
    }

    @Override
    public void clean() {
        super.clean();
        if (mInterstitial != null) {
            mInterstitial.destroy();
            mInterstitial.setOnAdErrorCallback(null);
            mInterstitial.setOnAdClickedCallback(null);
            mInterstitial.setOnAdClosedCallback(null);
            mInterstitial.setOnAdLoadedCallback(null);
            mInterstitial.setOnAdOpenedCallback(null);
            mInterstitial = null;
        }
    }

    @Override
    public void adError(String s) {
        Log.i(TAG, "adError: ");
        if (mLoadAdapterListener != null) {
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(s);
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }
    }

    @Override
    public void adClicked() {
        Log.i(TAG, "adClicked: ");
        if (mShowListener != null) {
            mShowListener.onAdVideoClicked();
        }
    }

    @Override
    public void onAdClosed() {
        Log.i(TAG, "onAdClosed: ");
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    @Override
    public void adLoaded(String s, AppnextAdCreativeType appnextAdCreativeType) {
        Log.i(TAG, "adLoaded: " + s);
        if (mLoadAdapterListener != null) {
            setNetworkObjectAd(mInterstitial);
            mLoadAdapterListener.loadAdapterLoaded(null);
        }
    }

    @Override
    public void adOpened() {
        Log.i(TAG, "adOpened: ");
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_APPNEXT);
    }

    @Override
    public String getNetworkVersion() {
        return "2.6.5.473";
    }
}
