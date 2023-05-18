package com.tradplus.ads.huawei;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.util.Log;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.AudioFocusType;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.splash.SplashAdDisplayListener;
import com.huawei.hms.ads.splash.SplashView;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class HuaweiSplash extends TPSplashAdapter {


    private String mPlacementId;
    private SplashView splashView;
    private static final String TAG = "HuaweiCnSplash";
    private int direction = 1; // 默认竖屏
    private boolean isReady = true;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (tpParams.containsKey(AppKeyManager.DIRECTION)) {
                direction = Integer.valueOf(tpParams.get(AppKeyManager.DIRECTION));
            }
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        HuaweiInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestSplash(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestSplash(Context context) {
        int orientation = direction == 1 ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : SCREEN_ORIENTATION_LANDSCAPE;
//        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ;
        AdParam adParam = new AdParam.Builder().build();

        splashView = new SplashView(context);

        splashView.setAdDisplayListener(adDisplayListener);
        // 设置视频类开屏广告的音频焦点类型
        splashView.setAudioFocusType(AudioFocusType.NOT_GAIN_AUDIO_FOCUS_WHEN_MUTE);
        // 获取并展示开屏广告
        splashView.load(mPlacementId, orientation, adParam, splashAdLoadListener);
    }

    private boolean isLoaded;

    private final SplashView.SplashAdLoadListener splashAdLoadListener = new SplashView.SplashAdLoadListener() {
        @Override
        public void onAdLoaded() {
            // 广告获取成功时调用
            if (mAdContainerView != null) {
                isLoaded = true;
                Log.i(TAG, "onAdLoaded: ");
                mAdContainerView.addView(splashView);
                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(splashView);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            } else {
                TPError tpError = new TPError(UNSPECIFIED);
                tpError.setErrorMessage("mAdContainerView 容器为空");
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            // 广告获取失败时调用, 跳转至App主界面
            Log.i(TAG, "onAdFailedToLoad: errorCode : " + errorCode);
            TPError tpError = new TPError(NETWORK_NO_FILL);
            tpError.setErrorCode(errorCode + "");
            if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);

        }

        @Override
        public void onAdDismissed() {
            // 广告展示完毕时调用, 跳转至App主界面
            if (mShowListener != null && isLoaded && isReady) {
                Log.i(TAG, "onAdDismissed: ");
                isReady = false;
                mShowListener.onAdClosed();
            }
        }
    };

    private final SplashAdDisplayListener adDisplayListener = new SplashAdDisplayListener() {
        @Override
        public void onAdShowed() {
            // 广告显示时调用
            Log.i(TAG, "onAdShowed: ");
            if (mShowListener != null)
                mShowListener.onAdShown();
        }

        @Override
        public void onAdClick() {
            // 广告被点击时调用
            Log.i(TAG, "onAdClick: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }

            // 不在生命周期stop回来时候clean，需要延迟，否则跳转前会看到广告remove 客户体验不好
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    clean();
                }
            }, 500);
        }
    };

    @Override
    public void showAd() {

    }

    @Override
    public void clean() {
        if (mShowListener != null && isReady) {
            isReady = false;
            mShowListener.onAdClosed();
        }

        if (splashView != null) {
            Log.i(TAG, "clean: ");
            splashView.setAdDisplayListener(null);
            splashView.removeAllViews();
            splashView.destroyView();
            splashView = null;
        }
    }

    @Override
    public boolean isReady() {
        Log.i(TAG, "isReady: " + isReady);
        return isReady;
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

