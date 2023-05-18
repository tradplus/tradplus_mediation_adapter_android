package com.tradplus.ads.klevin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.tencent.klevin.KlevinManager;
import com.tencent.klevin.ads.ad.SplashAd;
import com.tencent.klevin.ads.ad.SplashAdRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class KlevinSplash extends TPSplashAdapter {

    private int mPostId;
    private SplashAd mSplashAd;
    private int mWidth;
    private int mHeight;
    private int timeout = 3500; // 官方3500
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private int ecpmLevel;
    public static final String TAG = "KlevinSplash";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        String placementId;
        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);

            if (!TextUtils.isEmpty(placementId)) {
                mPostId = Integer.parseInt(placementId);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(KlevinConstant.AD_SPLASH_WIDTH)) {
                mWidth = (int) userParams.get(KlevinConstant.AD_SPLASH_WIDTH);
            }

            if (userParams.containsKey(KlevinConstant.AD_SPLASH_HEIGHT)) {
                mHeight = (int) userParams.get(KlevinConstant.AD_SPLASH_HEIGHT);
            }

            if (userParams.containsKey(AppKeyManager.TIME_DELTA)) {
                int localTimeOut = (int) userParams.get(AppKeyManager.TIME_DELTA);
                //可传值范围[3000-5000]
                if (localTimeOut >= 3000) {
                    timeout = localTimeOut;
                    Log.i(TAG, "timeout: " + timeout);
                }
            }

        }

        if (mWidth <= 0) {
            mWidth = context.getResources().getDisplayMetrics().widthPixels;
            Log.i(TAG, "Width:" + mWidth);
        }

        if (mHeight <= 0) {
            mHeight = context.getResources().getDisplayMetrics().heightPixels;
            Log.i(TAG, "Height:" + mHeight);
        }


//        mPostId = 30029;

        KlevinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestSplash();
            }

            @Override
            public void onFailed(String code, String msg) {
                TPError tpError = new TPError(INIT_FAILED);
                tpError.setErrorCode(code);
                tpError.setErrorMessage(msg);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });

    }

    private void requestSplash() {
        if (isC2SBidding && isBiddingLoaded) {
            if (mLoadAdapterListener != null && mSplashAd != null) {
                setNetworkObjectAd(mSplashAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            return;
        }

        SplashAdRequest.Builder splashAdBuilder = new SplashAdRequest.Builder();
        splashAdBuilder.setTimeOut(timeout)// 【可选】默认不限制，可传值范围[3000-5000]
                .setViewSize(mWidth, mHeight) //【必传】展示开屏⼴告的view宽⾼
                .setAdCount(1)//【可选】广告个数，默认1
                .setPosId(mPostId);//【必须】开屏广告位id


        //开屏广告加载回调
        SplashAd.load(splashAdBuilder.build(), new SplashAd.SplashAdLoadListener() {
            public void onTimeOut() { //加载超时
                Log.i(TAG, "onTimeOut");
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_TIMEOUT));
            }

            public void onAdLoadError(int err, String msg) {
                // 加载失败，err是错误码，msg是描述信息
                Log.i(TAG, "onAdLoadError: " + err + " " + msg);
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        onC2STokenListener.onC2SBiddingFailed(err+"",msg);
                    }
                    return;
                }

                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(KlevinErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, err, msg));
            }

            public void onAdLoaded(SplashAd ad) {//加载成功，ad为开屏广告实例
                Log.i(TAG, "splash ad loaded");
                mSplashAd = ad;

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        ecpmLevel = mSplashAd.getECPM();
                        Log.i(TAG, "开屏 bid price: " + ecpmLevel);
                        if (TextUtils.isEmpty(ecpmLevel+"")) {
                            onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                    }
                    isBiddingLoaded = true;
                    return;
                }

                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(ad);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }
        });
    }

    @Override
    public void showAd() {
        if (mSplashAd != null && mSplashAd.isValid()) {
            //设置开屏广告展示回调
            mSplashAd.registerAdInteractionListener(new SplashAd.SplashAdListener() {
                public void onAdSkip() { //用户跳过广告回调
                    Log.i(TAG, "onAdSkip");
                    if (mShowListener != null)
                        mShowListener.onRewardSkip();
                }

                public void onAdShow() { //广告曝光回调
                    Log.i(TAG, "onAdShow");
                    if (mShowListener != null)
                        mShowListener.onAdShown();
                }

                public void onAdClick() { //广告点击回调
                    Log.i(TAG, "onAdClick");
                    if (mShowListener != null)
                        mShowListener.onAdClicked();
                }

                public void onAdClosed() { //广告关闭回调
                    Log.i(TAG, "onAdClosed");
                    if (mShowListener != null)
                        mShowListener.onAdClosed();
                }

                public void onAdError(int err, String msg) {
                    //广告展示失败回调
                    Log.i(TAG, "onAdError err: " + err + " " + msg);
                    TPError tpError = new TPError(SHOW_FAILED);
                    tpError.setErrorCode(err + "");
                    tpError.setErrorMessage(msg);
                    if (mShowListener != null)
                        mShowListener.onAdVideoError(tpError);
                }

                @Override
                public void onAdDetailClosed(int i) {

                }
            });
            if (ecpmLevel > 0) {
                Log.i(TAG, "sendWinNotificationWithPrice: " +ecpmLevel);
                mSplashAd.sendWinNotificationWithPrice(ecpmLevel);
            }
            //展示开屏广告
            View splashView = mSplashAd.getSplashView();
            if (splashView != null && mAdContainerView != null) {
                // 把SplashView 添加到ViewGroup中
                mAdContainerView.addView(splashView);
            } else {
                Log.i(TAG, "showAd Failed, mSplashAd.getSplashView() == null ");
                TPError tpError = new TPError(UNSPECIFIED);
                tpError.setErrorMessage("mSplashAd.getSplashView() == null");
                if (mShowListener != null)
                    mShowListener.onAdVideoError(tpError);
            }

        } else {
            if (mShowListener != null)
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
        }
    }


    @Override
    public boolean isReady() {
        return mSplashAd != null && mSplashAd.isValid();
    }

    @Override
    public void clean() {
        if (mSplashAd != null) {
            mSplashAd.registerAdInteractionListener(null);
            mSplashAd.destroy();
            mSplashAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KLEVIN);
    }

    @Override
    public String getNetworkVersion() {
        return KlevinManager.getVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        TPTaskManager.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                loadCustomAd(context, localParams, tpParams);
            }
        });
    }

}
