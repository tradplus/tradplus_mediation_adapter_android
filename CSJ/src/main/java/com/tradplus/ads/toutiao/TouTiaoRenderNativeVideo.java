package com.tradplus.ads.toutiao;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

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
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TouTiaoRenderNativeVideo extends TPNativeAdapter {

    private TTFeedAd mTTFeedAd;
    private TTNativeExpressAd ttNativeExpressAd;
    private ToutiaoNativeAd mToutiaoNativeAd;
    public static final String TAG = "ToutiaoNative";
    private String mPlacementId;
    private int mWidth;
    private int mHeight;
    private String secType;
    private int mIsTemplateRending;
    private TTAdManager adManager;
    private TTAdNative mAdNative;
    private int onAdShow = 0;
    private int onAdShowExpressAd = 0;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private double ecpmLevel;
    private OnC2STokenListener onC2STokenListener;
    private boolean mNeedDownloadImg = false;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            String template = serverExtras.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            secType = serverExtras.get(AppKeyManager.ADTYPE_SEC);

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("", ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }


        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(DataKeys.AD_WIDTH)) {
                mWidth = (int) localExtras.get(DataKeys.AD_WIDTH);
            }

            if (localExtras.containsKey(DataKeys.AD_HEIGHT)) {
                mHeight = (int) localExtras.get(DataKeys.AD_HEIGHT);
            }

            if (localExtras.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) localExtras.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

        if (mWidth <= 0 && mHeight <= 0) {
            if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES || mIsTemplateRending == ToutiaoConstant.NATIVE_PATCH_VIDEO) {
                mWidth = (int) UIUtils.getScreenWidthDp(context);
                mHeight = AppKeyManager.NATIVE_DEFAULT_HEIGHT;
            } else {
                mWidth = ToutiaoConstant.NATIVE_IMAGE_ACCEPTED_SIZE_X;
                mHeight = ToutiaoConstant.NATIVE_IMAGE_ACCEPTED_SIZE_Y;
            }
        }

        Log.i(TAG, "Width :" + mWidth + ", Height :" + mHeight);

        adManager = TTAdSdk.getAdManager();
        mAdNative = adManager.createAdNative(context);

        ToutiaoInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
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

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });
    }

    private void requestNative(Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mLoadAdapterListener != null) {
                if (ttNativeExpressAd != null) {
                    ttNativeExpressAd.win(ecpmLevel);
                }
                if (mTTFeedAd != null) {
                    mTTFeedAd.win(ecpmLevel);
                }
                if (mExpressAd != null) {
                    mExpressAd.win(ecpmLevel);
                }
                mLoadAdapterListener.loadAdapterLoaded(mToutiaoNativeAd);
            }
            return;
        }

        if (mIsTemplateRending == ToutiaoConstant.NATIVE_PATCH_VIDEO) {
            AdSlot adSlot = new AdSlot.Builder()
                    .setCodeId(mPlacementId)
                    .setImageAcceptedSize(mWidth, mHeight)
                    .setAdCount(3)
                    .build();

            mAdNative.loadStream(adSlot,streamFeedAdListener);
        } else if (AppKeyManager.NATIVE_TYPE_DRAWLIST.equals(secType)) {
            Log.i(TAG, "requestNative: ");
            Activity activity = GlobalTradPlus.getInstance().getActivity();
            if (activity == null) {
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
                }
                return;
            }
            float expressViewWidth = UIUtils.getScreenWidthDp(context);
            float expressViewHeight = UIUtils.getHeight(activity);
            AdSlot adSlot = new AdSlot.Builder()
                    .setCodeId(mPlacementId)
                    .setAdCount(3)
                    .setSupportDeepLink(true)
                    .setExpressViewAcceptedSize(expressViewWidth, expressViewHeight)
                    .build();

            mAdNative.loadExpressDrawFeedAd(adSlot, nativeExpressdrawAdFeedListener);
        } else if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            //模版
            Log.i(TAG, "requestNative: 模版");
            AdSlot slot = new AdSlot.Builder()
                    .setCodeId(mPlacementId)
                    .setSupportDeepLink(true)
                    .setAdCount(1)
                    .setExpressViewAcceptedSize(mWidth, mHeight).build();
            mAdNative.loadNativeExpressAd(slot, nativeExpressAdListener);
        } else {
            //自渲染
            Log.i(TAG, "requestNative: 自渲染");
            AdSlot slot = new AdSlot.Builder()
                    .setCodeId(mPlacementId)
                    .setSupportDeepLink(true)
                    .setImageAcceptedSize(mWidth, mHeight)
                    .setAdCount(3)
                    .build();
            mAdNative.loadFeedAd(slot, feedAdListener);
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

    final TTAdNative.NativeExpressAdListener nativeExpressdrawAdFeedListener = new TTAdNative.NativeExpressAdListener() {

        @Override
        public void onError(int errorCode, String errorMsg) {
            Log.i(TAG, "onError: " + errorCode + ":errorMsg:" + errorMsg);
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
            }
        }

        @Override
        public void onNativeExpressAdLoad(List<TTNativeExpressAd> list) {
            Log.i(TAG, "onNativeExpressAdLoad: " + list.size() + ":list data:" + list);
            if (list == null || list.size() == 0) {
                return;
            }


            List<View> views = new ArrayList<>();

            for (TTNativeExpressAd ad : list) {
                views.add(ad.getExpressAdView());
                ttNativeExpressAd = ad;

                ad.setVideoAdListener(new TTNativeExpressAd.ExpressVideoAdListener() {
                    @Override
                    public void onVideoLoad() {
                        Log.i(TAG, "onVideoLoad: ");
                    }

                    @Override
                    public void onVideoError(int errorCode, int extraCode) {
                        Log.i(TAG, "onVideoError: errorCode :" + errorCode);
                        if (mToutiaoNativeAd != null) {
                            TPError tpError = new TPError(SHOW_FAILED);
                            tpError.setErrorCode(errorCode + "");
                            mToutiaoNativeAd.adShowFailed(tpError);
                        }
                    }

                    @Override
                    public void onVideoAdStartPlay() {
                        Log.i(TAG, "onVideoAdStartPlay: ");
                        if (mToutiaoNativeAd != null) {
                            mToutiaoNativeAd.adShown();
                        }

                        if (mToutiaoNativeAd != null) {
                            mToutiaoNativeAd.onAdVideoStart();
                        }

                    }

                    @Override
                    public void onVideoAdPaused() {
                        Log.i(TAG, "onVideoAdPaused: ");
                    }

                    @Override
                    public void onVideoAdContinuePlay() {
                        Log.i(TAG, "onVideoAdContinuePlay: ");
                    }

                    @Override
                    public void onProgressUpdate(long current, long duration) {
                    }

                    @Override
                    public void onVideoAdComplete() {
                        Log.i(TAG, "onVideoAdComplete: ");
                        if (mToutiaoNativeAd != null) {
                            mToutiaoNativeAd.onAdVideoEnd();
                        }
                    }

                    @Override
                    public void onClickRetry() {
                        Log.i(TAG, "onClickRetry: ");
                        Log.d("drawss", "onClickRetry!");
                    }
                });

                ad.setDownloadListener(downloadListener);
                ad.setCanInterruptVideoPlay(true);
                ad.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
                    @Override
                    public void onAdClicked(View view, int type) {
                        Log.i(TAG, "onAdClicked: ");
                        if (mToutiaoNativeAd != null) {
                            mToutiaoNativeAd.adClicked();
                        }
                    }

                    @Override
                    public void onAdShow(View view, int type) {
                        if (onAdShow == 0) {
                            Log.i(TAG, "onAdShow: ");
                            if (mToutiaoNativeAd != null) {
                                onAdShow = 1;
                                mToutiaoNativeAd.adShown();
                            }

                        }
                    }

                    @Override
                    public void onRenderFail(View view, String msg, int code) {
                        Log.i(TAG, "onRenderFail: " + msg + ":errorCode:" + code);
                        if (isC2SBidding) {
                            C2SBiddingFailed(code + "",msg);
                            return;
                        }

                        if (mLoadAdapterListener != null) {
                            mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(code, msg));
                        }
                    }

                    @Override
                    public void onRenderSuccess(View view, float width, float height) {
                        Log.i(TAG, "onRenderSuccess: ");
                    }
                });

                ad.render();
                bindDislike(ad, false);
            }
            mToutiaoNativeAd = new ToutiaoNativeAd(views);
            mToutiaoNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_LIST);

            if (isC2SBidding) {
                C2SBiddingLoaded(ttNativeExpressAd.getMediaExtraInfo());
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mToutiaoNativeAd);
            }

            Log.i(TAG, "onNativeExpressAdLoad: " + list.size() + ":list data:" + list);
        }

    };

    final TTAdNative.FeedAdListener feedAdListener = new TTAdNative.FeedAdListener() {
        @Override
        public void onError(int errorCode, String errorMsg) {
            Log.i(TAG, "onError: ");
            if (isC2SBidding) {
                C2SBiddingFailed(errorCode + "",errorMsg);
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
            }
        }

        @Override
        public void onFeedAdLoad(List<TTFeedAd> list) {
            Log.i(TAG, "onFeedAdLoad: ");
            if (list == null || list.size() == 0) {
                return;
            }
            mTTFeedAd = list.get(0);
            mTTFeedAd.setVideoAdListener(mVideoFeedListener);
            mTTFeedAd.setDownloadListener(downloadListener);
            mToutiaoNativeAd = new ToutiaoNativeAd(mTTFeedAd);
            // 返回自渲染Type
            mToutiaoNativeAd.setRenderType(TPBaseAd.AD_TYPE_NORMAL_NATIVE);

            if (isC2SBidding) {
                C2SBiddingLoaded(mTTFeedAd.getMediaExtraInfo());
                return;
            }

            downloadAndCallback(mToutiaoNativeAd, mNeedDownloadImg);
        }
    };

    final TTAdNative.FeedAdListener streamFeedAdListener = new TTAdNative.FeedAdListener() {
        @Override
        public void onError(int errorCode, String errorMsg) {
            Log.i(TAG, "onError: ");

            if (isC2SBidding) {
                C2SBiddingFailed(errorCode + "",errorMsg);
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
            }
        }

        @Override
        public void onFeedAdLoad(List<TTFeedAd> list) {
            Log.i(TAG, "onFeedAdLoad: ");
            if (list == null || list.size() == 0) {
                return;
            }

            mTTFeedAd = list.get(0);
            mTTFeedAd.setVideoAdListener(mVideoFeedListener);
            mTTFeedAd.setDownloadListener(downloadListener);
            mToutiaoNativeAd = new ToutiaoNativeAd(mTTFeedAd);
            mToutiaoNativeAd.setRenderType(ToutiaoConstant.NATIVE_PATCH_VIDEO);

            if (isC2SBidding) {
                C2SBiddingLoaded(mTTFeedAd.getMediaExtraInfo());
                return;
            }

            downloadAndCallback(mToutiaoNativeAd, mNeedDownloadImg);
        }
    };

    private TTNativeExpressAd mExpressAd;
    final TTAdNative.NativeExpressAdListener nativeExpressAdListener = new TTAdNative.NativeExpressAdListener() {
        @Override
        public void onError(int errorCode, String errorMsg) {
            Log.i(TAG, "onError: " + errorCode + ":errorMsg:" + errorMsg);

            if (isC2SBidding) {
                C2SBiddingFailed(errorCode + "",errorMsg);
                return;
            }

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
            }
        }

        @Override
        public void onNativeExpressAdLoad(List<TTNativeExpressAd> list) {
            Log.i(TAG, "onNativeExpressAdLoad: ");
            if (list == null || list.size() == 0) {
                return;
            }
            mExpressAd = list.get(0);
            bindListener(mExpressAd);
            mExpressAd.render();
        }
    };

    private void bindListener(final TTNativeExpressAd mTtNativeExpressAd) {
        if (mTtNativeExpressAd != null) {
            mTtNativeExpressAd.setVideoAdListener(mVideoAdListener);
            mTtNativeExpressAd.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
                @Override
                public void onAdClicked(View view, int i) {
                    Log.i(TAG, "onAdClicked: ");
                    if (mToutiaoNativeAd != null) {
                        mToutiaoNativeAd.adClicked();
                    }
                }

                @Override
                public void onAdShow(View view, int i) {
                    if (onAdShowExpressAd == 0) {
                        Log.i(TAG, "onAdShow: ");
                        if (mToutiaoNativeAd != null) {
                            mToutiaoNativeAd.adShown();
                        }
                        onAdShowExpressAd = 1;
                    }

                }

                @Override
                public void onRenderFail(View view, String errorMsg, int errorCode) {
                    Log.i(TAG, "onRenderFail: " + errorMsg + ":errorCode:" + errorCode);

                    if (isC2SBidding) {
                        C2SBiddingFailed(errorCode + "",errorMsg);
                        return;
                    }

                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(errorCode, errorMsg));
                    }
                }

                @Override
                public void onRenderSuccess(View view, float v, float v1) {
                    Log.i(TAG, "onRenderSuccess: ");

                    Context context = GlobalTradPlus.getInstance().getContext();
                    if (context == null) {
                        if (mLoadAdapterListener != null) {
                            TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                            tpError.setErrorMessage("context == null");
                            mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                        }
                        return;
                    }

                    mToutiaoNativeAd = new ToutiaoNativeAd(context,view);
                    mToutiaoNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_EXPRESS);

                    if (isC2SBidding) {
                        C2SBiddingLoaded(mTtNativeExpressAd.getMediaExtraInfo());
                        return;
                    }

                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoaded(mToutiaoNativeAd);
                    }
                }
            });
            mTtNativeExpressAd.setDownloadListener(downloadListener);
            bindDislike(mTtNativeExpressAd, false);
        }
    }

    private void C2SBiddingLoaded(Map<String, Object> mediaExtraInfo) {
        if (onC2STokenListener != null && mediaExtraInfo != null) {
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
    }

    private void C2SBiddingFailed(String code,String msg) {
        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                onC2STokenListener.onC2SBiddingFailed(code,msg);
            }
        }
    }

    @Override
    public void setLossNotifications(String auctionPrice, String lossReason) {
        if (ttNativeExpressAd != null) {
            try {
                ttNativeExpressAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        }

        if (mTTFeedAd != null) {
            try {
                mTTFeedAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        if (mExpressAd != null) {
            try {
                mExpressAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }


    }



    private void bindDislike(final TTNativeExpressAd ad, boolean customStyle) {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity != null) {
            ad.setDislikeCallback(activity, new TTAdDislike.DislikeInteractionCallback() {
                @Override
                public void onShow() {
                    Log.i(TAG, "onShow: ");
                }

                @Override
                public void onSelected(int position, String value, boolean b) {
                    Log.i(TAG, "onSelected: ");
                    if (ad != null)
                        ad.destroy();
                }

                @Override
                public void onCancel() {
                    Log.i(TAG, "onCancel: ");
                    if (mToutiaoNativeAd != null) {
                        mToutiaoNativeAd.adClosed();
                    }
                }

            });
        }
    }

    private final TTFeedAd.VideoAdListener mVideoFeedListener = new TTFeedAd.VideoAdListener() {
        @Override
        public void onVideoLoad(TTFeedAd ttFeedAd) {

        }

        @Override
        public void onVideoError(int i, int i1) {

        }

        @Override
        public void onVideoAdStartPlay(TTFeedAd ttFeedAd) {
            Log.i(TAG, "onVideoAdStartPlay: ");
            if (mToutiaoNativeAd != null) {
                mToutiaoNativeAd.onAdVideoStart();
            }
        }

        @Override
        public void onVideoAdPaused(TTFeedAd ttFeedAd) {

        }

        @Override
        public void onVideoAdContinuePlay(TTFeedAd ttFeedAd) {

        }

        @Override
        public void onProgressUpdate(long l, long l1) {

        }

        @Override
        public void onVideoAdComplete(TTFeedAd ttFeedAd) {
            Log.i(TAG, "onVideoAdComplete: ");
            if (mToutiaoNativeAd != null) {
                mToutiaoNativeAd.onAdVideoEnd();
            }
        }
    };

    private final TTNativeExpressAd.ExpressVideoAdListener mVideoAdListener = new TTNativeExpressAd.ExpressVideoAdListener() {
        @Override
        public void onVideoLoad() {
            Log.i(TAG, "onVideoLoad: ");
        }

        @Override
        public void onVideoError(int errorCode, int extraCode) {
            Log.i(TAG, "onVideoError: errorCode :" + errorCode);
            if (mToutiaoNativeAd != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(errorCode + "");
                mToutiaoNativeAd.adShowFailed(tpError);
            }
        }

        @Override
        public void onVideoAdStartPlay() {
            Log.i(TAG, "onVideoAdStartPlay: ");
            if (mToutiaoNativeAd != null) {
                mToutiaoNativeAd.onAdVideoStart();
            }
        }

        @Override
        public void onVideoAdPaused() {
            Log.i(TAG, "onVideoAdPaused: ");
        }

        @Override
        public void onVideoAdContinuePlay() {
            Log.i(TAG, "onVideoAdContinuePlay: ");
        }

        @Override
        public void onProgressUpdate(long current, long duration) {
        }

        @Override
        public void onVideoAdComplete() {
            Log.i(TAG, "onVideoAdComplete: ");
            if (mToutiaoNativeAd != null) {
                mToutiaoNativeAd.onAdVideoEnd();
            }
        }

        @Override
        public void onClickRetry() {
            Log.i(TAG, "onClickRetry: ");
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mTTFeedAd != null) {
            mTTFeedAd.setVideoAdListener(null);
            mTTFeedAd.destroy();
            mTTFeedAd = null;
        }

        if (mExpressAd != null) {
            mExpressAd.setExpressInteractionListener(null);
            mExpressAd.setVideoAdListener(null);
            mExpressAd.destroy();
            mExpressAd = null;
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
