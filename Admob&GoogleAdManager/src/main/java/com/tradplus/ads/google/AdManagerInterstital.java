package com.tradplus.ads.google;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class AdManagerInterstital extends TPInterstitialAdapter {

    private String placementId;
    private AdManagerAdRequest request;
    private AdManagerInterstitialAd mAdManagerInterstitialAd;
    private Integer mVideoMute = 0;
    private String id;
    private static final String TAG = "GAMInterstital";

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
            if (userParams.containsKey(GoogleConstant.VIDEO_ADMOB_MUTE)) {
                mVideoMute = (int) userParams.get(GoogleConstant.VIDEO_ADMOB_MUTE);
            }
        }


        request = AdManagerInit.getInstance().getAdmobAdRequest(userParams,null,null);

        AdManagerInit.getInstance().initSDK(context, request, userParams, tpParams, new TPInitMediation.InitCallback() {
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
        AdManagerInterstitialAd.load(context, placementId, request, new AdManagerInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AdManagerInterstitialAd interstitialAd) {
                // The mAdManagerInterstitialAd reference will be null until
                // an ad is loaded.
                Log.i(TAG, "onAdLoaded");
                mAdManagerInterstitialAd = interstitialAd;
                setFirstLoadedTime();
                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(interstitialAd);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                Log.i(TAG, "onAdFailedToLoad: code: " + loadAdError.getCode() + " ,msg:" + loadAdError.getMessage());
                mAdManagerInterstitialAd = null;
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(loadAdError.getMessage());
                tpError.setErrorCode(loadAdError.getCode() + "");
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

    }

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mAdManagerInterstitialAd == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.UNSPECIFIED));
            }
            return;
        }

        mAdManagerInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when fullscreen content is dismissed.
                Log.i(TAG, "onAdDismissedFullScreenContent: ");
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
                mAdManagerInterstitialAd = null;
                Log.i(TAG, "onAdShowedFullScreenContent: ");
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

        mAdManagerInterstitialAd.show(activity);

    }

    @Override
    public void clean() {
        if (mAdManagerInterstitialAd != null) {
            mAdManagerInterstitialAd.setFullScreenContentCallback(null);
            mAdManagerInterstitialAd = null;
        }
    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
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
