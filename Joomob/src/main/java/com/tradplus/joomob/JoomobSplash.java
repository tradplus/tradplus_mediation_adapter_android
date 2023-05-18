package com.tradplus.joomob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.joomob.sdk.common.ads.AdError;
import com.joomob.sdk.common.ads.JMADManager;
import com.joomob.sdk.common.ads.JmAdSlot;
import com.joomob.sdk.common.ads.listener.JmSplashListener;
import com.joomob.sdk.common.proxy.ISplashAd;
import com.joomob.sdk.common.proxy.JMAD;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class JoomobSplash extends TPSplashAdapter {
    public static final String TAG = "JoomobSplash";
    private String mAppId;
    private String mSlotId;
    private ISplashAd mISplashAd;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        Log.i(TAG, "loadAdView: ");

        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
            mSlotId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        }
//        mAppId = "2001920";
//        mSlotId = "3001785398";
        if (!AppKeyManager.getInstance().isInited(mAppId, AppKeyManager.AdType.SHARE)) {
            JoomobInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess: ");
                    requestSplash(context);
                }

                @Override
                public void onFailed(String code, String msg) {

                }
            });
        } else {
            requestSplash(context);
        }

    }

    private void requestSplash(Context context) {
        JmAdSlot jmAdSlot = new JmAdSlot.Builder()
                .setSlotId(mSlotId)
                .build();
        JMAD jmad = JMADManager.getInstance().create();
        if (mAdContainerView != null)
            jmad.loadSplashAd((Activity) context, jmAdSlot, mAdContainerView, jmSplashListener);
    }

    JmSplashListener jmSplashListener = new JmSplashListener()  {
        @Override
        public void onDisplayAd(ISplashAd iSplashAd) {
            Log.i(TAG, "onDisplayAd: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onClickAd() {
            Log.i(TAG, "onClickAd: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onCloseAd() {
            Log.i(TAG, "onCloseAd: ");
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdError(AdError adError) {
            Log.i(TAG, "onAdError: ");
            Log.i(TAG, "onAdError msg: " + adError.getErrorMsg() + ":code:" + adError.getErrorCode());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(adError.getErrorCode()+"");
                tpError.setErrorMessage(adError.getErrorMsg());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onRequestSuccess() {
            Log.i(TAG, "onRequestSuccess: ");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }
    };

    @Override
    public void showAd() {
        Log.i(TAG, "showAd: ");
    }

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_JULIANG);
    }

    @Override
    public String getNetworkVersion() {
        return "1.0.9.0";
    }

}
