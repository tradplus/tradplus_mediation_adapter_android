package com.tradplus.ads.google;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AdManagerSplash extends TPSplashAdapter {

    public static final String TAG = "GAMSplash";
    private String placementId;
    private AdManagerAdRequest request;
    private AppOpenAd mAppOpenAd;
    private Integer mVideoMute = 0;
    private int mOrientation = AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT;
    private String id;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            id = tpParams.get(GoogleConstant.ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.ADMOB_DIRECTION)) {
                mOrientation = (Integer) userParams.get(AppKeyManager.ADMOB_DIRECTION);
            }

            if (userParams.containsKey(GoogleConstant.VIDEO_ADMOB_MUTE)) {
                mVideoMute = (int) userParams.get(GoogleConstant.VIDEO_ADMOB_MUTE);
            }
        }

        request = AdManagerInit.getInstance().getAdmobAdRequest(userParams,null,null);

        AdManagerInit.getInstance().initSDK(context, request, userParams,tpParams, new TPInitMediation.InitCallback() {
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
        Log.i(TAG, "Orientation: " + mOrientation);
        AppOpenAd.load(context, placementId, request, mOrientation, mAppOpenAdLoadCallback);

    }

    private final AppOpenAd.AppOpenAdLoadCallback mAppOpenAdLoadCallback = new AppOpenAd.AppOpenAdLoadCallback() {

        @Override
        public void onAdLoaded(AppOpenAd ad) {
            Log.i(TAG, "onAdLoaded: ");
            mAppOpenAd = ad;
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(ad);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdFailedToLoad(LoadAdError loadAdError) {
            Log.i(TAG, "onAdFailedToLoad: code: " + loadAdError.getCode() + " ,msg:" + loadAdError.getMessage());
            mAppOpenAd = null;
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(loadAdError.getMessage());
            tpError.setErrorCode(loadAdError.getCode() + "");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
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
        if (mAppOpenAd == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.UNSPECIFIED));
            }
            return;
        }

        mAppOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
        mAppOpenAd.show(activity);
    }

    private final FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdDismissedFullScreenContent() {
            // Set the reference to null so isAdAvailable() returns false.
            Log.i(TAG, "onAdDismissedFullScreenContent");
            mAppOpenAd = null;
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }


        }

        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            Log.i(TAG, "onAdFailedToShowFullScreenContent: code:" + adError.getCode() + ", msg:" + adError.getMessage());
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage(adError.getMessage());
            tpError.setErrorCode(adError.getCode() + "");
            if (mShowListener != null) {
                mShowListener.onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdShowedFullScreenContent() {
            Log.i(TAG, "onAdShowedFullScreenContent");
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
