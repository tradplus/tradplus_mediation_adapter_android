package com.tradplus.ads.smaato;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.smaato.sdk.core.SmaatoSdk;
import com.smaato.sdk.interstitial.EventListener;
import com.smaato.sdk.interstitial.Interstitial;
import com.smaato.sdk.interstitial.InterstitialAd;
import com.smaato.sdk.interstitial.InterstitialError;
import com.smaato.sdk.interstitial.InterstitialRequestError;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class SmaatoInterstitial extends TPInterstitialAdapter {


    private SmaatoInterstitialCallbackRouter mSmattoICBr;
    private InterstitialAd loaded;
    private String mPlacementId;
    private static final String TAG = "SmaatoInterstitial";


    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        mSmattoICBr = SmaatoInterstitialCallbackRouter.getInstance();
        mSmattoICBr.addListener(mPlacementId, mLoadAdapterListener);

        SmaatoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(context);
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

    private void requestInterstitial(Context context) {
        // load interstitial ad
        Interstitial.loadAd(mPlacementId, new EventListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdLoaded: ");
                if (mSmattoICBr.getListener(mPlacementId) != null) {
                    setNetworkObjectAd(interstitialAd);
                    mSmattoICBr.getListener(mPlacementId).loadAdapterLoaded(null);
                    loaded = interstitialAd;
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull InterstitialRequestError interstitialRequestError) {
                Log.i(TAG, "onAdFailedToLoad: ");
                if (mSmattoICBr.getListener(mPlacementId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    if (interstitialRequestError != null) {
                        InterstitialError interstitialError = interstitialRequestError.getInterstitialError();
                        if (interstitialError != null && interstitialError.name() != null) {
                            tpError.setErrorMessage(interstitialError.name());
                        }
                    }
                    mSmattoICBr.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdError(@NonNull InterstitialAd interstitialAd, @NonNull InterstitialError interstitialError) {
                Log.i(TAG, "onAdError: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    TPError tpError = new TPError(SHOW_FAILED);
                    if (interstitialError != null) {
                        String name = interstitialError.name();
                        if (name != null) {
                            tpError.setErrorMessage(name);
                        }
                    }
                    mSmattoICBr.getShowListener(mPlacementId).onAdVideoError(tpError);
                }
            }

            @Override
            public void onAdOpened(@NonNull InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdOpened: ");
            }

            @Override
            public void onAdClosed(@NonNull InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdClosed: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    mSmattoICBr.getShowListener(mPlacementId).onAdClosed();
                }
            }

            @Override
            public void onAdClicked(@NonNull InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdClicked: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    mSmattoICBr.getShowListener(mPlacementId).onAdVideoClicked();
                }
            }

            @Override
            public void onAdImpression(@NonNull InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdImpression: ");
                if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                    mSmattoICBr.getShowListener(mPlacementId).onAdShown();
                }
            }

            @Override
            public void onAdTTLExpired(@NonNull InterstitialAd interstitialAd) {
                Log.i(TAG, "onAdTTLExpired: ");

            }
        });
    }


    @Override
    public void showAd() {
        if (mSmattoICBr != null && mShowListener != null) {
            mSmattoICBr.addShowListener(mPlacementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        Log.i(TAG, "showInterstitial: " + loaded.isAvailableForPresentation());

        if (loaded == null) {
            if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                mSmattoICBr.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
            return;
        }

        if (loaded.isAvailableForPresentation()) {
            loaded.showAd(activity);
        } else {
            if (mSmattoICBr.getShowListener(mPlacementId) != null) {
                mSmattoICBr.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        if (loaded != null) {
            boolean availableForPresentation = loaded.isAvailableForPresentation();
            Log.i(TAG, "isReadyInterstitial: " + availableForPresentation);
            return availableForPresentation && !isAdsTimeOut();
        } else {
            return false;
        }
    }

    @Override
    public void clean() {
        if (mPlacementId != null) {
            mSmattoICBr.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> tpParams) {
        final String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
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
