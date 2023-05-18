package com.tradplus.ads.fyber;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fyber.inneractive.sdk.external.ImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullScreenAdRewardedListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerWithImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.fyber.inneractive.sdk.external.VideoContentListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class FyberInterstitialVideo extends TPRewardAdapter {

    private String mPlacementId;
    private FyberInterstitialCallbackRouter mCallbackRouter;
    private InneractiveAdSpot mSpot;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "FyberInterstitialVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {

        if (mLoadAdapterListener == null) {
            return;
        }

        if (extrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }
        mCallbackRouter = FyberInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        FyberInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestRewardVideo();
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

    private void requestRewardVideo() {
        // spot integration for display Square
        mSpot = InneractiveAdSpotManager.get().createSpot();

        // adding the adview controller
        InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        mSpot.addUnitController(controller);

        InneractiveAdRequest adRequest = new InneractiveAdRequest(mPlacementId);

        //when ready to perform the ad request
        mSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot inneractiveAdSpot) {
                Log.i(TAG, "onInneractiveSuccessfulAdRequest: ");
                if (inneractiveAdSpot == null) return;
                String requestedSpotId = inneractiveAdSpot.getRequestedSpotId();

                if (requestedSpotId == null) return;
                TPLoadAdapterListener tploadListener = mCallbackRouter.getListener(requestedSpotId);

                if (tploadListener != null) {
                    setNetworkObjectAd(inneractiveAdSpot);
                    tploadListener.loadAdapterLoaded(null);
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot inneractiveAdSpot, InneractiveErrorCode inneractiveErrorCode) {
                Log.i(TAG, "onInneractiveFailedAdRequest: ");
                if (inneractiveAdSpot == null) return;
                String requestedSpotId = inneractiveAdSpot.getRequestedSpotId();

                if (requestedSpotId == null) return;
                TPLoadAdapterListener tploadListener = mCallbackRouter.getListener(requestedSpotId);

                if (tploadListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    if (inneractiveErrorCode != null && inneractiveErrorCode.name() != null) {
                        tpError.setErrorCode(inneractiveErrorCode + "");
                        tpError.setErrorMessage(inneractiveErrorCode.name());
                    }

                    tploadListener.loadAdapterLoadFailed(tpError);
                }
            }
        });

        mSpot.requestAd(adRequest);

    }

    @Override
    public void showAd() {
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                TPError tpError = new TPError(TPError.ADAPTER_ACTIVITY_ERROR);
                tpError.setErrorMessage("Context is not Acvitiy context");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        if (mSpot == null || !mSpot.isReady()) {
            mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }

        InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
        fullscreenUnitController.setEventsListener(mListenerWithImpressionData);

        mSpot.addUnitController(fullscreenUnitController);
        InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();
        // full screen video ad callbacks
        videoContentController.setEventsListener(mVideoContentListener);

        if (mSpot.getSelectedUnitController() == null) {
            mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }

        // Now add the content controller to the unit controller
        InneractiveFullscreenUnitController controller = (InneractiveFullscreenUnitController) mSpot.getSelectedUnitController();
        controller.addContentController(videoContentController);

        controller.setRewardedListener(new InneractiveFullScreenAdRewardedListener() {
            @Override
            public void onAdRewarded(InneractiveAdSpot inneractiveAdSpot) {
                Log.i(TAG, "onAdRewarded: ");
                hasGrantedReward = true;
            }
        });

        //showing the ad using the Activity's context
        controller.show(activity);


    }

    private final InneractiveFullscreenAdEventsListenerWithImpressionData mListenerWithImpressionData = new InneractiveFullscreenAdEventsListenerWithImpressionData() {
        @Override
        public void onAdImpression(InneractiveAdSpot inneractiveAdSpot, ImpressionData impressionData) {
            Log.i(TAG, "onAdImpression: ");
            if (inneractiveAdSpot == null) return;
            String requestedSpotId = inneractiveAdSpot.getRequestedSpotId();

            if (requestedSpotId == null) return;
            TPShowAdapterListener showListener = mCallbackRouter.getShowListener(requestedSpotId);

            if (showListener != null) {
                showListener.onAdShown();
                showListener.onAdVideoStart();
            }
        }

        @Override
        public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
        }

        @Override
        public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdClicked: ");
            if (inneractiveAdSpot == null) return;
            String requestedSpotId = inneractiveAdSpot.getRequestedSpotId();

            if (requestedSpotId == null) return;
            TPShowAdapterListener showListener = mCallbackRouter.getShowListener(requestedSpotId);

            if (showListener != null) {
                showListener.onAdVideoClicked();
            }
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
        public void onAdWillCloseInternalBrowser(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdWillCloseInternalBrowser: ");
        }

        @Override
        public void onAdDismissed(InneractiveAdSpot inneractiveAdSpot) {
            Log.i(TAG, "onAdDismissed: ");
            if (inneractiveAdSpot == null) return;
            String requestedSpotId = inneractiveAdSpot.getRequestedSpotId();

            if (requestedSpotId == null) return;
            TPShowAdapterListener showListener = mCallbackRouter.getShowListener(requestedSpotId);

            if (showListener != null) {
                if (hasGrantedReward || alwaysRewardUser) {
                    showListener.onReward();
                }
                showListener.onAdClosed();
            }
        }

    };

    private final VideoContentListener mVideoContentListener = new VideoContentListener() {

        @Override
        public void onProgress(int totalDurationInMsec, int positionInMsec) {
            Log.i(TAG, "onProgress: positionInMsec  :  " + positionInMsec);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "onCompleted: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
        }

        @Override
        public void onPlayerError() {
            /**
             * Please note that onPlayerError callback method is deprecated starting from VAMP v7.3.0,
             and won't be trigged when an error is occurred.
             * Note: The SDK handles such errors internally and no further action is required.
             */
            Log.i(TAG, "onPlayerError: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
        }
    };

    @Override
    public boolean isReady() {
        if (mSpot != null) {
            return mSpot.isReady();
        } else {
            return false;
        }
    }

    @Override
    public void clean() {
        if (mSpot != null) {
            mSpot.setRequestListener(null);
            mSpot.destroy();
            mSpot = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
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
