package com.tradplus.ads.chartboostx;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.callbacks.BannerCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class ChartboostTPBanner extends TPBannerAdapter {

    private static final String TAG = "ChartboostBanner";
    private boolean onAdCached = false;
    private Banner mChartboosBanner;
    private TPBannerAdImpl mTPBannerAd;
    private String location;
    private String mAdSize = BANNER;

    @Override
    public void loadCustomAd(final Context context, final Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }
        if (tpParams != null && tpParams.size() > 0) {
            location = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            Log.i(TAG, "adsize:" + tpParams.get(AppKeyManager.ADSIZE + location));
            if (tpParams.containsKey(AppKeyManager.ADSIZE + location)) {
                mAdSize = tpParams.get(AppKeyManager.ADSIZE + location);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        TPInitMediation.InitCallback initCallback = new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                Log.i(TAG, "initSDK onFailed: msg :" + msg);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code + "");
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        };

        CBInitManager.getInstance().initSDK(context, userParams, tpParams, initCallback);
    }

    private void requestBanner(Context context) {
        if (mChartboosBanner != null) {
            mChartboosBanner.clearCache();
        }

        mChartboosBanner = new Banner(context, location, calculateAdSize(mAdSize), new BannerCallback() {
            @Override
            public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
                if (cacheError == null) {
                    Log.i(TAG, "onAdCached: ");
                    mTPBannerAd = new TPBannerAdImpl(null, mChartboosBanner);
                    onAdCached = true;
                    mLoadAdapterListener.loadAdapterLoaded(mTPBannerAd);
                } else {
                    if (mLoadAdapterListener != null) {
                        Log.i(TAG, "CacheError: " + cacheError.getCode());
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(cacheError.getCode()+"");
                        tpError.setErrorMessage(cacheError.toString());
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                }
            }

            @Override
            public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {

            }

            @Override
            public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
                if (showError != null) {
                    Log.i(TAG, "ShowError: ");
                    if (mLoadAdapterListener != null) {
                        Log.i(TAG, "ChartboostShowError: " + showError.toString());
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(showError.getCode()+"");
                        tpError.setErrorMessage(showError.toString());
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                }
            }

            @Override
            public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
                Log.i(TAG, "onAdClicked: ");
                if (mTPBannerAd != null) {
                    mTPBannerAd.adClicked();
                }
            }

            @Override
            public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
                Log.i(TAG, "onImpressionRecorded: ");
                if (mTPBannerAd != null) {
                    mTPBannerAd.adShown();
                }
            }
        }, null);


        mChartboosBanner.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        mChartboosBanner.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                Log.i(TAG, "onViewAttachedToWindow: ");
                if (onAdCached && mChartboosBanner != null) {
                    mChartboosBanner.show();
                }else {
                    if (mTPBannerAd != null) {
                        mTPBannerAd.onAdShowFailed(new TPError(UNSPECIFIED));
                    }
                }
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                Log.i(TAG, "onViewDetachedFromWindow: ");
            }
        });
        mChartboosBanner.cache();


    }

    @Override
    public void clean() {
        if (mChartboosBanner != null) {
            mChartboosBanner.clearCache();
            mChartboosBanner = null;
        }
    }

    private Banner.BannerSize calculateAdSize(String adSize) {
        if (BANNER.equals(adSize)) { // 1
            return Banner.BannerSize.STANDARD; //320 * 50
        } else if (LARGEBANNER.equals(adSize)) { // 2
            return Banner.BannerSize.MEDIUM; //300, 250
        } else if (MEDIUMRECTANGLE.equals(adSize)) { // 3
            return Banner.BannerSize.LEADERBOARD; // 728, 90
        }
        return Banner.BannerSize.STANDARD;
    }


    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_CHARTBOOST);
    }

    @Override
    public String getNetworkVersion() {
        return Chartboost.getSDKVersion();
    }
}
