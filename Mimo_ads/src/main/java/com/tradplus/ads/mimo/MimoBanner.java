package com.tradplus.ads.mimo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import com.miui.zeus.mimo.sdk.BannerAd;
import com.miui.zeus.mimo.sdk.BuildConfig;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MimoBanner extends TPBannerAdapter {

    private BannerAd mBannerAd;
    private String placementId;
    private TPBannerAdImpl mTpBannerAd;
    private FrameLayout mFrameLayout;
    private float bannerViewScale = 1.0F; // 支持等比例缩放。取值（0，1] 默认1
    private static final String TAG = "MimoBanner";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }
//        placementId = "802e356f1726f9ff39c69308bfd6f06a";

        if (userParams != null && userParams.size() >0) {
            if (userParams.containsKey(MimoConstant.MIMO_BANNER_SCALE)) {
                String banneSize = (String)userParams.get(MimoConstant.MIMO_BANNER_SCALE);
                if (Float.parseFloat(banneSize) > 0 && Float.parseFloat(banneSize) <= 1) {
                    bannerViewScale = Float.parseFloat(banneSize);
                }
            }
        }

        MimoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner();
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

    private void requestBanner() {
        Activity mActivity = GlobalTradPlus.getInstance().getActivity();
        if (mActivity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        mBannerAd = new BannerAd();
        mBannerAd.loadAd(placementId, new BannerAd.BannerLoadListener() {
            @Override
            public void onBannerAdLoadSuccess() {
                if (mLoadAdapterListener != null && mAdContainerView != null) {
                    Log.i(TAG, "onBannerAdLoadSuccess: ");
                    mFrameLayout = new FrameLayout(mActivity);
                    mTpBannerAd = new TPBannerAdImpl(mBannerAd, mFrameLayout);
                    mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
                    mBannerAd.showAd(mActivity, mFrameLayout, bannerViewScale ,mInteractionListener);
                }
            }

            @Override
            public void onAdLoadFailed(int errorCode, String errorMsg) {
                Log.i(TAG, "onAdLoadFailed: errorCode :" + errorCode + ", errorMsg :" + errorMsg);
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(errorCode + "");
                    tpError.setErrorMessage(errorMsg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });
    }

    private final BannerAd.BannerInteractionListener mInteractionListener = new BannerAd.BannerInteractionListener() {
        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }
        }

        @Override
        public void onAdShow() {
            Log.i(TAG, "onAdShow: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adShown();
            }
        }

        @Override
        public void onAdDismiss() {
            Log.i(TAG, "onAdDismiss: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClosed();
            }
        }

        @Override
        public void onRenderSuccess() {
            Log.i(TAG, "onRenderSuccess: ");
        }

        @Override
        public void onRenderFail(int errorCode, String errorMsg) {
            Log.i(TAG, "onRenderFail: errorCode :" + errorCode + ", errorMsg :" + errorMsg);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(errorCode + "");
                tpError.setErrorMessage(errorMsg);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }
    };

    @Override
    public void clean() {
        if (mBannerAd != null) {
            mBannerAd.destroy();
            mFrameLayout.removeAllViews();
            mBannerAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MIMO);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
