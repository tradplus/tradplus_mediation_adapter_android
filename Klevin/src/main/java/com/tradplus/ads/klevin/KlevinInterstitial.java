package com.tradplus.ads.klevin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.klevin.KlevinManager;
import com.tencent.klevin.ads.ad.InterstitialAd;
import com.tencent.klevin.ads.ad.InterstitialAdRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class KlevinInterstitial extends TPInterstitialAdapter {


    private String placementId;
    private int mPostId;
    private InterstitialAd mInterstitialAd;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private int ecpmLevel;
    private KlevinInterstitialCallbackRouter mCallbackRouter;
    public static final String TAG = "KlevinInterstitial";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

            if (!TextUtils.isEmpty(placementId)) {
                mPostId = Integer.parseInt(placementId);
            }

        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }



        mCallbackRouter = KlevinInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        KlevinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial();
            }

            @Override
            public void onFailed(String code, String msg) {
                TPError tpError = new TPError(INIT_FAILED);
                tpError.setErrorCode(code);
                tpError.setErrorMessage(msg);

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });
       
    }

    private void requestInterstitial() {
        if (isC2SBidding && isBiddingLoaded) {
            if (mInterstitialAd != null && mCallbackRouter != null &&
                    mCallbackRouter.getListener(placementId) != null) {
                setNetworkObjectAd(mInterstitialAd);
                mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
            }
            return;
        }

        InterstitialAdRequest.Builder interstitialBuilder = new InterstitialAdRequest.Builder();
        interstitialBuilder.setAdCount(1)
                .setPosId(mPostId);

        InterstitialAd.load(interstitialBuilder.build(), new InterstitialAd.InterstitialAdLoadListener() {
            @Override
            public void onAdLoadError(int err, String msg) {
                Log.i(TAG, "ad load err: " + err + " " + msg);

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        onC2STokenListener.onC2SBiddingFailed(err+"",msg);
                    }
                    return;
                }

                if (mCallbackRouter.getListener(placementId) != null)
                    mCallbackRouter.getListener(placementId)
                            .loadAdapterLoadFailed(KlevinErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, err, msg));

            }

            public void onAdLoaded(InterstitialAd ad) {
                Log.i(TAG, "interstitial ad loaded");
                mInterstitialAd = ad;

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        ecpmLevel = mInterstitialAd.getECPM();
                        Log.i(TAG, " bid price: " + ecpmLevel);
                        if (TextUtils.isEmpty(ecpmLevel +"")) {
                            onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                    }
                    isBiddingLoaded = true;
                    return;
                }

                if (mCallbackRouter.getListener(placementId) != null) {
                    setNetworkObjectAd(ad);
                    mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
                }
            }
        });
    }

    @Override
    public void showAd() {
        if (mShowListener != null && mCallbackRouter != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        if (mInterstitialAd != null && mInterstitialAd.isValid()) {
            mInterstitialAd.setListener(new InterstitialAd.InterstitialAdListener() {
                public void onAdShow() {
                    Log.i(TAG, "onAdShow");
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId).onAdShown();
                }

                public void onAdClick() {
                    Log.i(TAG, "onAdClick");
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId).onAdVideoClicked();
                }

                public void onAdClosed() {
                    Log.i(TAG, "onAdClosed");
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId).onAdClosed();
                }

                public void onAdError(int err, String msg) {
                    Log.i(TAG, "onAdError err: " + err + " " + msg);
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId)
                                .onAdVideoError(KlevinErrorUtil.getTradPlusErrorCode(SHOW_FAILED, err, msg));
                }

                @Override
                public void onAdDetailClosed(int i) {

                }
            });
            if (ecpmLevel > 0) {
                Log.i(TAG, "sendWinNotificationWithPrice: " +ecpmLevel);
                mInterstitialAd.sendWinNotificationWithPrice(ecpmLevel);
            }
            mInterstitialAd.show();
        } else {
            if (mCallbackRouter.getShowListener(placementId) != null)
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(UNSPECIFIED));
        }
    }

    @Override
    public boolean isReady() {
        return mInterstitialAd != null && mInterstitialAd.isValid() && !isAdsTimeOut();
    }

    @Override
    public void clean() {
        super.clean();
        if (mInterstitialAd != null) {
            mInterstitialAd.setListener(null);
            mInterstitialAd = null;
        }

        if (mCallbackRouter != null)
            mCallbackRouter.removeListeners(placementId);

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KLEVIN);
    }

    @Override
    public String getNetworkVersion() {
        return KlevinManager.getVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        TPTaskManager.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                loadCustomAd(context, localParams, tpParams);
            }
        });
    }


}
