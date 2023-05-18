package com.tradplus.ads.bigo;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.annotation.NonNull;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdInteractionListener;
import sg.bigo.ads.api.AdLoadListener;
import sg.bigo.ads.api.InterstitialAd;
import sg.bigo.ads.api.InterstitialAdLoader;
import sg.bigo.ads.api.InterstitialAdRequest;

public class BigoInterstitial extends TPInterstitialAdapter {

    private static final String TAG = "BigoInterstitial";
    private String mPlacementId;
    private String serverBiddingAdm;
    private InterstitialAd mInterstitial;
    private InterstitialCallbackRouter mCallbackRouter;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

            String payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(payload)) {
                serverBiddingAdm = payload;
            }
        }

        mCallbackRouter = InterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);
        mLoadAdapterListener = mCallbackRouter.getListener(mPlacementId);

        BigoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                request();
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

    private void request() {
        InterstitialAdRequest.Builder builder = new InterstitialAdRequest.Builder().withSlotId(mPlacementId);

        InterstitialAdLoader interstitialAdLoader = new InterstitialAdLoader.Builder().withAdLoadListener(new AdLoadListener<InterstitialAd>() {
            @Override
            public void onError(@NonNull AdError error) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                if (error != null) {
                    int code = error.getCode();
                    String message = error.getMessage();
                    tpError.setErrorMessage(message);
                    tpError.setErrorCode(code + "");
                    Log.i(TAG, "code :" + code + ", message :" + message);
                }

                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                mInterstitial = ad;
                ad.setAdInteractionListener(adInteractionListener);
                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(ad);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }
        }).build();

        if (!TextUtils.isEmpty(serverBiddingAdm)) {
            builder.withBid(serverBiddingAdm);
        }

        interstitialAdLoader.loadAd(builder.build());
    }

    private final AdInteractionListener adInteractionListener = new AdInteractionListener() {
        @Override
        public void onAdError(@NonNull AdError error) {
            TPError tpError = new TPError(SHOW_FAILED);
            if (error != null) {
                int code = error.getCode();
                String message = error.getMessage();
                tpError.setErrorMessage(message);
                tpError.setErrorCode(code + "");
                Log.i(TAG, "code :" + code + ", message :" + message);
            }

            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdImpression() {
            // When the ad appears on the screen.
            Log.i(TAG, "onAdImpression: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClicked();
            }
        }

        @Override
        public void onAdOpened() {
            Log.i(TAG, "onAdOpened: ");
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }
        }
    };

    @Override
    public void showAd() {
        if (mShowListener == null) return;
        mCallbackRouter.addShowListener(mPlacementId, mShowListener);

        TPError tpErrorShow = new TPError(SHOW_FAILED);
        if (mInterstitial == null) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                tpErrorShow.setErrorMessage("showFailed: InterstitialAd == null");
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpErrorShow);
            }
            return;
        }

        if (mInterstitial.isExpired()) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                tpErrorShow.setErrorMessage("Ad is Expired");
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(tpErrorShow);
            }
            return;
        }

        mInterstitial.show();
    }

    @Override
    public boolean isReady() {
        return mInterstitial != null && !mInterstitial.isExpired();
    }

    @Override
    public void clean() {
        if (mInterstitial != null) {
            mInterstitial.setAdInteractionListener(null);
            mInterstitial.destroy();
            mInterstitial = null;
        }

        if (mPlacementId != null) {
            mCallbackRouter.removeListeners(mPlacementId);
        }
    }

    @Override
    public String getNetworkName() {
        return "Bigo";
    }

    @Override
    public String getNetworkVersion() {
        return BigoAdSdk.getSDKVersionName();
    }


    @Override
    public void getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        // token在SDK未初始化或者异常情况下可能为空，开发者需要进⾏空值校验
        // 成功初始化BigoAds SDK后 获取token
        boolean initialized = BigoAdSdk.isInitialized();
        BigoInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                String bidderToken = BigoAdSdk.getBidderToken();
                boolean tokenEmpty = TextUtils.isEmpty(bidderToken);
                Log.i(TAG, "onSuccess bidderToken isEmpty " + tokenEmpty);
                if (!initialized) {
                    // 第一次初始化打印250
                    BigoInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                }
                onS2STokenListener.onTokenResult(!tokenEmpty ? bidderToken : "", null);
            }

            @Override
            public void onFailed(String code, String msg) {
                BigoInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                onS2STokenListener.onTokenResult("", null);
            }
        });
    }
}
