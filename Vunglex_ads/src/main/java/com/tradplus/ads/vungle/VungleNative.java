package com.tradplus.ads.vungle;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.vungle.warren.Vungle.canPlayAd;


public class VungleNative extends TPNativeAdapter {

    private VungleTpNativeAd mVungleTpNativeAd;
    private String placementId, payload, appId;
    private BannerAdConfig mBannerAdConfig;
    private VungleBanner mVungleBanner;
    private static final String TAG = "VungleNative";
    private Map<String, Object> userParams;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }
        this.userParams = userParams;

        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        VungleInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Banners.loadBanner(placementId, isEmptyPayLoad(), getAdConfig(), vungleLoadAdCallback);
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

    private BannerAdConfig getAdConfig() {
        mBannerAdConfig = new BannerAdConfig();
        mBannerAdConfig.setAdSize(AdConfig.AdSize.VUNGLE_MREC);
        mBannerAdConfig.setMuted(true);
        return mBannerAdConfig;
    }

    private String isEmptyPayLoad() {
        return TextUtils.isEmpty(payload) ? null : payload;
    }

    private final LoadAdCallback vungleLoadAdCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(String placementReferenceId) {

            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }


            if (Banners.canPlayAd(placementId, isEmptyPayLoad(), AdConfig.AdSize.VUNGLE_MREC)) {
                mVungleBanner = Banners.getBanner(placementId, isEmptyPayLoad(), getAdConfig(), vunglePlayAdCallback);
                if (mVungleBanner != null) {
                    Log.i(TAG, "onAdLoad:");
                    mVungleTpNativeAd = new VungleTpNativeAd(context, mVungleBanner);
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mVungleTpNativeAd);
                    }
                    return;
                }

                if (mLoadAdapterListener != null) {
                    Log.i(TAG, "onAdLoad, but Banners.getBanner return null");
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage("onAdLoad, but Banners.getBanner return null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;

            }

            if (mLoadAdapterListener != null) {
                Log.i(TAG, "onAdLoad, but Banners can't PlayAd");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage("onAdLoad ,but Banners can't PlayAd");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

        }

        @Override
        public void onError(String placementReferenceId, VungleException exception) {
            if (exception != null) {
                Log.i(TAG, "InitCallback - onError: " + exception.getLocalizedMessage());
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(VungleErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, exception));
            } else {
                Log.i(TAG, "Throwable is null");
            }
        }
    };

    private final PlayAdCallback vunglePlayAdCallback = new PlayAdCallback() {
        @Override
        public void creativeId(String s) {

        }

        @Override
        public void onAdStart(String placementReferenceId) {
            // Invoked when the Vungle SDK has successfully launched the advertisement and an advertisement will begin playing momentarily.
            Log.i(TAG, "onAdStart: ");


        }

        @Override
        public void onAdEnd(String s, boolean b, boolean b1) {
            // 已经废弃
        }

        @Override
        public void onAdEnd(String id) {
            Log.i(TAG, "onAdEnd: ");
            if (mVungleTpNativeAd != null) {
                mVungleTpNativeAd.clean();
            }

        }

        @Override
        public void onAdClick(String id) {
            Log.i(TAG, "onAdClick: ");
            if (mVungleTpNativeAd != null) {
                mVungleTpNativeAd.onAdClicked();
            }
        }

        @Override
        public void onAdRewarded(String id) {
            Log.i(TAG, "onAdRewarded: ");
        }

        @Override
        public void onAdLeftApplication(String id) {
            Log.i(TAG, "onAdLeftApplication: ");
        }

        @Override
        public void onError(String placementReferenceId, VungleException exception) {
            // Ad failed to play
            Log.i(TAG, "onError ,placementReferenceID : " + exception + " , errormessage :" + exception.getLocalizedMessage());
            if (mVungleTpNativeAd != null)
                mVungleTpNativeAd.onAdVideoError(VungleErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, exception));


        }

        @Override
        public void onAdViewed(String id) {
            //Invoked when the ad is first rendered on device. Please use this callback to track impressions.
            Log.i(TAG, "onAdViewed: ");
            if (mVungleTpNativeAd != null) {
                mVungleTpNativeAd.onAdShown();
            }
        }
    };

    @Override
    public void clean() {
        if (mVungleBanner != null) {
            Log.i(TAG, "clean: ");
            mVungleBanner.destroyAd();
        }
    }

    private boolean extrasAreValid(final Map<String, String> tpParams) {
        return tpParams.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_VUNGLE);
    }

    @Override
    public String getNetworkVersion() {
        return com.vungle.warren.BuildConfig.VERSION_NAME;
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams) {
        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return "";
        }

        if (GlobalTradPlus.getInstance().getContext() != null && !TextUtils.isEmpty(appId)) {
            VungleInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailed(String code, String msg) {

                }
            });
            return Vungle.getAvailableBidTokens(GlobalTradPlus.getInstance().getContext(), placementId,100);
        }
        return "";
    }
}
