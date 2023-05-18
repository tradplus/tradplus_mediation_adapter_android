package com.tradplus.ads.baidu;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.ExpressResponse;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

public class BaiduNative extends TPNativeAdapter {

    private String mPlacementId, template;
    private int mIsTemplateRending;
    private BaiduNativeManager mBaiduNativeManager;
    private BaiduNativeAd mBaiduNativeAd;
    private boolean mNeedDownloadImg = false;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private NativeResponse nativeResponse;
    private ExpressResponse mExpressResponse;//模版
    private static final String TAG = "BaiduNative";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
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
//        mAppId = "e866cfb0";
//        mPlacementId = "2058628";
//        mIsTemplateRending = 2;

        BaiduInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestNative();
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });


    }

    private void requestNative() {
        Activity mActivity = GlobalTradPlus.getInstance().getActivity();
        if (mActivity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }

            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_ACTIVITY_ERROR);
                }
            }
            return;
        }
        // C2S loaded成功
        C2SBiddingLoaded(mActivity);

        mBaiduNativeManager = new BaiduNativeManager(mActivity, mPlacementId);
        RequestParameters requestParameters = new RequestParameters.Builder()
                // 用户点击下载类广告时，是否弹出提示框让用户选择下载与否
                .downloadAppConfirmPolicy(RequestParameters.DOWNLOAD_APP_CONFIRM_ALWAYS)
                .build();

        if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            Log.i(TAG, "智能优选");
            mBaiduNativeManager.loadExpressAd(requestParameters, mExpressAdListener);
        } else {
            Log.i(TAG, "自渲染");
            mBaiduNativeManager.loadFeedAd(requestParameters, mFeedAdListener);
        }
    }

    private void C2SBiddingLoaded(Activity activity) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
                Log.i(TAG, "ExpressResponse: 智能优选");
                mBaiduNativeAd = new BaiduNativeAd(mExpressResponse, activity);
                mBaiduNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_EXPRESS);
                mExpressResponse.setInteractionListener(mListener);
                mExpressResponse.render();
            } else {
                Log.i(TAG, "nativeResponse: 自渲染");
                mBaiduNativeAd = new BaiduNativeAd(nativeResponse, activity);
                mBaiduNativeAd.setRenderType(TPBaseAd.AD_TYPE_NORMAL_NATIVE);
                downloadAndCallback(mBaiduNativeAd, mNeedDownloadImg);
            }
        }
    }

    // 百度自渲染
    private final BaiduNativeManager.FeedAdListener mFeedAdListener = new BaiduNativeManager.FeedAdListener() {
        @Override
        public void onNativeLoad(List<NativeResponse> nativeResponses) {
            Activity mActivity = GlobalTradPlus.getInstance().getActivity();
            if (mActivity == null) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
                }

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        onC2STokenListener.onC2SBiddingFailed("",ADAPTER_ACTIVITY_ERROR);
                    }
                }

                return;
            }
            Log.i(TAG, "onNativeLoad: 自渲染");
            // 一个广告只允许展现一次，多次展现、点击只会计入一次
            if (nativeResponses != null && nativeResponses.size() > 0) {
                nativeResponse = nativeResponses.get(0);
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        String ecpmLevel = nativeResponse.getECPMLevel();
                        Log.i(TAG, "bid price: " + ecpmLevel);
                        if (TextUtils.isEmpty(ecpmLevel)) {
                            onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is Empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(Double.parseDouble(ecpmLevel));
                    }
                    isBiddingLoaded = true;
                }else {
                    mBaiduNativeAd = new BaiduNativeAd(nativeResponse, mActivity);
                    mBaiduNativeAd.setRenderType(TPBaseAd.AD_TYPE_NORMAL_NATIVE);

                    downloadAndCallback(mBaiduNativeAd, mNeedDownloadImg);
                }
            }
        }

        @Override
        public void onNativeFail(int code, String msg) {
            Log.i(TAG, "onNativeFail:  errorCode:" + code + ", errorMsg:" + msg);
            loadFailed(code, msg);
        }

        @Override
        public void onNoAd(int errorCode, String message) {
            Log.i(TAG, "onNoAd: errorCode:" + errorCode + ", errorMsg:" + message);
            loadFailed(errorCode, message);
        }

        @Override
        public void onVideoDownloadSuccess() {
            Log.i(TAG, "onVideoDownloadSuccess: ");

        }

        @Override
        public void onVideoDownloadFailed() {
            Log.i(TAG, "onVideoDownloadFailed: ");
        }

        @Override
        public void onLpClosed() {
            // lp页面被关闭，并不是真正的Closed，落地页被关闭会有回调
            Log.i(TAG, "onLpClosed: ");
//            if(mBaiduNativeAd != null){
//                mBaiduNativeAd.close();
//            }
        }
    };

    // 模版监听回调
    private final BaiduNativeManager.ExpressAdListener mExpressAdListener = new BaiduNativeManager.ExpressAdListener() {
        @Override
        public void onNativeLoad(List<ExpressResponse> list) {
            Activity mActivity = GlobalTradPlus.getInstance().getActivity();
            if (mActivity == null) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
                }

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        onC2STokenListener.onC2SBiddingFailed("",ADAPTER_ACTIVITY_ERROR);
                    }
                }
                return;
            }

            // 一个广告只允许展现一次，多次展现、点击只会计入一次
            if (list != null && list.size() > 0) {
                mExpressResponse = list.get(0);
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        String ecpmLevel = mExpressResponse.getECPMLevel();
                        Log.i(TAG, "bid price: " + ecpmLevel);
                        if (TextUtils.isEmpty(ecpmLevel)) {
                            onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(Double.parseDouble(ecpmLevel));
                    }
                    isBiddingLoaded = true;
                }else {
                    mBaiduNativeAd = new BaiduNativeAd(mExpressResponse, mActivity);
                    mBaiduNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_EXPRESS);
                    mExpressResponse.setInteractionListener(mListener);
                    mExpressResponse.render();
                }
            } else {
                loadFailed(0, "ExpressResponse返回list为空");
            }
        }

        @Override
        public void onNativeFail(int code, String msg) {
            Log.i(TAG, "onNativeFail:  errorCode:" + code + ", errorMsg:" + msg);
            loadFailed(code, msg);
        }

        @Override
        public void onNoAd(int code, String msg) {
            Log.i(TAG, "onNoAd:  errorCode:" + code + ", errorMsg:" + msg);
            loadFailed(code, msg);
        }

        @Override
        public void onVideoDownloadSuccess() {

        }

        @Override
        public void onVideoDownloadFailed() {

        }

        @Override
        public void onLpClosed() {

        }
    };

    public void loadFailed(int code, String msg) {
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                onC2STokenListener.onC2SBiddingFailed(code+"",msg);
            }
            return;
        }

        if (mLoadAdapterListener != null) {
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(code + "");
            tpError.setErrorMessage(msg);
            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
        }
    }

    // Baidu模版
    ExpressResponse.ExpressInteractionListener mListener =
            new ExpressResponse.ExpressInteractionListener() {
        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mBaiduNativeAd != null) {
                mBaiduNativeAd.onAdClicked();
            }
        }

        @Override
        public void onAdExposed() {
            Log.i(TAG, "onAdExposed: " + mBaiduNativeAd);
            if (mBaiduNativeAd != null) {
                mBaiduNativeAd.onAdShown();
            }
        }

        @Override
        public void onAdRenderFail(View view, String s, int i) {
            loadFailed(i, "onAdRenderFail," +s);
        }

        @Override
        public void onAdRenderSuccess(View view, float v, float v1) {
            Log.i(TAG, "onAdRenderSuccess: ");
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mBaiduNativeAd);
            }
        }

        @Override
        public void onAdUnionClick() {

        }
    };

    @Override
    public void clean() {
        if (mExpressResponse != null) {
            mExpressResponse.setInteractionListener(null);
            mExpressResponse.setAdPrivacyListener(null);
            mExpressResponse.setAdDislikeListener(null);
            mExpressResponse = null;
        }

        if (nativeResponse != null) {
            nativeResponse.setAdPrivacyListener(null);
            nativeResponse = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_BAIDU);
    }

    @Override
    public String getNetworkVersion() {
        return AdSettings.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }


}
