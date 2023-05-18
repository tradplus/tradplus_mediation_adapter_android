package com.tradplus.ads.toutiao;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;

public class TouTiaoBanner extends TPBannerAdapter {

    private TTAdNative mAdNative;
    private TTAdManager adManager;
    private String mPlacementId;
    private TTNativeExpressAd mTTAd;
    private TPBannerAdImpl mTpBannerAd;
    private int onAdShow = 0;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private double ecpmLevel;
    private OnC2STokenListener onC2STokenListener;
    private static final String TAG = "ToutiaoBanner";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            setAdHeightAndWidthByService(mPlacementId, tpParams);
            setDefaultAdSize(640, 100);
        }else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        setAdHeightAndWidthByUser(userParams);

        adManager = TTAdSdk.getAdManager();
        mAdNative = adManager.createAdNative(context);

        ToutiaoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                initBanner();
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });


    }


    private void initBanner() {
        if (isC2SBidding && isBiddingLoaded) {
            if (mLoadAdapterListener != null) {
                mTTAd.win(ecpmLevel);
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }
            return;
        }

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(mPlacementId)
                .setAdCount(1)
                .setImageAcceptedSize(mAdWidth, mAdHeight)
                .setExpressViewAcceptedSize(mAdViewWidth <= 0 ? mAdWidth / 2 : mAdViewWidth, mAdViewHeight <= 0 ? mAdHeight / 2 : mAdViewHeight)//期望模板广告view的size,单位dp
                .build();

        mAdNative.loadBannerExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.i(TAG, "onError: errorCode : " + errorCode + ", errorMsg :" + errorMsg);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> list) {
                if (list == null || list.size() == 0) {
                    return;
                }

                mTTAd = list.get(0);
                mTTAd.setSlideIntervalTime(0);
                mTTAd.setExpressInteractionListener(mExpressAdInteractionListener);
                mTTAd.setDownloadListener(downloadListener);

                Activity activity = GlobalTradPlus.getInstance().getActivity();
                Log.i(TAG, "activity: " + activity);
                if (activity != null) {
                    bindDislike(activity, mTTAd, false);
                }else {
                }

                mTTAd.render();
            }
        });

    }

    @Override
    public void setLossNotifications(String auctionPrice, String lossReason) {
        if (mTTAd != null) {
            try {
                mTTAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private boolean isDownLoadStart;
    private TTAppDownloadListener downloadListener =  new TTAppDownloadListener() {
        @Override
        public void onIdle() {

        }

        @Override
        public void onDownloadActive(long l, long l1, String s, String s1) {
            if (mDownloadListener != null && !isDownLoadStart) {
                isDownLoadStart = true;
                mDownloadListener.onDownloadStart(l, l1, s, s1);
            }
            Log.i(TAG, "onDownloadActive: " +  l + " " + l1 );
            if (mDownloadListener != null)
                mDownloadListener.onDownloadUpdate(l,l1,s,s1,0);
        }

        @Override
        public void onDownloadPaused(long l, long l1, String s, String s1) {
            Log.i(TAG, "onDownloadPaused: " +  l + " " + l1 );
            if (mDownloadListener != null)
                mDownloadListener.onDownloadPause(l,l1,s,s1);
        }

        @Override
        public void onDownloadFailed(long l, long l1, String s, String s1) {
            Log.i(TAG, "onDownloadFailed: " +  l + " " + l1 );
            if (mDownloadListener != null)
                mDownloadListener.onDownloadFail(l,l1,s,s1);
        }

        @Override
        public void onDownloadFinished(long l, String s, String s1) {
            Log.i(TAG, "onDownloadFinished: " +  l + " " + s1 );
            if (mDownloadListener != null)
                mDownloadListener.onDownloadFinish(l,l,s,s1);
        }

        @Override
        public void onInstalled(String s, String s1) {
            Log.i(TAG, "onInstalled: " +  s + " " + s1 );
            if (mDownloadListener != null)
                mDownloadListener.onInstalled(0,0,s,s1);
        }
    };

    TTNativeExpressAd.ExpressAdInteractionListener mExpressAdInteractionListener = new TTNativeExpressAd.ExpressAdInteractionListener() {
        @Override
        public void onAdClicked(View view, int i) {
            Log.i(TAG, "onAdClicked: ");
            if (mTpBannerAd != null) {
                mTpBannerAd.adClicked();
            }
        }

        @Override
        public void onAdShow(View view, int i) {
            if (mTpBannerAd != null && onAdShow == 0) {
                Log.i(TAG, "onAdShow: ");
                onAdShow = 1;
                mTpBannerAd.adShown();
            }
        }

        @Override
        public void onRenderFail(View view, String errorMsg, int errorCode) {
            Log.i(TAG, "onRenderFail: ");
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(errorCode+"",errorMsg);
                }
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
            }
        }

        @Override
        public void onRenderSuccess(View view, float v, float v1) {
            Log.i(TAG, "onRenderSuccess: ");
            mAdWidth /= 2;
            mAdHeight /= 2;
            setBannerLayoutParams(view);
            mTpBannerAd = new TPBannerAdImpl(null, view);

            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    Map<String, Object> mediaExtraInfo = mTTAd.getMediaExtraInfo();
                    Integer price = (Integer)mediaExtraInfo.get("price");
                    Log.i(TAG, "price: "  + price);
                    if (price == null) {
                        onC2STokenListener.onC2SBiddingFailed("","price == null");
                        return;
                    }
                    ecpmLevel = price.doubleValue();
                    onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                }
                isBiddingLoaded = true;
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mTpBannerAd);
            }
        }
    };


    private void bindDislike(Activity activity, final TTNativeExpressAd ad, boolean customStyle) {
        ad.setDislikeCallback(activity, new TTAdDislike.DislikeInteractionCallback() {
            @Override
            public void onShow() {
                Log.i(TAG, "onShow: ");
            }

            @Override
            public void onSelected(int position, String value, boolean b) {
                Log.i(TAG, "onSelected: ");
                if (mTpBannerAd != null)
                    mTpBannerAd.adClosed();
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "onCancel: ");
            }

        });
    }

    @Override
    public void clean() {
        if (mTTAd != null) {
            mTTAd.setExpressInteractionListener(null);
            mTTAd.destroy();
            mTTAd = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_PANGLECN);
    }

    @Override
    public String getNetworkVersion() {
        if (adManager != null) {
            return adManager.getSDKVersion();
        }
        return null;
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }
}
