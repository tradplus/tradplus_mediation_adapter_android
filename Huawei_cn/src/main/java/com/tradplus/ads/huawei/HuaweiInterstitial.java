package com.tradplus.ads.huawei;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.InterstitialAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class HuaweiInterstitial extends TPInterstitialAdapter {


    private String mPlacementId;
    private InterstitialAd interstitialAd;
    private HuaweiInterstitialCallbackRouter mCallbackRouter;
    private static final String TAG = "HuaweiCnInterstitial";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        mCallbackRouter = HuaweiInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);

        HuaweiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestInterstital(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });


    }

    private void requestInterstital(Context context) {
        if (interstitialAd == null)
            interstitialAd = new InterstitialAd(context);

        // "testb4znbuh3n2"为测试专用的广告位ID，App正式发布时需要改为正式的广告位ID
        interstitialAd.setAdId(mPlacementId);
        interstitialAd.setAdListener(adListener);
        interstitialAd.loadAd(new AdParam.Builder().build());
    }

    private AdListener adListener = new AdListener() {
        @Override
        public void onAdLoaded() {
            // 广告获取成功时调用
            Log.i(TAG, "onAdLoaded: ");
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(interstitialAd);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onAdFailed(int errorCode) {
            // 广告获取失败时调用
            Log.i(TAG, "onAdFailed: errorCode : " + errorCode);
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(errorCode + "");
            if (mCallbackRouter.getListener(mPlacementId) != null)
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
        }

        @Override
        public void onAdClosed() {
            // 广告关闭时调用
            Log.i(TAG, "onAdClosed: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
        }

        @Override
        public void onAdClicked() {
            // 广告点击时调用
            Log.i(TAG, "onAdClicked: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
        }

        @Override
        public void onAdLeave() {
            // 广告离开时调用
            Log.i(TAG, "onAdLeave: ");
        }

        @Override
        public void onAdOpened() {
            // 广告打开时调用
            Log.i(TAG, "onAdOpened: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
        }

        @Override
        public void onAdImpression() {
            // 广告曝光时调用
            Log.i(TAG, "onAdImpression: ");

        }
    };

    private void suportGDPR(Map<String, Object> userParams) {

    }

    @Override
    public void showAd() {
        if (mShowListener != null)
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);


        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        // 显示广告
        if (interstitialAd != null && interstitialAd.isLoaded()) {
            interstitialAd.show(activity);
        } else {
            if (mCallbackRouter.getShowListener(mPlacementId) != null)
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    @Override
    public boolean isReady() {
        if (interstitialAd != null) {
            return interstitialAd.isLoaded() && !isAdsTimeOut();
        }
        return false;
    }

    @Override
    public void clean() {
        super.clean();
        if (interstitialAd != null) {
            interstitialAd.setAdListener(null);
            interstitialAd = null;
        }
        
        if (mCallbackRouter.getShowListener(mPlacementId) != null)
            mCallbackRouter.removeListeners(mPlacementId);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_HUAWEI);
    }

    @Override
    public String getNetworkVersion() {
        return HwAds.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

}
