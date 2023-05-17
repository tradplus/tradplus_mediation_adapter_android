package com.tradplus.ads.google;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VersionInfo;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.common.util.Views;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusDataConstants;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.google.android.gms.ads.AdSize.BANNER;
import static com.google.android.gms.ads.AdSize.FULL_BANNER;
import static com.google.android.gms.ads.AdSize.LARGE_BANNER;
import static com.google.android.gms.ads.AdSize.LEADERBOARD;
import static com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.google.GoogleConstant.PAID_CURRENCYCODE;
import static com.tradplus.ads.google.GoogleConstant.PAID_PRECISION;
import static com.tradplus.ads.google.GoogleConstant.PAID_VALUEMICROS;

class GooglePlayServicesBanner extends TPBannerAdapter {

    private AdView mGoogleAdView;
    private String placementId;
    private String mAdSize;
    private AdRequest request;
    private TPBannerAdImpl tpBannerAd;
    private String id;
    private String mContentUrls;
    private ArrayList<String> mNeighboringUrls;
    private static final String TAG = "AdmobBanner";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (serverExtrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + placementId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + placementId);
            }
            id = tpParams.get(GoogleConstant.ID);
            Log.i(TAG, "BannerSize: " + mAdSize);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(GoogleConstant.GOOGLE_CONTENT_URLS)) {
                Object contentUrl = userParams.get(GoogleConstant.GOOGLE_CONTENT_URLS);
                if (contentUrl instanceof ArrayList) {
                    ArrayList<String> url = (ArrayList<String>) contentUrl;
                    int size = url.size();
                    Log.i(TAG, "contentUrl size : " + size);

                    try {
                        if (size == 1) {
                            mContentUrls = url.get(0);
                        }

                        if (size >= 2) {
                            mNeighboringUrls = new ArrayList<String>();
                            mNeighboringUrls.addAll(url);
                        }
                    }catch(Throwable e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        request = GoogleInitManager.getInstance().getAdmobAdRequest(userParams, mContentUrls, mNeighboringUrls);


        GoogleInitManager.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

    }

    private void requestBanner(Context context) {
        mGoogleAdView = new AdView(context);
        mGoogleAdView.setAdListener(new AdViewListener());
        mGoogleAdView.setAdUnitId(placementId);
        mGoogleAdView.setAdSize(calculateAdSize(mAdSize, context));

        try {
            mGoogleAdView.loadAd(request);
        } catch (Exception e) {
            Log.i(TAG, "Exception: " + e.getLocalizedMessage());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(UNSPECIFIED);
                tpError.setErrorMessage(e.getLocalizedMessage());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }
    }

    private AdSize calculateAdSize(String adSize, Context context) {
        if (TradPlusDataConstants.LARGEBANNER.equals(adSize)) {
            return LARGE_BANNER;
        } else if (TradPlusDataConstants.MEDIUMRECTANGLE.equals(adSize)) {
            return MEDIUM_RECTANGLE;
        } else if (TradPlusDataConstants.FULLSIZEBANNER.equals(adSize)) {
            return FULL_BANNER;
        } else if (TradPlusDataConstants.LEADERBOAD.equals(adSize)) {
            return LEADERBOARD;
        } else if (TradPlusDataConstants.DEVICE_ID_EMULATOR.equals(adSize)) {
            // Step 2 - Determine the screen width (less decorations) to use for the ad width.
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            float widthPixels = displayMetrics.widthPixels;
            float density = displayMetrics.density;
            int adWidth = (int) (widthPixels / density);
            // Step 3 - Get adaptive ad size and return for setting on the ad view.
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth); // smart banner
        } else {
            return BANNER;
        }
    }


    private class AdViewListener extends AdListener {

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            Log.i(TAG, "Google Play Services banner ad failed to load ， errorCode : " + loadAdError.getMessage());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_NO_FILL), loadAdError));
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (tpBannerAd != null) {
                tpBannerAd.adClicked();
            }

        }

        @Override
        public void onAdImpression() {
            Log.i(TAG, "onAdImpression: ");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (tpBannerAd != null) {
                        tpBannerAd.adShown();
                    }
                }
            }, 1000);
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
        }

        @Override
        public void onAdLoaded() {
            if (mGoogleAdView == null) {
                return;
            }
            mGoogleAdView.setOnPaidEventListener(new OnPaidEventListener() {
                @Override
                public void onPaidEvent(@NonNull AdValue adValue) {
                    Log.i(TAG, "onAdImpression: ");

                    long valueMicros = adValue.getValueMicros();
                    String currencyCode = adValue.getCurrencyCode();
                    int precision = adValue.getPrecisionType();
                    Log.i(TAG, "valueMicros: "+valueMicros);

                    if (tpBannerAd != null) {
                        Map<String, Object> map = new HashMap<>();
                        Long value = new Long(valueMicros);
                        double dvalue = value.doubleValue();

                        map.put(PAID_VALUEMICROS, dvalue / 1000 / 1000);
                        map.put(PAID_CURRENCYCODE, currencyCode);
                        map.put(PAID_PRECISION, precision);


                        tpBannerAd.onAdImPaid(map);
                    }
                }
            });

            Log.i(TAG, "onAdLoaded:");
            if (mLoadAdapterListener != null) {
                if (tpBannerAd == null) {
                    tpBannerAd = new TPBannerAdImpl(null, mGoogleAdView);
                }

                mLoadAdapterListener.loadAdapterLoaded(tpBannerAd);
            }


        }

        @Override
        public void onAdOpened() {
            // Code to be executed when an ad opens an overlay that
            // covers the screen.
            Log.i(TAG, "onAdOpened: ");

        }

    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mGoogleAdView != null) {
            Views.removeFromParent(mGoogleAdView);
            mGoogleAdView.setAdListener(null);
            mGoogleAdView.destroy();
            mGoogleAdView = null;
        }

    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(id);
    }

    @Override
    public String getNetworkVersion() {
        VersionInfo version = MobileAds.getVersion();
        int majorVersion = version.getMajorVersion();
        int minorVersion = version.getMinorVersion();
        int microVersion = version.getMicroVersion();
        return majorVersion + "." + minorVersion + "." + microVersion + "";
    }


    private boolean localExtrasAreValid(final Map<String, Object> localExtras) {
        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer
                && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
