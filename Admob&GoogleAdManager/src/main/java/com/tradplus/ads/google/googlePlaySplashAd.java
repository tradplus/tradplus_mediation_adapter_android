package com.tradplus.ads.google;

import android.app.Activity;
import android.content.Context;
import android.util.Log;


import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.google.GoogleConstant.PAID_CURRENCYCODE;
import static com.tradplus.ads.google.GoogleConstant.PAID_PRECISION;
import static com.tradplus.ads.google.GoogleConstant.PAID_VALUEMICROS;

import androidx.annotation.NonNull;

public class googlePlaySplashAd extends TPSplashAdapter {
    private static final long EXPIRED_TIME = 4;
    private long loadTime = 0;
    public static final String TAG = "AdmobSplash";
    private AppOpenAd mAppOpenAd;
    private String mAdUnitId;
    private AdRequest request;
    private Integer mVideoMute = 0;
    private int mOrientation = AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT;
    private String id;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (tpParams != null && tpParams.size() > 0) {
            mAdUnitId = (String) tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            id = tpParams.get(GoogleConstant.ID);
        }


        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.ADMOB_DIRECTION)) {
                mOrientation = (Integer) userParams.get(AppKeyManager.ADMOB_DIRECTION);
            }

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
                requestSplash(context);
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

    private void requestSplash(Context context) {
        if (mOrientation == 2) {
            mOrientation = AppOpenAd.APP_OPEN_AD_ORIENTATION_LANDSCAPE;
        }
        Log.i(TAG, "Orientation: " + mOrientation );

        AppOpenAd.load(context, mAdUnitId, request, mOrientation, appOpenAdLoadCallback);
    }

    private final AppOpenAd.AppOpenAdLoadCallback appOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {

        @Override
        public void onAdLoaded(AppOpenAd appOpenAd) {
            Log.i(TAG, "onAppOpenAdLoaded: ");
            if (appOpenAd != null) {
                loadTime = System.currentTimeMillis();
                mAppOpenAd = appOpenAd;

                mAppOpenAd .setOnPaidEventListener(new OnPaidEventListener() {
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
                    setNetworkObjectAd(appOpenAd);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }

            }

        }

        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError) {
            Log.i(TAG, "onAppOpenAdFailedToLoad message: " + loadAdError.getMessage() + ":code:" + loadAdError.getCode());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(String.valueOf(loadAdError.getCode()));
            tpError.setErrorMessage(loadAdError.getMessage());
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }

    };

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mAppOpenAd != null) {
            Log.i(TAG, "showAd: ");
            mAppOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            mAppOpenAd.show(activity);
        } else {
            if (mShowListener != null)
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
        }
    }


    final FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdDismissedFullScreenContent() {
            Log.i(TAG, "onAdDismissedFullScreenContent: ");
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            Log.i(TAG, "onAdFailedToShowFullScreenContent msg : " + adError.getMessage() + ":code:" + adError.getCode());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(String.valueOf(adError.getCode()));
            tpError.setErrorMessage(adError.getMessage());
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }

        @Override
        public void onAdShowedFullScreenContent() {
            Log.i(TAG, "onAdShowedFullScreenContent: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }
    };

    /**
     * @param numHours Expired time  is four hours.
     * @return
     */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = System.currentTimeMillis() - loadTime;
        return dateDifference < (numHours * 3600000);
    }


    @Override
    public void clean() {
        if (mAppOpenAd != null) {
            mAppOpenAd.setFullScreenContentCallback(null);
            mAppOpenAd = null;
        }
    }

    @Override
    public boolean isReady() {
        return true;
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


}
