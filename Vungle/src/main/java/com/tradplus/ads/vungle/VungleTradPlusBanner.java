package com.tradplus.ads.vungle;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.BuildConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.error.VungleException;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.BANNERZERO;
import static com.tradplus.ads.base.util.TradPlusDataConstants.LARGEBANNER;
import static com.tradplus.ads.base.util.TradPlusDataConstants.MEDIUMRECTANGLE;


public class VungleTradPlusBanner extends TPBannerAdapter {

    private String placementId, payload, appId;
    private boolean destroyed;
    private String mAdSize;
    private VungleBanner mVungleBanner;
    private TPBannerAdImpl mTpBannerAd;
    private BannerAdConfig bannerAdConfig;
    private VungleInterstitialCallbackRouter mICbR;
    private static final String TAG = "VungleBanner";
    private Map<String, Object> localExtras;

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras,
                             Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        this.localExtras = localExtras;

        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(AppKeyManager.APP_ID);
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            if (serverExtras.get(AppKeyManager.ADSIZE + placementId) != null) {
                if (BANNERZERO.equals(serverExtras.get(AppKeyManager.ADSIZE + placementId))) {
                    mAdSize = BANNERZERO;
                } else {
                    mAdSize = serverExtras.get(AppKeyManager.ADSIZE + placementId);
                }
            } else {
                mAdSize = BANNER;
            }

            Log.i(TAG, "BannerSize: " + mAdSize + ". '1' means ad size will be 320 * 50 , '2' means ad size will be 300*50 , " +
                    "'3' means ad size will be 728* 90 only for tablets,'0' means ad size will be 300 * 250");
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        mVungleBanner = VungleInitManager.getInstance().getVungleBanner(placementId);
        if(mVungleBanner != null){
            mVungleBanner.destroyAd();
            mVungleBanner = null;
            VungleInitManager.getInstance().removeVungleBanner(placementId);
        }


        mICbR = VungleInterstitialCallbackRouter.getInstance();

        VungleInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                // 下发AdSize 为 0 的情况是选择了300 * 250 中矩形
                Banners.loadBanner(placementId, isEmptyPayLoad(), getBannerAdConfig(BANNERZERO.equals(mAdSize)), vungleLoadAdCallback);
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

    private String isEmptyPayLoad() {
        return TextUtils.isEmpty(payload) ? null : payload;
    }

    private BannerAdConfig getBannerAdConfig(boolean isMrec) {
        if (bannerAdConfig == null) {
            bannerAdConfig = new BannerAdConfig();
        }
        bannerAdConfig.setAdSize(isMrec ? AdConfig.AdSize.VUNGLE_MREC : calculateAdSize(mAdSize));
        bannerAdConfig.setMuted(true);
        return bannerAdConfig;
    }

    private final LoadAdCallback vungleLoadAdCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(final String placementReferenceID) {
            VungleBannerPlayAd();
        }

        @Override
        public void onError(final String placementReferenceID, VungleException throwable) {
            Log.i(TAG, "onError ,placementReferenceID : " + placementReferenceID + " , errormessage :" + throwable.getLocalizedMessage());
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(VungleErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, throwable));
        }
    };

    @Override
    public void clean() {
        //must be called
        Log.i(TAG, "mVungleBanner: "+mVungleBanner);
        if (mVungleBanner != null) {
            Log.i(TAG, "clean: ");
            destroyed = true;
            mVungleBanner.destroyAd();
            mVungleBanner = null;
            VungleInitManager.getInstance().removeVungleBanner(placementId);
        }
    }

    private void VungleBannerPlayAd() {
        if (Banners.canPlayAd(placementId, isEmptyPayLoad(), calculateAdSize(mAdSize))) {
            mVungleBanner = Banners.getBanner(placementId, isEmptyPayLoad(), getBannerAdConfig((BANNERZERO.equals(mAdSize))), vunglebannerPlayAdCallback);
            VungleInitManager.getInstance().setVungleBanner(placementId,mVungleBanner);
            // VungleBanner 可能为null
            if (mVungleBanner == null) {
                Log.i(TAG, "onAdLoad, but Banners.getBanner return null");
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage("onAdLoad, but Banners.getBanner return null");
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(SHOW_FAILED));
                }
                return;
            }

            if (BANNERZERO.equals(mAdSize)) {
                setDefaultAdViewSize(320, 250);
            }

            mTpBannerAd = new TPBannerAdImpl(null, mVungleBanner);

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }

            return;
        }

        //Banner can not PlayAd
        if (mLoadAdapterListener != null) {
            Log.i(TAG, "onAdLoad, but Banners can't PlayAd ");
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorMessage("onAdLoad，but Banners can't PlayAd");
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }
    }


    private final PlayAdCallback vunglebannerPlayAdCallback = new PlayAdCallback() {
        @Override
        public void creativeId(String s) {

        }

        @Override
        public void onAdStart(String s) {
            Log.i(TAG, "Vungle Banner onAdStart ,placementReferenceID : " + s);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mVungleBanner != null) {
                        mVungleBanner.setVisibility(View.GONE);
                        mVungleBanner.setAdVisibility(false);
                        mVungleBanner.setVisibility(View.VISIBLE);
                        mVungleBanner.setAdVisibility(true);
                    }else {
                       return;
                    }
                }
            }, 1000);

            if (mTpBannerAd != null)
                mTpBannerAd.adShown();

        }

        @Override
        public void onAdEnd(String id, boolean completed, boolean isCTAClicked) {
        }


        @Override
        public void onAdEnd(String id) {
        }

        @Override
        public void onAdClick(String id) {
            Log.d("TradPlus", "Vungle Banner isCTAClicked ,placementReferenceID : " + id);
            if (mTpBannerAd != null)
                mTpBannerAd.adClicked();
        }

        @Override
        public void onAdRewarded(String id) {
        }

        @Override
        public void onAdLeftApplication(String id) {
            Log.i(TAG, "Vungle Banner onAdLeftApplication ,placementReferenceID : " + id);
        }

        @Override
        public void onError(String s, VungleException e) {
        }

        @Override
        public void onAdViewed(String id) {
        }
    };


    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_VUNGLE);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private AdConfig.AdSize calculateAdSize(String adSize) {
        Log.i(TAG, "calculateAdSize: " + adSize);
        if (adSize.equals(BANNER)) {
            return AdConfig.AdSize.BANNER; // 320 * 50
        } else if (adSize.equals(LARGEBANNER)) {
            return AdConfig.AdSize.BANNER_SHORT; // 300* 50
        } else if (adSize.equals(MEDIUMRECTANGLE)) {
            return AdConfig.AdSize.BANNER_LEADERBOARD; // 728 * 90
        } else if (adSize.equals(BANNERZERO)) {
            return AdConfig.AdSize.VUNGLE_MREC; //300 * 250
        }
        return AdConfig.AdSize.BANNER;
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
            VungleInitManager.getInstance().initSDK(context, localExtras, tpParams, new TPInitMediation.InitCallback() {
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
