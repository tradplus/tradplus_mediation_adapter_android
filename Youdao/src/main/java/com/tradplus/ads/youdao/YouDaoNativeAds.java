package com.tradplus.ads.youdao;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.youdao.sdk.common.YouDaoAd;
import com.youdao.sdk.nativeads.NativeErrorCode;
import com.youdao.sdk.nativeads.NativeResponse;
import com.youdao.sdk.nativeads.YouDaoNative;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.INIT_FAILED;

public class YouDaoNativeAds extends TPNativeAdapter {
    private static final String TAG = "YouDaoNative";
    private YouDaoNative youDaoNative;
    private String placementId;
    private YouDaoNativeData mYouDaoNativeData;
    private boolean mNeedDownloadImg = false;

    @Override
    public void loadCustomAd(final Context context,
                             Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            if (serverExtras.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
                placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            }
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) localExtras.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }


        YouDaoInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestNative(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                Log.i(TAG, "onFailed: ");
                TPError tpError = new TPError(INIT_FAILED);
                tpError.setErrorCode(code);
                tpError.setErrorMessage(msg);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);

            }
        });

    }

    private void requestNative(Context context) {
        youDaoNative = new YouDaoNative(context, placementId, youDaoNativeNetworkListener);
        //设置不采用默认浏览器打开广告落地页，全局设置，所有广告都生效
        YouDaoAd.getYouDaoOptions().setSdkBrowserOpenLandpageEnabled(false);
        YouDaoAd.getNativeDownloadOptions().setConfirmDialogEnabled(true);
        youDaoNative.setNativeEventListener(youDaoNativeEventListener);
        //定位相关的信息没有添加
        youDaoNative.makeRequest();
    }

    final YouDaoNative.YouDaoNativeEventListener youDaoNativeEventListener = new YouDaoNative.YouDaoNativeEventListener() {
        @Override
        public void onNativeImpression(View view, NativeResponse nativeResponse) {
            Log.i(TAG, "onNativeImpression: ");
            if (mYouDaoNativeData != null) {
                mYouDaoNativeData.adShown();
            }
        }

        @Override
        public void onNativeClick(View view, NativeResponse nativeResponse) {
            Log.i(TAG, "onNativeClick: ");
            if (mYouDaoNativeData != null) {
                mYouDaoNativeData.adClicked();
            }
        }
    };

    YouDaoNative.YouDaoNativeNetworkListener youDaoNativeNetworkListener = new YouDaoNative.YouDaoNativeNetworkListener() {
        @Override
        public void onNativeLoad(final NativeResponse nativeResponse) {
            Log.i(TAG, "onNativeLoad: ");
            mYouDaoNativeData = new YouDaoNativeData(nativeResponse);
            //"showConfirmDialog"为服务器配置的是否弹窗的参数，其取值为"00"，"01"，"11"，其中"00"代表不弹窗，"01"代表在非wifi时弹窗，"11"代表在任何网络状态下都弹窗；
            //在自定义弹窗时，开发者可使用该参数控制什么时候是否展示弹窗
            String showConfirmDialog = nativeResponse.getShowConfirmDialog();
            Log.i(TAG, "showConfirmDialog : "+ showConfirmDialog);

            downloadAndCallback(mYouDaoNativeData, mNeedDownloadImg);
        }

        @Override
        public void onNativeFail(NativeErrorCode nativeErrorCode) {
            Log.i(TAG, "onNativeFail code: " + nativeErrorCode.getCode() + ":msg：" + nativeErrorCode.toString());
            Log.d("TradPlus", "YouDaoNative onNativeFail , " + "onNativeFail code: " + nativeErrorCode.getCode() + ":msg：" + nativeErrorCode.toString());
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(YoudaoErrorUtil.getTradPlusErrorCode(nativeErrorCode));
            }
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        if (youDaoNative != null) {
            youDaoNative.setNativeEventListener(null);
            youDaoNative.destroy();
            youDaoNative = null;
        }

    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_YOUDAO);
    }

    @Override
    public String getNetworkVersion() {
        return "4.2.16";
    }
}
