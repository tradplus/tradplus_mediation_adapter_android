package com.tradplus.ads.vungle;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BuildConfig;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;


public class VungleInterstitial extends TPInterstitialAdapter {
    private final static String TAG = "VungleInterstitial";
    private String payload;
    private VungleInterstitialCallbackRouter mRouter;
    private String placementId, appId;
    private Map<String, Object> localExtras;
    private Integer mVideoMute = 1;
    private Integer mAdOri = 0;

    @Override
    public void loadCustomAd(final Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        this.localExtras = localExtras;

        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(AppKeyManager.APP_ID);
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);

            if (serverExtras.containsKey(AppKeyManager.DIRECTION)) {
                mAdOri =  Integer.parseInt(serverExtras.get(AppKeyManager.DIRECTION));
            }

            if (serverExtras.containsKey(AppKeyManager.VIDEO_MUTE)) {
                mVideoMute = Integer.parseInt(serverExtras.get(AppKeyManager.VIDEO_MUTE));
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(VungleConstant.VIDEO_MUTE)) {
                mVideoMute = (int) localExtras.get(VungleConstant.VIDEO_MUTE);
            }

            if (localExtras.containsKey(VungleConstant.AD_ORI)) {
                mAdOri = (int) localExtras.get(VungleConstant.AD_ORI);
            }
        }

        mRouter = VungleInterstitialCallbackRouter.getInstance();
        mRouter.addListener(placementId, mLoadAdapterListener);

        VungleInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                VungleLoadAd();
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

    private void VungleLoadAd() {
        if (TextUtils.isEmpty(payload)) {
            Vungle.loadAd(placementId, getAdConfig(), vungleLoadAdCallback);
        } else {
            Vungle.loadAd(placementId, payload, getAdConfig(), vungleLoadAdCallback);
        }
    }


    private AdConfig getAdConfig() {
        AdConfig adConfig = new AdConfig();
        adConfig.setBackButtonImmediatelyEnabled(true);
        adConfig.setAdOrientation(AdOrientation(mAdOri));
        adConfig.setMuted(mVideoMute == 1);
        adConfig.setOrdinal(1);

        return adConfig;
    }

    private int AdOrientation(int ori) {
        Log.i(TAG, "AdOrientation: " + ori);
        if (ori == 0){
            return AdConfig.AUTO_ROTATE;
        }else if (ori == 1) {
            return AdConfig.PORTRAIT;
        }else if (ori == 2) {
            return AdConfig.LANDSCAPE;
        }else if (ori == 3) {
            return AdConfig.MATCH_VIDEO;
        }
        return AdConfig.AUTO_ROTATE;
    }


    @Override
    public boolean isReady() {
        if (placementId == null) {
            return !isAdsTimeOut();
        }

        if (!TextUtils.isEmpty(payload)) {
            return !isAdsTimeOut() && Vungle.canPlayAd(placementId, payload);
        } else {
            return !isAdsTimeOut() && Vungle.canPlayAd(placementId);
        }
    }

    @Override
    public void showAd() {
        if (mRouter != null && mShowListener != null) {
            mRouter.addShowListener(placementId, mShowListener);
        }

        if (!Vungle.isInitialized()) {
            mRouter.getShowListener(placementId).onAdVideoError(new TPError(INIT_FAILED));
            return;
        }

        if (mRouter.getShowListener(placementId) == null) {
            mRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }

        if (!TextUtils.isEmpty(payload)) {
            Log.i(TAG, "showAd payload : " + payload);
            if (!Vungle.canPlayAd(placementId, payload)) {
                mRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
                return;
            }
            Vungle.playAd(placementId, payload, getAdConfig(), vunglePlayAdCallback);

        } else {
            if (!Vungle.canPlayAd(placementId)) {
                mRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
                return;
            }
            Vungle.playAd(placementId, getAdConfig(), vunglePlayAdCallback);
        }
    }


    private final LoadAdCallback vungleLoadAdCallback = new LoadAdCallback() {
        @Override
        public void onAdLoad(final String placementReferenceID) {
            setFirstLoadedTime();
            Log.i(TAG, "onAdLoad: ");
            if (mRouter.getListener(placementReferenceID) != null)
                mRouter.getListener(placementReferenceID).loadAdapterLoaded(null);
        }

        @Override
        public void onError(String id, VungleException exception) {
            Log.i(TAG, "onError: " + exception.getLocalizedMessage());
            if (mRouter.getListener(id) != null)
                mRouter.getListener(id).loadAdapterLoadFailed(VungleErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, exception));
        }

    };

    /**
     * Callback handler for playing a Vungle advertisement. This is given as a parameter to {@link Vungle#playAd(String, AdConfig, PlayAdCallback)}
     * and is triggered when the advertisement begins to play, when the advertisement ends, and when
     * any errors occur.
     */
    private final PlayAdCallback vunglePlayAdCallback = new PlayAdCallback() {
        @Override
        public void creativeId(String s) {

        }

        @Override
        public void onAdStart(final String placementReferenceID) {
            Log.i(TAG, "onAdStart: ");
            // Called before playing an ad.

        }

        @Override
        public void onAdEnd(final String placementReferenceID, final boolean completed, final boolean isCTAClicked) {
            Log.i(TAG, "onAdEnd: ");
        }

        @Override
        public void onAdEnd(String id) {
            Log.i(TAG, "onAdEnd: ");
            if (mRouter.getShowListener(id) != null) {
                mRouter.getShowListener(id).onAdClosed();
            }
        }

        @Override
        public void onAdClick(String id) {
            Log.i(TAG, "onAdClick: ");
            if (mRouter.getShowListener(id) != null) {
                mRouter.getShowListener(id).onAdClicked();
            }
        }

        @Override
        public void onAdRewarded(String id) {

        }

        @Override
        public void onAdLeftApplication(String id) {

        }

        @Override
        public void onError(String id, VungleException exception) {
            Log.i(TAG, "onError: " + exception.getLocalizedMessage());
            if (mRouter.getShowListener(id) != null)
                mRouter.getShowListener(id).onAdVideoError(VungleErrorUtil.getTradPlusErrorCode(SHOW_FAILED, exception));
        }

        @Override
        public void onAdViewed(String id) {
            Log.i(TAG, "onAdViewed: ");
            if (mRouter.getShowListener(id) != null)
                mRouter.getShowListener(id).onAdShown();
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
