package com.tradplus.criteo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


import com.criteo.publisher.Criteo;
import com.criteo.publisher.CriteoErrorCode;
import com.criteo.publisher.advancednative.CriteoMedia;
import com.criteo.publisher.advancednative.CriteoNativeAd;
import com.criteo.publisher.advancednative.CriteoNativeAdListener;
import com.criteo.publisher.advancednative.CriteoNativeLoader;
import com.criteo.publisher.advancednative.CriteoNativeRenderer;
import com.criteo.publisher.advancednative.RendererHelper;
import com.criteo.publisher.model.AdUnit;
import com.criteo.publisher.model.NativeAdUnit;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CriteoAdvancedNative extends TPNativeAdapter {

    private static final String TAG = "CriteoNative";
    private String mAdUnitId;
    private TPCriteoNativeAd mCriteoNativeAd;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) return;

        if (serverExtras != null && serverExtras.size() > 0) {
            mAdUnitId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

//         mAdUnitId = "190tsfngohsvfkh3hmkm";
//         mCriteoPublisherId = "B-000000";

        final NativeAdUnit nativeAdUnit = new NativeAdUnit(mAdUnitId);
        List<AdUnit> adUnits = new ArrayList<>();
        adUnits.add(nativeAdUnit);

        CriteoInitManager.getInstance().initSDK(context, localExtras, serverExtras, adUnits, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestNative(nativeAdUnit);
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

    private void requestNative(NativeAdUnit nativeAdUnit) {
        CriteoNativeLoader nativeLoader = new CriteoNativeLoader(nativeAdUnit, new MyCriteoNativeAdListener(), new MyCriteoNativeRender());
        nativeLoader.loadAd();
    }

    class MyCriteoNativeRender implements CriteoNativeRenderer {


        @Override
        public View createNativeView(Context context, ViewGroup parent) {
            Log.i(TAG, "createNativeView: ");
            if (mCriteoNativeAd != null) {
                return mCriteoNativeAd.getmViewGroup();
            }
            return null;
        }

        @Override
        public void renderNativeView(RendererHelper helper, View nativeView, CriteoNativeAd nativeAd) {
            if (mCriteoNativeAd != null) {
                TPError tpError = new TPError(UNSPECIFIED);
                if (helper == null) {
                    if (mCriteoNativeAd != null) {
                        tpError.setErrorMessage("render failed,RendererHelper == null ");
                        mCriteoNativeAd.onAdVideoError(tpError);
                    }
                    return;
                }

                CriteoMedia productMedia = nativeAd.getProductMedia();
                if (productMedia == null) {
                    if (mCriteoNativeAd != null) {
                        tpError.setErrorMessage("render failed, productMedia == null ");
                        mCriteoNativeAd.onAdVideoError(tpError);
                    }
                    return;
                }
                Log.i(TAG, "renderNativeView: ");
                mCriteoNativeAd.initData(productMedia, helper);
            }
        }
    }

    class MyCriteoNativeAdListener implements CriteoNativeAdListener {

        @Override
        public void onAdReceived(CriteoNativeAd nativeAd) {
            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }
            Log.i(TAG, "onAdReceived: ");
            mCriteoNativeAd = new TPCriteoNativeAd(nativeAd, context);
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mCriteoNativeAd);
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCriteoNativeAd != null) {
                mCriteoNativeAd.onAdClicked();
            }
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mCriteoNativeAd != null) {
                mCriteoNativeAd.onAdClosed();
            }

        }

        @Override
        public void onAdFailedToReceive(CriteoErrorCode errorCode) {
            Log.i(TAG, "onAdFailedToReceive: " + errorCode.name() + ":msg:" + errorCode.toString());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(errorCode.name());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdImpression() {
            Log.i(TAG, "onAdImpression: ");
            if (mCriteoNativeAd != null) {
                mCriteoNativeAd.onAdShown();
            }
        }

        @Override
        public void onAdLeftApplication() {
            Log.i(TAG, "onAdLeftApplication: ");
        }
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
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
