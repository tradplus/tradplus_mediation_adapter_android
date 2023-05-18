package com.tradplus.joomob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.RelativeLayout;

import com.joomob.sdk.common.ads.AdError;
import com.joomob.sdk.common.ads.JMADManager;
import com.joomob.sdk.common.ads.JmAdSlot;
import com.joomob.sdk.common.ads.listener.JmBannerListener;
import com.joomob.sdk.common.proxy.IBannerAd;
import com.joomob.sdk.common.proxy.JMAD;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.util.DeviceUtils;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TPContextUtils;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class JoomobBanner extends TPBannerAdapter {
    public static final String TAG = "JoomobBanner";
    private String mAppId;
    private String mSlotId;
    private RelativeLayout mContainer;
    private IBannerAd mIBannerAd;
    private TPBannerAdImpl mTPBannerAdImpl;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Log.i(TAG, "loadAdView: ");
        if (mLoadAdapterListener == null) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            mAppId = serverExtras.get(AppKeyManager.APP_ID);
            mSlotId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        }
//        mAppId = "2001920";
//        mSlotId = "3001785436";
        if (!AppKeyManager.getInstance().isInited(mAppId, AppKeyManager.AdType.SHARE)) {
             JoomobInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
                 @Override
                 public void onSuccess() {
                     Log.i(TAG, "onSuccess: ");
                     requestBanner(context);
                 }

                 @Override
                 public void onFailed(String code, String msg) {

                 }
             });
        }else {
            requestBanner(context);
        }

    }

    private void requestBanner(Context context) {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        final JMAD jmad = JMADManager.getInstance().create();
        final int width = DeviceUtils.dip2px(context,AppKeyManager.NATIVE_DEFAULT_WIDTH);
        final int height = DeviceUtils.dip2px(context,AppKeyManager.BANNER_DEFAULT_HEIGHT);
        JmAdSlot adSlot = new JmAdSlot.Builder().setSlotId(mSlotId).setWidth(width).setHeight(height).setBannerCycleTime(30).build();
        mContainer = new RelativeLayout(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        mContainer.setLayoutParams(layoutParams);
        jmad.loadBannerAd(activity, mContainer, adSlot, jmBannerListener);
    }

    private JmBannerListener jmBannerListener = new JmBannerListener() {
        @Override
        public void onDisplayAd() {
            Log.i(TAG, "onDisplayAd: ");
            if (mTPBannerAdImpl != null) {
                mTPBannerAdImpl.adShown();
            }
        }

        @Override
        public void onClickAd() {
            Log.i(TAG, "onClickAd: ");
            if (mTPBannerAdImpl != null) {
                mTPBannerAdImpl.adClicked();
            }
        }

        @Override
        public void onCloseAd() {
            Log.i(TAG, "onCloseAd: ");
        }

        @Override
        public void onAdError(AdError adError) {
            Log.i(TAG, "onAdError msg: " + adError.getErrorMsg() + ":code:" + adError.getErrorCode());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(adError.getErrorCode()+"");
                tpError.setErrorMessage(adError.getErrorMsg());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onRequestSuccess(IBannerAd iBannerAd) {
            Log.i(TAG, "onRequestSuccess: ");
            mIBannerAd = iBannerAd;
            mTPBannerAdImpl = new TPBannerAdImpl(iBannerAd, mContainer);
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mTPBannerAdImpl);
            }
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        if (mIBannerAd != null) {
            mIBannerAd.destroy();
        }
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
