package com.tradplus.criteo;

import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import com.criteo.publisher.Criteo;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;


import com.criteo.publisher.CriteoBannerAdListener;
import com.criteo.publisher.CriteoBannerView;
import com.criteo.publisher.CriteoErrorCode;
import com.criteo.publisher.model.AdSize;
import com.criteo.publisher.model.AdUnit;
import com.criteo.publisher.model.BannerAdUnit;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CriteoBanner extends TPBannerAdapter {
    public static final String TAG = "CriteoBanner";
    private CriteoBannerView criteoBannerView;
    private String mCriteoPublisherId;
    private String mAdUnitId;
    private TPBannerAdImpl mTpBannerAd;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (serverExtras != null && serverExtras.size() > 0) {
            mCriteoPublisherId = serverExtras.get(AppKeyManager.APP_ID);
            mAdUnitId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        }

        final BannerAdUnit bannerAdUnit = new BannerAdUnit(mAdUnitId,
                new AdSize(AppKeyManager.NATIVE_DEFAULT_WIDTH, AppKeyManager.BANNER_DEFAULT_HEIGHT));
        List<AdUnit> adUnits = new ArrayList<>();
        adUnits.add(bannerAdUnit);


        CriteoInitManager.getInstance().initSDK(context, localExtras, serverExtras, adUnits, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestBanner(bannerAdUnit, context);
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

    private void requestBanner(BannerAdUnit bannerAdUnit, Context context) {
        if (criteoBannerView == null)
            criteoBannerView = new CriteoBannerView(context, bannerAdUnit);


        criteoBannerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        criteoBannerView.setCriteoBannerAdListener(criteoBannerAdListener);
        criteoBannerView.loadAd();

    }

    final CriteoBannerAdListener criteoBannerAdListener = new CriteoBannerAdListener() {
        @Override
        public void onAdReceived(CriteoBannerView view) {
            Log.i(TAG, "onAdReceived: ");
            mTpBannerAd = new TPBannerAdImpl(null, criteoBannerView);
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }

        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }
        }

        @Override
        public void onAdFailedToReceive(CriteoErrorCode code) {
            Log.i(TAG, "onAdFailedToReceive: " + code.name());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(code.name());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

        }

        @Override
        public void onAdLeftApplication() {
            Log.i(TAG, "onAdLeftApplication: ");
        }
    };

    @Override
    public void clean() {
        if (criteoBannerView != null) {
            criteoBannerView.destroy();
            criteoBannerView.setCriteoBannerAdListener(null);
            criteoBannerView = null;
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
