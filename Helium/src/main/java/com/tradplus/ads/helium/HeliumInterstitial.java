package com.tradplus.ads.helium;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.heliumsdk.HeliumSdk;
import com.chartboost.heliumsdk.ad.HeliumFullscreenAdListener;
import com.chartboost.heliumsdk.ad.HeliumInterstitialAd;
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.HashMap;
import java.util.Map;

public class HeliumInterstitial extends TPInterstitialAdapter {

    private String mPlacementId;
    private HeliumInterstitialAd mHeliumInterstitialAd;
    private HeliumInterstitialCallbackRouter mCallbackRouter;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "HeliumInterstitial";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null && onC2STokenListener == null) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("", ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }


        mCallbackRouter = HeliumInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);
        mLoadAdapterListener = mCallbackRouter.getListener(mPlacementId);


        HeliumInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
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

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }

            }
        });

    }

    private void requestInterstitial(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mHeliumInterstitialAd != null && mCallbackRouter != null && mLoadAdapterListener != null) {
                setNetworkObjectAd(mHeliumInterstitialAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            return;
        }

        if (mHeliumInterstitialAd != null) {
            mHeliumInterstitialAd.clearLoaded();
        }

        mHeliumInterstitialAd = new HeliumInterstitialAd(context, mPlacementId, new HeliumFullscreenAdListener() {
            @Override
            public void onAdCached(@NonNull String placementId,
                                   @NonNull String loadId,
                                   @NonNull Map<String, String> winningBidInfo,
                                   @Nullable ChartboostMediationAdException error) {
                if (error != null) {
                    String message = error.getMessage();
                    if (isC2SBidding) {
                        if (onC2STokenListener != null) {
                            onC2STokenListener.onC2SBiddingFailed("", message);
                        }
                        return;
                    }

                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(message);
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                String price = winningBidInfo.get("price");
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        if (TextUtils.isEmpty(price)) {
                            onC2STokenListener.onC2SBiddingFailed("", "price is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(Double.parseDouble(price));
                    }
                    isBiddingLoaded = true;
                }

            }

            @Override
            public void onAdShown(@NonNull String placementId, @Nullable ChartboostMediationAdException heliumAdError) {
                if (heliumAdError != null) {
                    String message = heliumAdError.getMessage();
                    TPError tpError = new TPError(SHOW_FAILED);
                    tpError.setErrorMessage(message);
                    if (mShowListener != null) {
                        mShowListener.onAdVideoError(tpError);
                    }
                    Log.i(TAG, "onAdShown Failed: " + message);
                    return;
                }

                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }

            }

            @Override
            public void onAdClicked(@NonNull String placementId) {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onAdClosed(@NonNull String placementId, @Nullable ChartboostMediationAdException heliumAdError) {
                if (heliumAdError != null) {
                    String message = heliumAdError.getMessage();
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(message);
                    if (mShowListener != null) {
                        mShowListener.onAdVideoError(tpError);
                    }
                    Log.i(TAG, "onAdClosed Failed: " + message);
                    return;
                }

                Log.i(TAG, "onAdClosed: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }

            }

            @Override
            public void onAdRewarded(@NonNull String s) {

            }

            @Override
            public void onAdImpressionRecorded(@NonNull String s) {

            }
        });

        mHeliumInterstitialAd.load();
    }

    @Override
    public void showAd() {
        Log.i(TAG, "showInterstitial: ");
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
            mShowListener = mCallbackRouter.getShowListener(mPlacementId);
        }

        if (mHeliumInterstitialAd == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
            }
            return;
        }

        if (mHeliumInterstitialAd.readyToShow()) {
            mHeliumInterstitialAd.show();
        } else {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(NETWORK_NO_FILL));
            }
        }

    }

    @Override
    public boolean isReady() {
        return mHeliumInterstitialAd != null && mHeliumInterstitialAd.readyToShow() && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        if (mHeliumInterstitialAd != null) {
            mHeliumInterstitialAd.destroy();
            mHeliumInterstitialAd = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return "Helium";
    }

    @Override
    public String getNetworkVersion() {
        return HeliumSdk.getVersion();
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }
}
