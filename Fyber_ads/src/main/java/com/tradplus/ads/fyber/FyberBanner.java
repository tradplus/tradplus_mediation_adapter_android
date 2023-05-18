package com.tradplus.ads.fyber;

import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import com.fyber.inneractive.sdk.external.ImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerWithImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class FyberBanner extends TPBannerAdapter {

    private String placementId;
    private InneractiveAdSpot mSpot;
    private static final String TAG = "FyberBanner";
    private InneractiveAdViewUnitController controllerSpot;
    private TPBannerAdImpl mTPBannerAd;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (serverExtrasAreValid(serverExtras)) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }


        FyberInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "initSDK onSuccess: ");
                requestBanner(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                Log.i(TAG, "initSDK onFailed: msg :" + msg);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });


    }

    private void requestBanner(Context context) {
        mSpot = InneractiveAdSpotManager.get().createSpot();
        // adding the adview controller
        InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        mSpot.addUnitController(controller);

        InneractiveAdRequest adRequest = new InneractiveAdRequest(placementId);

        mSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot inneractiveAdSpot) {
                if (context == null || mSpot == null || mSpot.getSelectedUnitController() == null || !mSpot.isReady()) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(SHOW_FAILED));
                    return;
                }

                controllerSpot = (InneractiveAdViewUnitController) mSpot.getSelectedUnitController();
                controllerSpot.setEventsListener(mListenerWithImpressionData);
                //getting the spot's controller
                FrameLayout container = new FrameLayout(context);
                Log.i(TAG, "onInneractiveSuccessfulAdRequest: ");
                mTPBannerAd = new TPBannerAdImpl(inneractiveAdSpot, container);
                //showing the ad
                controllerSpot.bindView(container);
                mLoadAdapterListener.loadAdapterLoaded(mTPBannerAd);

            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot inneractiveAdSpot, InneractiveErrorCode inneractiveErrorCode) {
                Log.i(TAG, "Failed loading Square! with error: ");
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (inneractiveErrorCode != null && inneractiveErrorCode.name() != null) {
                    tpError.setErrorCode(inneractiveErrorCode + "");
                    tpError.setErrorMessage(inneractiveErrorCode.name());
                }

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

        //when ready to perform the ad request
        mSpot.requestAd(adRequest);
    }

    private final InneractiveAdViewEventsListenerWithImpressionData mListenerWithImpressionData = new InneractiveAdViewEventsListenerWithImpressionData() {

        @Override
        public void onAdImpression(InneractiveAdSpot inneractiveAdSpot, ImpressionData impressionData) {
            Log.i(TAG, "onAdImpression: ");
        }

        @Override
        public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdImpression: spotId : ");
            if (mTPBannerAd != null) {
                mTPBannerAd.adShown();
            }
        }

        @Override
        public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdClicked: spotId : ");
            if (mTPBannerAd != null) {
                mTPBannerAd.adClicked();
            }
        }

        @Override
        public void onAdWillCloseInternalBrowser(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdWillCloseInternalBrowser: ");
        }

        @Override
        public void onAdWillOpenExternalApp(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdWillOpenExternalApp: ");
        }

        @Override
        public void onAdEnteredErrorState(InneractiveAdSpot inneractiveAdSpot, InneractiveUnitController.AdDisplayError adDisplayError) {
            Log.i(TAG, "onAdEnteredErrorState: ");
        }

        @Override
        public void onAdExpanded(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdExpanded: ");
        }

        @Override
        public void onAdResized(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdResized: ");
        }

        @Override
        public void onAdCollapsed(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdCollapsed: ");
        }

    };

    @Override
    public void clean() {
        if (mSpot != null) {
            mSpot.setRequestListener(null);
            mSpot.destroy();
            mSpot = null;
        }
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    private boolean localExtrasAreValid(final Map<String, Object> localExtras) {
        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_FYBER);
    }

    @Override
    public String getNetworkVersion() {
        return InneractiveAdManager.getVersion();
    }
}
