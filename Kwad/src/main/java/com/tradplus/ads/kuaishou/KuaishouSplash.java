package com.tradplus.ads.kuaishou;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsSplashScreenAd;
import com.kwad.sdk.api.model.SplashAdExtraData;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.kwad_ads.KuaishouInitManager;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class KuaishouSplash extends TPSplashAdapter {

    private String placementId, mBidResponseV2;
    private KsSplashScreenAd mKsSplashScreenAd;
    private int onAdClosed = 0 ;
    private boolean mShakable = true;
    private static final String TAG = "KuaishouSplash";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mBidResponseV2 = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams.containsKey(KuaishouConstant.SHAKABLE)) {
            mShakable = (boolean) userParams.get(KuaishouConstant.SHAKABLE);
        }

        KuaishouInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                KsAdSDK.getLoadManager().loadSplashScreenAd(getKsScene(), mSplashScreenAdListener);
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

    private KsScene getKsScene() {
        KsScene.Builder builder = new KsScene.Builder(Long.parseLong(placementId));

        SplashAdExtraData extraData = new SplashAdExtraData();
        extraData.setDisableShakeStatus(mShakable);
        builder.setSplashExtraData(extraData);

        if (!TextUtils.isEmpty(mBidResponseV2)) {
            builder.setBidResponseV2(mBidResponseV2);
        }
        return builder.build();
    }

    @Override
    public void clean() {
        if (mKsSplashScreenAd != null) {
            mKsSplashScreenAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KUAISHOU);
    }

    @Override
    public String getNetworkVersion() {
        return KsAdSDK.getSDKVersion();
    }

    private final KsLoadManager.SplashScreenAdListener mSplashScreenAdListener = new KsLoadManager.SplashScreenAdListener() {

        @Override
        public void onError(int i, String s) {
            Log.i(TAG, "Kuaishou Splash ad failed to load , ErrorCode :" + i + ", ErrorMessage : " + s);
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(KuaishouErrorUtil.geTpMsg(NETWORK_NO_FILL, i, s));
        }

        @Override
        public void onRequestResult(int i) {
            Log.i(TAG, "onRequestResult: " + i);
        }

        @Override
        public void onSplashScreenAdLoad(KsSplashScreenAd ksSplashScreenAd) {
            if (mLoadAdapterListener != null) {
                Log.i(TAG, "Kuaishou Splash ad onSplashScreenAdLoad");
                setNetworkObjectAd(ksSplashScreenAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            mKsSplashScreenAd = ksSplashScreenAd;
        }
    };

    @Override
    public boolean isReady() {
        if (mKsSplashScreenAd == null) return false;
        return mKsSplashScreenAd.isAdEnable();
    }

    @Override
    public void showAd() {
        Context context = GlobalTradPlus.getInstance().getContext();
        if (context == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }
            return;
        }
        if (mKsSplashScreenAd != null) {
            View frameLayout = mKsSplashScreenAd.getView(context, new KsSplashScreenAd.SplashScreenAdInteractionListener() {
                @Override
                public void onAdClicked() {
                    Log.i(TAG, "Kusishou Splash ad onAdClicked");
                    if (mShowListener != null)
                        mShowListener.onAdClicked();
                }

                @Override
                public void onAdShowError(int i, String s) {
                    Log.i(TAG, "Kuaishou Splash ad onAdShowError, ErrorCode : " + i + ", ErrorMessage : " + s);
                    if (mShowListener != null)
                        mShowListener.onAdVideoError(KuaishouErrorUtil.geTpMsg(SHOW_FAILED, i, s));

                }

                @Override
                public void onAdShowEnd() {
                    Log.i(TAG, "Kuaishou Splash ad onAdShowEnd");
                    if (mShowListener != null && onAdClosed == 0) {
                        onAdClosed = 1;
                        mShowListener.onAdClosed();
                    }
                }

                @Override
                public void onAdShowStart() {
                    Log.d(TAG, "Kuaishou Splash ad onAdShowStart");
                    if (mShowListener != null)
                        mShowListener.onAdShown();
                }

                @Override
                public void onSkippedAd() {
                    Log.i(TAG, "Kuaishou Splash ad onSkippedAd");
                    if (mShowListener != null && onAdClosed == 0) {
                        onAdClosed = 1;
                        mShowListener.onAdClosed();
                    }
                }

                @Override
                public void onDownloadTipsDialogShow() {

                }

                @Override
                public void onDownloadTipsDialogDismiss() {

                }

                @Override
                public void onDownloadTipsDialogCancel() {

                }
            });

            if (mAdContainerView != null) {
                mAdContainerView.removeAllViews();
                mAdContainerView.addView(frameLayout);
            }
        }
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> userParams) {
        KuaishouInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "getBiddingToken onSuccess: ");

            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
        return KsAdSDK.getLoadManager().getBidRequestTokenV2(new KsScene.Builder(0).build());
    }

}

