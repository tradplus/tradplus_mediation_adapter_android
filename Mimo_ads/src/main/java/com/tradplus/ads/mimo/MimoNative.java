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
    private int mIsTemplateRending = AppKeyManager.TEMPLATE_RENDERING_NO; // 默认自渲染
    private MimoNativeAd mMimoNativeAd; //自渲染
    private TemplateAd mTemplateAd; //模版
    private FeedAd mFeedAd;//原生视频
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
//        placementId = "ffc009779b4a62177fffe3d594bb35ff";//原生模板-横版视频模板
//        placementId = "702b6a3b2f67a52efd3bdbf51fbef5fe";//自渲染大图（仅图片）
//        placementId = "737fd8fce83832ffac1da2244d24add5";//自渲染大图（仅视频）
//        placementId = "270c1630710a858d633aaf752025eae2";//自渲染——大图（图片+视频混出）
//        placementId = "4966931579570a31c70269f560e9577e";//原生模板-上文下图
//        placementId = "e8cad3a962d8f5ccb3e42a5c2427107d";//原生模板-左文右图
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
            Log.i(TAG, "loadCustomAd: 模版图片，placementId == " + placementId);
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
            Log.i(TAG, "loadCustomAd: 模版视频，placementId == " + placementId);
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
                    //资源缓存成功
                    mMimoNativeAd = new MimoNativeAd(mFeedAd, activity, TPBaseAd.AD_TYPE_NATIVE_EXPRESS);

                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mMimoNativeAd);
                    }
                }

                @Override
                public void onAdRequestSuccess() {
                    //网络请求成功
                }

                @Override
                public void onAdLoadFailed(int errorCode, String errorMessage) {
                    //广告加载失败
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
            Log.i(TAG, "loadCustomAd: 自渲染，placementId == " + placementId);
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
//        if (mNativeAd != null) {
//            mNativeAd.destroy();
//            mNativeAd = null;
//        }
//
//        if (mFeedAd != null) {
//            mFeedAd.destroy();
//            mFeedAd = null;
//        }
//
//        if (mTemplateAd != null) {
//            mTemplateAd.destroy();
//            mTemplateAd = null;
//        }

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
