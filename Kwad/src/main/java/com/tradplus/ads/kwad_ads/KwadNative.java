package com.tradplus.ads.kwad_ads;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsAppDownloadListener;
import com.kwad.sdk.api.KsDrawAd;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsNativeAd;
import com.kwad.sdk.api.KsScene;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.kuaishou.KuaishouErrorUtil;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;


public class KwadNative extends TPNativeAdapter {

    private String mPlacementId, mBidResponseV2;
    private KwadNativeAd mKwadNativeAd;
    private KsFeedAd mKsFeedAd;
    private KsNativeAd mKsNativeAd;
    private KsDrawAd mKsDrawAd;
    private String secType;
    private int mIsTemplateRending;
    private static final String TAG = "KwadNative";
    private boolean mNeedDownloadImg = false;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String template;
        if (serverExtras != null && serverExtras.size() > 0) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            secType = serverExtras.get(AppKeyManager.ADTYPE_SEC);
            mBidResponseV2 = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            template = serverExtras.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(DataKeys.DOWNLOAD_IMG)) {
                String downLoadImg = (String) localExtras.get(DataKeys.DOWNLOAD_IMG);
                if (downLoadImg.equals("true")) {
                    mNeedDownloadImg = true;
                }
            }
        }

        KuaishouInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestNative();
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

    private KsScene getKsScene() {
        KsScene.Builder builder = new KsScene.Builder(Long.parseLong(mPlacementId));
        builder.adNum(3);

        if (!TextUtils.isEmpty(mBidResponseV2)) {
            builder.setBidResponseV2(mBidResponseV2);
        }
        return builder.build();
    }

    private void requestNative() {
        if (secType.equals(AppKeyManager.NATIVE_TYPE_DRAWLIST)) {
            Log.i(TAG, "requestDrawAd : ");
            requestDrawAd();
        } else if (mIsTemplateRending == AppKeyManager.TEMPLATE_RENDERING_YES) {
            Log.i(TAG, "requestExpress : ");
            requestExpress();
        } else {
            Log.i(TAG, "request : ");
            requestAd();
        }

    }

    private void requestExpress() {
        KsAdSDK.getLoadManager().loadConfigFeedAd(getKsScene(), new KsLoadManager.FeedAdListener() {
            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.i(TAG, "onError: " + errorCode + ":errorMsg:" + errorMsg);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(KuaishouErrorUtil.getTradPlusErrorCode(errorCode));
                }
            }

            @Override
            public void onFeedAdLoad(List<KsFeedAd> list) {
                if (list == null || list.isEmpty()) {
                    return;
                }
                mKsFeedAd = list.get(0);
                showExpressAd(mKsFeedAd);
            }
        });
    }

    private void showExpressAd(KsFeedAd ksFeedAd) {
        Context context = GlobalTradPlus.getInstance().getContext();
        if (context == null) {
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                tpError.setErrorMessage("context == null");
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
            return;
        }

        if (ksFeedAd != null) {
            mKwadNativeAd = new KwadNativeAd(context, ksFeedAd);

            View feedView = ksFeedAd.getFeedView(context);

            mKwadNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_EXPRESS);

            ksFeedAd.setAdInteractionListener(new KsFeedAd.AdInteractionListener() {
                @Override
                public void onAdClicked() {
                    Log.i(TAG, "onAdClicked: ");
                    if (mKwadNativeAd != null)
                        mKwadNativeAd.onAdViewClicked();
                }

                @Override
                public void onAdShow() {
                    Log.i(TAG, "onAdShow: ");
                    if (mKwadNativeAd != null)
                        mKwadNativeAd.onAdViewExpanded();
                }

                @Override
                public void onDislikeClicked() {
                    Log.i(TAG, "onDislikeClicked: ");
                    if (mKwadNativeAd != null) {
                        mKwadNativeAd.onAdClosed();
                    }
                }

                @Override
                public void onDownloadTipsDialogShow() {

                }

                @Override
                public void onDownloadTipsDialogDismiss() {

                }
            });

            mKwadNativeAd.setKsFeedAdView(feedView);

            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoaded(mKwadNativeAd);
            }

        }


    }

    private void requestDrawAd() {
        KsAdSDK.getLoadManager().loadDrawAd(getKsScene(), new KsLoadManager.DrawAdListener() {
            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.i(TAG, "onError: " + errorCode + ":errorMsg:" + errorMsg);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(KuaishouErrorUtil.getTradPlusErrorCode(errorCode));
                }
            }

            @Override
            public void onDrawAdLoad(List<KsDrawAd> list) {
                if (list == null || list.isEmpty()) {
                    return;
                }
                Context context = GlobalTradPlus.getInstance().getContext();
                if (context == null) {
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                        tpError.setErrorMessage("context == null");
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                    return;
                }

                List<View> views = new ArrayList<>();
                for (KsDrawAd ksDrawAd : list) {
                    mKsDrawAd = ksDrawAd;
                    mKwadNativeAd = new KwadNativeAd(ksDrawAd);
                    views.add(ksDrawAd.getDrawView(context));
                    mKwadNativeAd.setRenderType(TPBaseAd.AD_TYPE_NATIVE_LIST);
                    ksDrawAd.setAdInteractionListener(new KsDrawAd.AdInteractionListener() {
                        @Override
                        public void onAdClicked() {
                            Log.i(TAG, "onAdClicked: ");
                            if (mKwadNativeAd != null)
                                mKwadNativeAd.onAdViewClicked();
                        }

                        @Override
                        public void onAdShow() {
                            Log.i(TAG, "onAdShow: ");
                            if (mKwadNativeAd != null)
                                mKwadNativeAd.onAdViewExpanded();
                        }

                        @Override
                        public void onVideoPlayStart() {
                            Log.i(TAG, "onVideoPlayStart: ");
                            if (mKwadNativeAd != null) {
                                mKwadNativeAd.onAdVideoStart();
                            }
                        }

                        @Override
                        public void onVideoPlayPause() {
                            Log.i(TAG, "onVideoPlayPause: ");
                        }

                        @Override
                        public void onVideoPlayResume() {
                            Log.i(TAG, "onVideoPlayResume: ");
                        }

                        @Override
                        public void onVideoPlayEnd() {
                            Log.i(TAG, "onVideoPlayEnd: ");
                            if (mKwadNativeAd != null) {
                                mKwadNativeAd.onAdVideoEnd();
                            }
                        }

                        @Override
                        public void onVideoPlayError() {
                            Log.i(TAG, "onVideoPlayError: ");
                        }
                    });
                }


                mKwadNativeAd.setDrawViews(views);

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mKwadNativeAd);
                }

            }
        });
    }

    private void requestAd() {
        KsAdSDK.getLoadManager().loadNativeAd(getKsScene(), new KsLoadManager.NativeAdListener() {
            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.i(TAG, "onError: ， errorCode ：" + errorCode + ", errormessage :" + errorMsg);

                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(KuaishouErrorUtil.geTpMsg(NETWORK_NO_FILL, errorCode, errorMsg));
                }
            }

            @Override
            public void onNativeAdLoad(List<KsNativeAd> adList) {
                if (adList == null || adList.isEmpty()) {
                    return;
                }

                mKsNativeAd = adList.get(0);
                showAd(mKsNativeAd);
            }
        });
    }

    private void showAd(KsNativeAd ksNativeAd) {
        if (ksNativeAd != null && !secType.equals(AppKeyManager.NATIVE_TYPE_DRAWLIST)) {

            Context context = GlobalTradPlus.getInstance().getContext();
            if (context == null) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(TPError.ADAPTER_CONTEXT_ERROR);
                    tpError.setErrorMessage("context == null");
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
                return;
            }

            mKwadNativeAd = new KwadNativeAd(context, ksNativeAd);

            ksNativeAd.setVideoPlayListener(new KsNativeAd.VideoPlayListener() {
                @Override
                public void onVideoPlayReady() {

                }

                @Override
                public void onVideoPlayStart() {
                    Log.i(TAG, "onVideoPlayStart: ");
                    if (mKwadNativeAd != null) {
                        mKwadNativeAd.onAdVideoStart();
                    }

                }

                @Override
                public void onVideoPlayComplete() {
                    Log.i(TAG, "onVideoPlayComplete: ");
                    if (mKwadNativeAd != null) {
                        mKwadNativeAd.onAdVideoEnd();
                    }
                }

                @Override
                public void onVideoPlayError(int i, int i1) {
                    Log.i(TAG, "onVideoPlayError: ");
                }

                @Override
                public void onVideoPlayPause() {

                }

                @Override
                public void onVideoPlayResume() {

                }
            });

            ksNativeAd.setDownloadListener(new KsAppDownloadListener() {
                @Override
                public void onIdle() {
                    Log.i(TAG, "onIdle: ");
                }

                @Override
                public void onDownloadStarted() {
                    if (mDownloadListener != null)
                        mDownloadListener.onDownloadStart(0,0,"","");
                    Log.i(TAG, "onDownloadStarted: ");
                }

                @Override
                public void onProgressUpdate(int i) {
                    if (mDownloadListener != null)
                        mDownloadListener.onDownloadUpdate(0,0,"","",i);
                    Log.i(TAG, "onProgressUpdate: ");
                }

                @Override
                public void onDownloadFinished() {
                    if (mDownloadListener != null)
                        mDownloadListener.onDownloadFinish(0,0,"","");
                    Log.i(TAG, "onDownloadFinished: ");
                }

                @Override
                public void onDownloadFailed() {
                    if (mDownloadListener != null)
                        mDownloadListener.onDownloadFail(0,0,"","");
                    Log.i(TAG, "onDownloadFailed: ");
                }

                @Override
                public void onInstalled() {
                    if (mDownloadListener != null)
                        mDownloadListener.onInstalled(0,0,"","");
                    Log.i(TAG, "onInstalled: ");
                }
            });

            mKwadNativeAd.setRenderType(TPBaseAd.AD_TYPE_NORMAL_NATIVE);
            downloadAndCallback(mKwadNativeAd, mNeedDownloadImg);
        }


    }

    @Override
    public void clean() {
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_KUAISHOU);
    }

    @Override
    public String getNetworkVersion() {
        return KsAdSDK.getSDKVersion();
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams, Map<String, Object> userParams) {
        KuaishouInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "getBiddingToken onSuccess: ");

            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });
        return KsAdSDK.getLoadManager().getBidRequestTokenV2(new KsScene.Builder(0).build());
    }
}
