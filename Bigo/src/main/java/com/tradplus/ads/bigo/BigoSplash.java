package com.tradplus.ads.bigo;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.annotation.NonNull;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdLoadListener;
import sg.bigo.ads.api.SplashAd;
import sg.bigo.ads.api.SplashAdInteractionListener;
import sg.bigo.ads.api.SplashAdLoader;
import sg.bigo.ads.api.SplashAdRequest;

public class BigoSplash extends TPSplashAdapter {

    private static final String TAG = "BigoSplash";
    private String mPlacementId;
    private SplashAd mSplashAd;
    private int isFlullSreenVideoAd;
    private int mAppIcon;
    private String mAppName;
    private String serverBiddingAdm;
    private int AdSkipped = 0;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            String fullSreenTpye = tpParams.get(AppKeyManager.FULL_SCREEN_TYPE);

            if (!TextUtils.isEmpty(fullSreenTpye)) {
                isFlullSreenVideoAd = Integer.parseInt(fullSreenTpye);
            }

            String payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            if (!TextUtils.isEmpty(payload)) {
                serverBiddingAdm = payload;
            }
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.APPICON)) {
                mAppIcon = (int) userParams.get(AppKeyManager.APPICON);
            }

            if (userParams.containsKey(AppKeyManager.APP_NAME)) {
                mAppName = (String) userParams.get(AppKeyManager.APP_NAME);
            }

        }

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
        SplashAdLoader splashAdLoader = new SplashAdLoader.Builder().
                withAdLoadListener(new AdLoadListener<SplashAd>() {
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
                    public void onAdLoaded(SplashAd splashAd) {
                        Log.i(TAG, "onAdLoaded: ");
                        mSplashAd = splashAd;
                        splashAd.setAdInteractionListener(adInteractionListener);
                        if (mLoadAdapterListener != null) {
                            setNetworkObjectAd(splashAd);
                            mLoadAdapterListener.loadAdapterLoaded(null);
                        }
                    }

                }).build();


        SplashAdRequest.Builder builder = new SplashAdRequest.Builder()
                .withSlotId(mPlacementId);

        if (isFlullSreenVideoAd == BigoConstant.SPLASH_HALF_FULL) {
            builder.withAppLogo(mAppIcon);
            builder.withAppName(mAppName);
        } else {
        }

        if (!TextUtils.isEmpty(serverBiddingAdm)) {
            builder.withBid(serverBiddingAdm);
        }

        splashAdLoader.loadAd(builder.build());
    }

    private final SplashAdInteractionListener adInteractionListener = new SplashAdInteractionListener() {
        @Override
        public void onAdSkipped() {
            Log.i(TAG, "onAdSkipped: ");
            if (mShowListener != null && AdSkipped == 0) {
                AdSkipped = 1;
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdFinished() {
            Log.i(TAG, "onAdFinished: ");
            if (mShowListener != null && AdSkipped == 0) {
                AdSkipped = 1;
                mShowListener.onAdClosed();
            }
        }

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

            if (mShowListener != null) {
                mShowListener.onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdImpression() {
            // When the ad appears on the screen.
            Log.i(TAG, "onAdImpression: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onAdOpened() {

        }

        @Override
        public void onAdClosed() {

        }
    };


    @Override
    public void showAd() {
        TPError tpErrorShow = new TPError(SHOW_FAILED);
        if (mAdContainerView == null || mSplashAd == null) {
            tpErrorShow.setErrorMessage("showFailed AdContainerView or SplashAd == null");
            mShowListener.onAdVideoError(tpErrorShow);
            return;
        }

        if (mSplashAd.isExpired()) {
            tpErrorShow.setErrorMessage("Ad is Expired");
            mShowListener.onAdVideoError(tpErrorShow);
            return;
        }

        mSplashAd.showInAdContainer(mAdContainerView);

    }

    @Override
    public boolean isReady() {
        return mSplashAd != null && !mSplashAd.isExpired();
    }

    @Override
    public void clean() {
        if (mSplashAd != null) {
            mSplashAd.setAdInteractionListener(null);
            mSplashAd.destroy();
            mSplashAd = null;
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
        boolean initialized = BigoAdSdk.isInitialized();
        BigoInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                String bidderToken = BigoAdSdk.getBidderToken();
                boolean tokenEmpty = TextUtils.isEmpty(bidderToken);
                Log.i(TAG, "onSuccess bidderToken isEmpty " + tokenEmpty);
                if (!initialized) {
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
