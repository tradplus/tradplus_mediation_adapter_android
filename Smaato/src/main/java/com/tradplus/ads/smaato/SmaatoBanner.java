package com.tradplus.ads.smaato;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.smaato.sdk.banner.ad.AutoReloadInterval;
import com.smaato.sdk.banner.ad.BannerAdSize;
import com.smaato.sdk.banner.widget.BannerError;
import com.smaato.sdk.banner.widget.BannerView;
import com.smaato.sdk.core.SmaatoSdk;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNERZERO;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

public class SmaatoBanner extends TPBannerAdapter {


    private String mPlacementId;
    private BannerView bannerView;
    private TPBannerAdImpl mTpBannerAd;
    private String mAdSize = BANNER;
    private static final String TAG = "SmattoBanner";


    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (serverExtrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.get(AppKeyManager.ADSIZE + mPlacementId) != null && !BANNERZERO.equals(tpParams.get(AppKeyManager.ADSIZE + mPlacementId))) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
            }
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }


        SmaatoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner(context);
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

    private void requestBanner(Context context) {
        // find bannerView you setup in your activity.xml、
        bannerView = new BannerView(context);
        // 自动刷新关闭
        bannerView.setAutoReloadInterval(AutoReloadInterval.DISABLED);
        // load banner with desired size
        bannerView.loadAd(mPlacementId, calculateAdSize(mAdSize));


        // You can also set BannerView.EventListener to listen to events describing the advertisement lifecycle:
        bannerView.setEventListener(new BannerView.EventListener() {
            @Override
            // banner ad successfully loaded
            public void onAdLoaded(@NonNull BannerView bannerView) {
                Log.i(TAG, "onAdLoaded: ");
                if (mLoadAdapterListener != null) {
                    mTpBannerAd = new TPBannerAdImpl(null, bannerView);
                    mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                }
            }

            // banner ad failed to load
            @Override
            public void onAdFailedToLoad(@NonNull BannerView bannerView, @NonNull BannerError bannerError) {
                Log.i(TAG, "onAdFailedToLoad: " + bannerError.name());
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(bannerError.name());
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            // banner ad was seen by the user
            public void onAdImpression(@NonNull BannerView bannerView) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mTpBannerAd != null) {
                            mTpBannerAd.adShown();
                            Log.i(TAG, "onAdImpression: ");
                        }
                    }
                }, 500);

            }

            @Override
            // banner ad was clicked by the user
            public void onAdClicked(@NonNull BannerView bannerView) {
                Log.i(TAG, "onAdClicked: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adClicked();
            }

            @Override
            // banner ad Time to Live expired
            public void onAdTTLExpired(@NonNull BannerView bannerView) {
                Log.i(TAG, "onAdTTLExpired: ");
            }
        });
    }


    private boolean serverExtrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    private boolean localExtrasAreValid(final Map<String, Object> localExtras) {
        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer
                && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    private BannerAdSize calculateAdSize(String adSize) {
        if (BANNER.equals(adSize)) {
            return BannerAdSize.XX_LARGE_320x50; //320 * 50
        } else if (LARGEBANNER.equals(adSize)) {
            return BannerAdSize.LEADERBOARD_728x90; // 728*90
        } else if (MEDIUMRECTANGLE.equals(adSize)) {
            return BannerAdSize.MEDIUM_RECTANGLE_300x250; // 320 * 250
        }
        return BannerAdSize.XX_LARGE_320x50;
    }

    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        if (bannerView != null) {
            bannerView.setEventListener(null);
            bannerView.destroy();
            bannerView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_SMAATO);
    }

    @Override
    public String getNetworkVersion() {
        return SmaatoSdk.getVersion();
    }


}
