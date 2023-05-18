package com.tradplus.ads.facebook;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.BuildConfig;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.Views;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

public class FacebookBanner extends TPBannerAdapter {

    private AdView mFacebookBanner;
    private String mAdSize = BANNER;
    private String placementId;
    private TPBannerAdImpl mTpBannerAd;
    private String payload;
    private static final String TAG = "FacebookBanner";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (serverExtrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            Log.d(TAG, "adsize:" + tpParams.get(AppKeyManager.ADSIZE + placementId));
            if (tpParams.containsKey(AppKeyManager.ADSIZE + placementId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + placementId);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        if (userParams.size() > 0) {

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                if (coppa) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
                    return;
                }
            }
        }

        FacebookInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
    }


    private void requestAd(Context context){
        mFacebookBanner = new AdView(context, placementId, calculateAdSize(mAdSize));
        AdListener adListener = new AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                Log.i(TAG, "Facebook banner ad load failed " + " , ErrorCode : " + adError.getErrorCode() + ", ErrorMessage : " + adError.getErrorMessage());

                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(FacebookErrorUtil.getTradPlusErrorCode(adError));

            }

            @Override
            public void onAdLoaded(Ad ad) {
                if (mFacebookBanner == null) {
                    return;
                }
                Log.i(TAG, "onAdLoaded: ");
                mTpBannerAd = new TPBannerAdImpl(null, mFacebookBanner);
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.i(TAG, "onAdClicked: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adClicked();
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.i(TAG, "onLoggingImpression: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adShown();
            }
        };

        mFacebookBanner.loadAd(mFacebookBanner.buildLoadAdConfig()
                .withBid(TextUtils.isEmpty(payload) ? "" : payload)
                .withAdListener(adListener).build());
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");

        if (mFacebookBanner != null) {
            Views.removeFromParent(mFacebookBanner);
            mFacebookBanner.destroy();
            mFacebookBanner = null;
        }

    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_FACEBOOK);
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    private AdSize calculateAdSize(String adSize) {
        if (adSize.equals(BANNER)) {
            return AdSize.BANNER_HEIGHT_50; //320 * 50
        } else if (adSize.equals(LARGEBANNER)) {
            return AdSize.BANNER_HEIGHT_90; //320 * 90
        } else if (adSize.equals(MEDIUMRECTANGLE)) {
            return AdSize.RECTANGLE_HEIGHT_250; // 320 * 250
        }
        return AdSize.BANNER_HEIGHT_50;
    }

    @Override
    public String getBiddingToken() {
        try {
            return BidderTokenProvider.getBidderToken(GlobalTradPlus.getInstance().getContext());
        } catch (Exception e) {

        }
        return null;
    }
}
