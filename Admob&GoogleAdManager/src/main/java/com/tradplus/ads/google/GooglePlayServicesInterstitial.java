package com.tradplus.ads.google;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.google.GoogleConstant.PAID_CURRENCYCODE;
import static com.tradplus.ads.google.GoogleConstant.PAID_PRECISION;
import static com.tradplus.ads.google.GoogleConstant.PAID_VALUEMICROS;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.util.Log;


import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;


public class GooglePlayServicesInterstitial extends TPInterstitialAdapter {


    private InterstitialAd mInterstitialAd;
    private AdRequest request;
    private String adUnitId;
    private Integer mVideoMute = 0;
    private String id;
    private static final String TAG = "AdmobInterstitial";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (extrasAreValid(tpParams)) {
            adUnitId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            id = tpParams.get(GoogleConstant.ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(GoogleConstant.VIDEO_ADMOB_MUTE)) {
                mVideoMute = (int) userParams.get(GoogleConstant.VIDEO_ADMOB_MUTE);
            }
        }

        request = GoogleInitManager.getInstance().getAdmobAdRequest(userParams,null,null);

        GoogleInitManager.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                if (mVideoMute != 0) {
                    MobileAds.setAppMuted(mVideoMute == 1);
                }
                requestInterstitial(context);
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

    private void requestInterstitial(Context context) {
        try {
            InterstitialAd.load(context, adUnitId, request, mInterstitialAdLoadCallback);
        } catch (Throwable e) {
            Log.i(TAG, "Throwable: " + e.getLocalizedMessage());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(UNSPECIFIED);
                tpError.setErrorMessage(e.getLocalizedMessage());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }
    }

    private final InterstitialAdLoadCallback mInterstitialAdLoadCallback = new InterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(InterstitialAd interstitialAd) {
            Log.i(TAG, "onAdLoaded: ");
            setFirstLoadedTime();
            mInterstitialAd = interstitialAd;

            mInterstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                @Override
                public void onPaidEvent(@NonNull AdValue adValue) {
                    Log.i(TAG, "onAdImpression: ");

                    long valueMicros = adValue.getValueMicros();
                    String currencyCode = adValue.getCurrencyCode();
                    int precision = adValue.getPrecisionType();

                    if (mShowListener != null) {
                        Map<String, Object> map = new HashMap<>();
                        Long value = new Long(valueMicros);
                        double dvalue = value.doubleValue();

                        map.put(PAID_VALUEMICROS, dvalue / 1000 / 1000);
                        map.put(PAID_CURRENCYCODE, currencyCode);
                        map.put(PAID_PRECISION, precision);

                        mShowListener.onAdImpPaid(map);
                    }
                }
            });

            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(interstitialAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }


            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Log.i(TAG, "The ad was dismissed.");
                    if (mShowListener != null) {
                        mShowListener.onAdClosed();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                    Log.i(TAG, "onAdFailedToShowFullScreenContent: code :" + adError.getCode() + ", msg :" + adError.getMessage());
                    if (mShowListener != null) {
                        TPError tpError = new TPError(SHOW_FAILED);
                        tpError.setErrorCode(adError.getCode() + "");
                        tpError.setErrorMessage(adError.getMessage());
                        mShowListener.onAdVideoError(tpError);
                    }
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    mInterstitialAd = null;
                    Log.i(TAG, "The ad was shown.");
                    if (mShowListener != null) {
                        mShowListener.onAdShown();
                    }
                }

                @Override
                public void onAdClicked() {
                    Log.i(TAG, "onAdClicked: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoClicked();
                    }
                }
            });
        }

        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError) {
            Log.i(TAG, "onAdFailedToLoad: code :" + loadAdError.getCode() + " , msg :" + loadAdError.getMessage());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(GoogleErrorUtil.getTradPlusErrorCode(new TPError(NETWORK_NO_FILL), loadAdError));
                mInterstitialAd = null;
            }
        }
    };


    @Override
    public void showAd() {
        if (mInterstitialAd != null) {
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }
            mInterstitialAd.show(activity);

        } else {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
                Log.i(TAG, "Tried to show a Google Play Services interstitial ad before it finished loading. Please try again.");
            }
        }

    }

    @Override
    public boolean isReady() {
        if (mInterstitialAd == null) return false;
        boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
        if (isMainThread) {
            return !isAdsTimeOut();
        } else return !isAdsTimeOut();
    }

    @Override
    public void clean() {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(null);
            mInterstitialAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        Log.i(TAG, "getNetworkName: " + id);
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

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }
}
