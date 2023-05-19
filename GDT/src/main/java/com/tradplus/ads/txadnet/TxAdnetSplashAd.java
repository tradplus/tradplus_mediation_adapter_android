package com.tradplus.ads.txadnet;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;


import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADZoomOutListener;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.setting.GlobalSetting;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.HashMap;
import java.util.Map;

public class TxAdnetSplashAd extends TPSplashAdapter {

    private String mPlacementId;
    private long fetchSplashADTime;
    private SplashAD splashAD;
    private static final String TAG = "GDTSplashAd";
    private int minSplashTimeWhenNoAD = 2000;
    private long expireTimestamp = 0;
    private int mZoomOut;
    private int mIsHaslfSplash = 0;
    private String payload;
    private String price;
    private String mShakable = "1";
    private int fetchDelay = 0;
    private int mAppIcon;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            price = tpParams.get(DataKeys.BIDDING_PRICE);
            mZoomOut = Integer.parseInt(tpParams.get(GDTConstant.ZOOM_OUT));
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(GDTConstant.GDT_HALFSPLASH)) {
                mIsHaslfSplash = (Integer) userParams.get(GDTConstant.GDT_HALFSPLASH);
            }

            if (userParams.containsKey(GDTConstant.SHAKABLE)) {
                boolean shakable = (boolean) userParams.get(GDTConstant.SHAKABLE);
                if (!shakable) mShakable = "0";

            }

            if (userParams.containsKey(AppKeyManager.TIME_DELTA)) {
                int localTimeOut = (int) userParams.get(AppKeyManager.TIME_DELTA);

                if (localTimeOut >= 1500) {
                    fetchDelay = localTimeOut;
                    Log.i(TAG, "fetchDelay: " + fetchDelay);
                }
            }

            if (userParams.containsKey(AppKeyManager.APPICON)) {
                int appIcon = (int) userParams.get(AppKeyManager.APPICON);
                mAppIcon = appIcon;
            }
        }

        TencentInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                fetchSplashAD((Activity) context, mPlacementId, fetchDelay);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void fetchSplashAD(Activity activity, String posId, int fetchDelay) {
        fetchSplashADTime = System.currentTimeMillis();

        Map<String, String> extraUserData = new HashMap<>();
        extraUserData.put("shakable", mShakable);
        GlobalSetting.setExtraUserData(extraUserData);

        if (TextUtils.isEmpty(payload)) {
            splashAD = new SplashAD(activity, posId, mSplashADZoomOutListener, fetchDelay);
        } else {
            splashAD = new SplashAD(activity, posId, mSplashADZoomOutListener, fetchDelay, payload);
        }

        if (mIsHaslfSplash == 1) {
            splashAD.fetchAdOnly();
        } else {
            if (mAppIcon != 0) {
                splashAD.setDeveloperLogo(mAppIcon);
            }
            splashAD.fetchFullScreenAdOnly();
        }

    }

    private ViewGroup zoomOutView;
    private final SplashADZoomOutListener mSplashADZoomOutListener = new SplashADZoomOutListener() {
        @Override
        public void onZoomOut() {
            Log.i(TAG, "onZoomOut: ");
            if (mAdContainerView == null) {
                return;
            }
            SplashZoomOutManager splashZoomOutManager = SplashZoomOutManager.getInstance();
            zoomOutView = splashZoomOutManager.startZoomOut(mAdContainerView.getChildAt(0), mAdContainerView, mAdContainerView,
                    new SplashZoomOutManager.AnimationCallBack() {
                        @Override
                        public void animationStart(int animationTime) {
                            Log.d("AD_DEMO", "animationStart:" + animationTime);
                            if (mShowListener != null) {
                                mShowListener.onZoomOutStart();
                            }
                        }

                        @Override
                        public void animationEnd() {
                            Log.d("AD_DEMO", "animationEnd");
                            splashAD.zoomOutAnimationFinish();
                            if (mZoomOut == 1) {
                                if (mShowListener != null) {
                                    mShowListener.onZoomOutEnd();
                                }
                            }
                        }
                    });
        }

        @Override
        public void onZoomOutPlayFinish() {
            Log.i(TAG, "onZoomOutPlayFinish: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }

        }

        @Override
        public boolean isSupportZoomOut() {
            return mZoomOut == 1 ? true : false;
        }

        @Override
        public void onADDismissed() {
            Log.i(TAG, "onADDismissed: ");
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onNoAD(final AdError adError) {
            Log.i(TAG, "onNoAD，errorCode：" + adError.getErrorCode() + ",errorMessage: " + adError.getErrorMsg());
            long alreadyDelayMills = System.currentTimeMillis() - fetchSplashADTime;
            long shouldDelayMills = alreadyDelayMills > minSplashTimeWhenNoAD ? 0 : minSplashTimeWhenNoAD
                    - alreadyDelayMills;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
                    }
                }
            }, shouldDelayMills);

        }

        @Override
        public void onADPresent() {
            Log.i(TAG, "onADPresent: ");


        }

        @Override
        public void onADClicked() {
            Log.i(TAG, "onADClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onADTick(long l) {
            Log.i(TAG, "onADTick: ");
            if (mShowListener != null) {
                mShowListener.onTick(l);
            }
        }

        @Override
        public void onADExposure() {
            Log.i(TAG, "onADExposure: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onADLoaded(long l) {
            Log.i(TAG, "onADLoaded: " + l);
            expireTimestamp = l;
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(splashAD);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
        }
    };


    @Override
    public boolean isReady() {
        return SystemClock.elapsedRealtime() < expireTimestamp && splashAD != null && splashAD.isValid();
    }

    @Override
    public void showAd() {
        if (isReady() && mAdContainerView != null) {
            setBidEcpm();
            if (mIsHaslfSplash == 1) {
                splashAD.showAd(mAdContainerView);
            } else {
                splashAD.showFullScreenAd(mAdContainerView);
            }
        }
    }

    @Override
    public void clean() {
        if (splashAD != null) {
            splashAD = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TENCENT);
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    @Override
    public void setNetworkExtObj(Object obj) {
        if (obj instanceof DownloadConfirmListener) {
            if (splashAD != null) {
                splashAD.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }
        }

    }

    private void setBidEcpm() {
        try {
            float temp = Float.parseFloat(price);
            int price = (int) temp;
            Log.i(TAG, "setBidEcpm: " + price);
            splashAD.setBidECPM(price);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }
        String appId = tpParams.get(AppKeyManager.APP_ID);
        if (!TencentInitManager.isInited(appId)) {
            GDTAdSdk.init(context, appId);
        }
        return GDTAdSdk.getGDTAdManger().getBuyerId(null);
    }

    @Override
    public String getBiddingNetworkInfo(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        if (tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            try {
                return GDTAdSdk.getGDTAdManger().getSDKInfo(placementId);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return "";
            }
        }
        return "";
    }
}
