package com.tradplus.ads.google;

import static com.google.android.gms.ads.AdSize.BANNER;
import static com.google.android.gms.ads.AdSize.FULL_BANNER;
import static com.google.android.gms.ads.AdSize.LARGE_BANNER;
import static com.google.android.gms.ads.AdSize.LEADERBOARD;
import static com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusDataConstants;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class AdManagerBanner extends TPBannerAdapter {

    private AdManagerAdView adView;
    private String placementId;
    private String mAdSize;
    private AdManagerAdRequest request;
    private TPBannerAdImpl tpBannerAd;
    private String id;
    private String mContentUrls;
    private ArrayList<String> mNeighboringUrls;
    private static final String TAG = "GAMBanner";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mAdSize = tpParams.get(AppKeyManager.ADSIZE + placementId);
            id = tpParams.get(GoogleConstant.ID);
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

        request = AdManagerInit.getInstance().getAdmobAdRequest(userParams,mContentUrls,mNeighboringUrls);

        AdManagerInit.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
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
        adView = new AdManagerAdView(context);
        adView.setAdSizes(calculateAdSize(context, mAdSize));
        adView.setAdUnitId(placementId);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                Log.i(TAG, "onAdClosed: ");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.i(TAG, "onAdFailedToLoad: code: " + loadAdError.getCode() + " ,msg:" + loadAdError.getMessage());
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(loadAdError.getMessage());
                tpError.setErrorCode(loadAdError.getCode() + "");
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdOpened() {
                Log.i(TAG, "onAdOpened: ");
            }

            @Override
            public void onAdLoaded() {
                Log.i(TAG, "onAdLoaded: ");
                if (tpBannerAd == null) {
                    tpBannerAd = new TPBannerAdImpl(null, adView);
                }
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(tpBannerAd);
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
                if (tpBannerAd != null) {
                    tpBannerAd.adShown();
                }
            }
        });

        adView.loadAd(request);
    }

    @Override
    public void clean() {
        if (adView != null) {
            adView.setAdListener(null);
            adView.destroy();
            adView = null;
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

    private AdSize calculateAdSize(Context context, String adSize) {
        Log.i(TAG, "BannerSize: " + mAdSize);
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
}
