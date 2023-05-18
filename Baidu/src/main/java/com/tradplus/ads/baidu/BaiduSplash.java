package com.tradplus.ads.baidu;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashInteractionListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class BaiduSplash extends TPSplashAdapter {

    private String mPlacementId;
    private SplashAd mSplashAd;
    private static final String TAG = "BaiduSplash";
    private int mHeight = 640;
    private int mWidth = 480;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
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
            if (userParams.containsKey(AppKeyManager.SPLASH_HEIGHT)) {
                mHeight = (int) userParams.get(AppKeyManager.SPLASH_HEIGHT);
                Log.i(TAG, "Height:" + mHeight);
            }

            if (userParams.containsKey(AppKeyManager.SPLASH_WIDTH)) {
                mWidth = (int) userParams.get(AppKeyManager.SPLASH_WIDTH);
                Log.i(TAG, "Width:" + mWidth);
            }
        }

        BaiduInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                requestSplash(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestSplash(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mSplashAd);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            return;
        }

        SplashInteractionListener listener = new SplashInteractionListener() {
            @Override
            public void onLpClosed() {
                Log.i(TAG, "onLpClosed");

            }

            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed");
                if (mShowListener != null)
                    mShowListener.onAdClosed();
            }

            @Override
            public void onADLoaded() {
                Log.i(TAG, "onADLoaded");
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        String ecpmLevel = mSplashAd.getECPMLevel();
                        Log.i(TAG, "bid price: " + ecpmLevel);
                        if (TextUtils.isEmpty(ecpmLevel)) {
                            onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(Double.parseDouble(ecpmLevel));
                    }
                    isBiddingLoaded = true;
                    return;
                }

                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(mSplashAd);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }

            @Override
            public void onAdFailed(String reason) {
                Log.i(TAG, "onAdFailed:" + reason);
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        onC2STokenListener.onC2SBiddingFailed("",reason);
                    }
                    return;
                }

                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorMessage(reason);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onAdPresent() {
                Log.i(TAG, "onAdPresent");
                if (mShowListener != null)
                    mShowListener.onAdShown();
            }

            @Override
            public void onAdClick() {
                Log.i(TAG, "onAdClick");
                if (mShowListener != null)
                    mShowListener.onAdClicked();
            }

            @Override
            public void onAdCacheSuccess() {
                Log.i(TAG, "onAdCacheSuccess: ");
            }

            @Override
            public void onAdCacheFailed() {
                Log.i(TAG, "onAdCacheFailed: 广告缓存失败");
            }
        };


        RequestParameters.Builder parameters = new RequestParameters.Builder()
                .setHeight(mHeight)
                .setWidth(mWidth)
                .addExtra(SplashAd.KEY_POPDIALOG_DOWNLOAD, "true")
                .addExtra(SplashAd.KEY_DISPLAY_DOWNLOADINFO, "true")
                .addExtra(SplashAd.KEY_FETCHAD, "false");
        mSplashAd = new SplashAd(context, mPlacementId, parameters.build(),
                listener);

        mSplashAd.setDownloadDialogListener(new SplashAd.SplashAdDownloadDialogListener() {
            @Override
            public void adDownloadWindowShow() {
                Log.i(TAG, "adDownloadWindowShow");
            }

            @Override
            public void adDownloadWindowClose() {
                Log.i(TAG, "adDownloadWindowClose");
            }

            @Override
            public void onADPrivacyLpShow() {
                Log.i(TAG, "onADPrivacyLpShow");
            }

            @Override
            public void onADPrivacyLpClose() {
                Log.i(TAG, "onADPrivacyLpClose");
            }

            @Override
            public void onADPermissionShow() {
                Log.i(TAG, "onADPermissionShow");
            }

            @Override
            public void onADPermissionClose() {
                Log.i(TAG, "onADPermissionClose");
            }
        });
        mSplashAd.load();
    }

    @Override
    public void showAd() {
        if (mSplashAd == null || mAdContainerView == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            }
            return;
        }

        mSplashAd.show(mAdContainerView);
    }

    @Override
    public void clean() {
        if (mSplashAd != null) {
            mSplashAd.destroy();
            mSplashAd = null;
        }
    }

    @Override
    public boolean isReady() {
        return true;

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
