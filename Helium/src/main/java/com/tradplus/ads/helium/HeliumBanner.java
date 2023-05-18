package com.tradplus.ads.helium;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chartboost.heliumsdk.HeliumSdk;
import com.chartboost.heliumsdk.ad.HeliumBannerAd;
import com.chartboost.heliumsdk.ad.HeliumBannerAdListener;
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.HashMap;
import java.util.Map;

public class HeliumBanner extends TPBannerAdapter {

    private String mPlacementId;
    private String mAdSize = BANNER;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private HeliumBannerAd bannerAd;
    private OnC2STokenListener onC2STokenListener;
    private TPBannerAdImpl mTpBannerAd;
    private static final String TAG = "HeliumBanner";

    // loadAndShow
    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && onC2STokenListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.containsKey(AppKeyManager.ADSIZE + mPlacementId)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + mPlacementId);
            }
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

        HeliumInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
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

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }

            }
        });
    }

    private void requestBanner(Context context) {
        if (isC2SBidding && isBiddingLoaded && bannerAd != null) {
            mTpBannerAd = new TPBannerAdImpl(null, bannerAd);
            mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            return;
        }

        // clear loaded ads on existing placements
        if (bannerAd != null) {
            bannerAd.clearAd();
        }

        bannerAd = new HeliumBannerAd(context, mPlacementId, calculateAdSize(mAdSize), new HeliumBannerAdListener() {
            @Override
            public void onAdImpressionRecorded(@NonNull String placementId) {
                Log.i(TAG, "onAdImpressionRecorded: ");
                if (mTpBannerAd != null) {
                    mTpBannerAd.adShown();
                }
            }

            @Override
            public void onAdClicked(@NonNull String placementId) {
                Log.i(TAG, "onAdClicked: ");
                if (mTpBannerAd != null) {
                    mTpBannerAd.adClicked();
                }
            }

            @Override
            public void onAdCached(@NonNull String placementId,
                                   @NonNull String loadId,
                                   @NonNull Map<String, String> winningBidInfo,
                                   @Nullable ChartboostMediationAdException error) {
                if (error != null) {
                    Log.i(TAG, "onAdCached Failed: ");
                    String message = error.getMessage();
                    if (isC2SBidding) {
                        if (onC2STokenListener != null) {
                            onC2STokenListener.onC2SBiddingFailed("",message);
                        }
                        return;
                    }

                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorMessage(message);
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                Log.i(TAG, "onAdCached: ");
                String price = winningBidInfo.get("price");
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        Log.i(TAG, "Banner bid price: " + price);
                        if (TextUtils.isEmpty(price)) {
                            onC2STokenListener.onC2SBiddingFailed(""," price is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(Double.parseDouble(price));
                    }
                    isBiddingLoaded = true;
                }
            }
        });

        bannerAd.load();

    }

    @Override
    public void clean() {
        if (bannerAd != null) {
            bannerAd.destroy();
            bannerAd = null;
        }
    }

    private HeliumBannerAd.HeliumBannerSize calculateAdSize(String adSize) {
        if (adSize.equals(BANNER)) {
            return HeliumBannerAd.HeliumBannerSize.STANDARD; //320 * 50
        } else if (adSize.equals(LARGEBANNER)) {
            return HeliumBannerAd.HeliumBannerSize.MEDIUM; //300 * 250
        } else if (adSize.equals(MEDIUMRECTANGLE)) {
            return HeliumBannerAd.HeliumBannerSize.LEADERBOARD; //728 * 90
        }
        return HeliumBannerAd.HeliumBannerSize.STANDARD;
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
