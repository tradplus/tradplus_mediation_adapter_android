package com.tradplus.ads.mimo;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.miui.zeus.mimo.sdk.BuildConfig;
import com.miui.zeus.mimo.sdk.FeedAd;
import com.miui.zeus.mimo.sdk.NativeAd;
import com.miui.zeus.mimo.sdk.NativeAdData;
import com.miui.zeus.mimo.sdk.TemplateAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MimoNative extends TPNativeAdapter {

    private NativeAd mNativeAd;
    private String placementId;
    private int mIsTemplateRending = AppKeyManager.TEMPLATE_RENDERING_NO;
    private MimoNativeAd mMimoNativeAd;
    private TemplateAd mTemplateAd;
    private FeedAd mFeedAd;
    private boolean mNeedDownloadImg = false;
    private static final String TAG = "MimoNative";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) return;

        String template;
        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) userParams.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

        MimoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative(context);
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

    private void requestNative(Context context) {
        if (mIsTemplateRending == 3) {
            Log.i(TAG, "loadCustomAd: ，placementId == " + placementId);
            mTemplateAd = new TemplateAd();
            mTemplateAd.load(placementId, new TemplateAd.TemplateAdLoadListener() {
                @Override
                public void onAdLoaded() {
                    Log.i(TAG, "onAdLoaded: ");
                    mMimoNativeAd = new MimoNativeAd(mTemplateAd, context, AppKeyManager.TEMPLATE_RENDERING_YES);

                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mMimoNativeAd);
                    }
                }

                @Override
                public void onAdLoadFailed(int code, String msg) {
                    Log.i(TAG, "onAdLoadFailed: code ==" + code + " , msg ==" + msg);
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(code + "");
                        tpError.setErrorMessage(msg);
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                }
            });
        } else if (mIsTemplateRending == 4) {
            Log.i(TAG, "loadCustomAd: ，placementId == " + placementId);
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }

            mFeedAd = new FeedAd();
            mFeedAd.load(placementId, new FeedAd.FeedLoadListener() {
                @Override
                public void onAdResourceCached() {
                    Log.i(TAG, "onAdResourceCached: ");
                    mMimoNativeAd = new MimoNativeAd(mFeedAd, activity, TPBaseAd.AD_TYPE_NATIVE_EXPRESS);

                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mMimoNativeAd);
                    }
                }

                @Override
                public void onAdRequestSuccess() {
                }

                @Override
                public void onAdLoadFailed(int errorCode, String errorMessage) {
                    Log.i(TAG, "onAdLoadFailed: code ==" + errorCode + " , msg ==" + errorMessage);
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(errorCode + "");
                        tpError.setErrorMessage(errorMessage);
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                }
            });
        } else {
            Log.i(TAG, "loadCustomAd: ，placementId == " + placementId);
            mNativeAd = new NativeAd();
            mNativeAd.load(placementId, new NativeAd.NativeAdLoadListener() {
                @Override
                public void onAdLoadSuccess(NativeAdData nativeAdData) {
                    Log.i(TAG, "onAdLoadSuccess: ");
                    mMimoNativeAd = new MimoNativeAd(mNativeAd, nativeAdData, TPBaseAd.AD_TYPE_NORMAL_NATIVE);
                    downloadAndCallback(mMimoNativeAd, mNeedDownloadImg);
                }

                @Override
                public void onAdLoadFailed(int code, String msg) {
                    Log.i(TAG, "onAdLoadFailed: code ==" + code + " , msg ==" + msg);
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(NETWORK_NO_FILL);
                        tpError.setErrorCode(code + "");
                        tpError.setErrorMessage(msg);
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }

                }
            });

        }
    }


    @Override
    public void clean() {

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
