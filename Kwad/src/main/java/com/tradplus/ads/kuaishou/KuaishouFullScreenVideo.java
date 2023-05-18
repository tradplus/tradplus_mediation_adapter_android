package com.tradplus.ads.kuaishou;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsFullScreenVideoAd;
import com.kwad.sdk.api.KsInterstitialAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.kwad_ads.KuaishouInitManager;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.List;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class KuaishouFullScreenVideo extends TPInterstitialAdapter {

    private String placementId, mBidResponseV2;
    private Integer direction;
    private KuaishouInterstitialCallbackRouter mRouter;
    private KsFullScreenVideoAd mFullScreenVideoAd;
    private KsInterstitialAd mKsInterstitialAd;
    private boolean isVideoSoundEnable = true;
    private int isFlullSreenVideoAd;
    private static final String TAG = "KuaishouFullScreenVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mBidResponseV2 = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            String videoMute = tpParams.get(AppKeyManager.VIDEO_MUTE);
            String direct = tpParams.get(AppKeyManager.DIRECTION);
            String fullSreenTpye = tpParams.get(AppKeyManager.FULL_SCREEN_TYPE);

            if (!TextUtils.isEmpty(videoMute)) {
                if (videoMute.equals(AppKeyManager.VIDEO_MUTE_YES)) {
                    isVideoSoundEnable = false;
                }
            }

            if (!TextUtils.isEmpty(fullSreenTpye)) {
                isFlullSreenVideoAd = Integer.parseInt(fullSreenTpye);
            }

            if (!TextUtils.isEmpty(direct)) {
                direction = Integer.valueOf(direct);
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }



        mRouter = KuaishouInterstitialCallbackRouter.getInstance();
        mRouter.addListener(placementId, mLoadAdapterListener);

        KuaishouInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial();
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
        KsScene.Builder builder = new KsScene.Builder(Long.parseLong(placementId));

        if (!TextUtils.isEmpty(mBidResponseV2)) {
            builder.setBidResponseV2(mBidResponseV2);
        }

        return builder.build();
    }

    private void requestInterstitial() {
        if (isFlullSreenVideoAd == AppKeyManager.FULL_TYPE) {
            mFullScreenVideoAd = null;
            KsAdSDK.getLoadManager().loadFullScreenVideoAd(getKsScene(), new KsLoadManager.FullScreenVideoAdListener() {
                @Override
                public void onError(int code, String msg) {
                    Log.i(TAG, "onError:  code ：" + code + ", msg :" + msg);
                    if (mRouter.getListener(placementId) != null) {
                        mRouter.getListener(placementId).loadAdapterLoadFailed(KuaishouErrorUtil.geTpMsg(NETWORK_NO_FILL, code, msg));
                    }

                }

                @Override
                public void onFullScreenVideoResult(List<KsFullScreenVideoAd> list) {
                    Log.i(TAG, "onRequestResult: ");
                }

                @Override
                public void onFullScreenVideoAdLoad(List<KsFullScreenVideoAd> adList) {
                    if (adList != null && adList.size() > 0) {
                        mFullScreenVideoAd = adList.get(0);
                        Log.i(TAG, "onFullScreenVideoAdLoad: ");
                        if (mRouter.getListener(placementId) != null) {
                            setFirstLoadedTime();
                            setNetworkObjectAd(mFullScreenVideoAd);
                            mRouter.getListener(placementId).loadAdapterLoaded(null);
                        }
                    } else {
                        Log.i(TAG, "onFullScreenVideoAdLoad,but adList < 0");
                        if (mRouter.getListener(placementId) != null) {
                            TPError tpError = new TPError(NETWORK_NO_FILL);
                            tpError.setErrorMessage("onFullScreenVideoAdLoad,but adList < 0");
                            mRouter.getListener(placementId).loadAdapterLoadFailed(tpError);
                        }
                    }
                }
            });

        } else {
            mKsInterstitialAd = null;
            // InterstitialAd
            KsAdSDK.getLoadManager().loadInterstitialAd(getKsScene(), new KsLoadManager.InterstitialAdListener() {
                @Override
                public void onError(int code, String msg) {
                    Log.i(TAG, "onError:  code ：" + code + ", msg :" + msg);
                    if (mRouter.getListener(placementId) != null) {
                        mRouter.getListener(placementId).loadAdapterLoadFailed(KuaishouErrorUtil.geTpMsg(NETWORK_NO_FILL, code, msg));
                    }
                }

                @Override
                public void onRequestResult(int i) {
                    Log.i(TAG, "onRequestResult: ");
                }

                @Override
                public void onInterstitialAdLoad(List<KsInterstitialAd> list) {
                    Log.i(TAG, "onInterstitialAdLoad: ");
                    if (list != null && list.size() > 0) {
                        mKsInterstitialAd = list.get(0);
                        Log.i(TAG, "onFullScreenVideoAdLoad: ");
                        if (mRouter.getListener(placementId) != null) {
                            setFirstLoadedTime();
                            setNetworkObjectAd(mKsInterstitialAd);
                            mRouter.getListener(placementId).loadAdapterLoaded(null);
                        }
                    }
                }
            });

        }

    }

    @Override
    public void showAd() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        if (mShowListener != null) {
            mRouter.addShowListener(placementId, mShowListener);
        }

        KsVideoPlayConfig ksVideoPlayConfig = videoPlayConfig(activity);
        if (isFlullSreenVideoAd == AppKeyManager.FULL_TYPE) {
            Log.i(TAG, "showAd : ");
            if (mFullScreenVideoAd != null && mFullScreenVideoAd.isAdEnable()) {
                mFullScreenVideoAd.setFullScreenVideoAdInteractionListener(mFsLinstener);
                mFullScreenVideoAd.showFullScreenVideoAd(activity, ksVideoPlayConfig);
            } else {
                if (mRouter.getShowListener(placementId) != null)
                    mRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        } else {
            Log.i(TAG, "showAd : ");
            if (mKsInterstitialAd != null) {
                mKsInterstitialAd.setAdInteractionListener(mAdInteractionListener);
                mKsInterstitialAd.showInterstitialAd(activity, ksVideoPlayConfig);
            } else {
                if (mRouter.getShowListener(placementId) != null)
                    mRouter.getShowListener(placementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }


    private KsVideoPlayConfig videoPlayConfig(Activity activity) {
        KsVideoPlayConfig.Builder builder = new KsVideoPlayConfig.Builder();
        if (direction == 1 || direction == 2) {
            builder.showLandscape(direction == 2);
        } else {
            int ori = activity.getResources().getConfiguration().orientation;
            builder.showLandscape(ori == ORIENTATION_LANDSCAPE);
        }
        Log.i(TAG, "videoSoundEnable: " + isVideoSoundEnable);
        builder.videoSoundEnable(isVideoSoundEnable);
        return builder.build();
    }

    private final KsInterstitialAd.AdInteractionListener mAdInteractionListener = new KsInterstitialAd.AdInteractionListener() {
        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdShow() {
            Log.i(TAG, "onAdShow: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdShown();
            }
        }

        @Override
        public void onAdClosed() {
            Log.i(TAG, "onAdClosed: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdClosed();
            }
        }

        @Override
        public void onPageDismiss() {
        }

        @Override
        public void onVideoPlayError(int code, int i1) {
            Log.i(TAG, "onVideoPlayError: code :" + code);
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoError(KuaishouErrorUtil.getTradPlusErrorCode(code));
            }
        }

        @Override
        public void onVideoPlayEnd() {
            Log.i(TAG, "onVideoPlayEnd: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoEnd();
            }
        }

        @Override
        public void onVideoPlayStart() {
            Log.i(TAG, "onVideoPlayStart: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoStart();
            }
        }

        @Override
        public void onSkippedAd() {
            Log.i(TAG, "onSkippedAd: ");
        }
    };

    private final KsFullScreenVideoAd.FullScreenVideoAdInteractionListener mFsLinstener = new KsFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onPageDismiss() {
            Log.i(TAG, "onPageDismiss: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdClosed();
            }
        }

        @Override
        public void onVideoPlayError(int code, int i1) {
            Log.i(TAG, "onVideoPlayError: code :" + code);
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoError(KuaishouErrorUtil.getTradPlusErrorCode(code));
            }
        }

        @Override
        public void onVideoPlayEnd() {
            Log.i(TAG, "onVideoPlayEnd: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoEnd();
            }
        }

        @Override
        public void onVideoPlayStart() {
            Log.i(TAG, "onVideoPlayStart: ");
            if (mRouter.getShowListener(placementId) != null) {
                mRouter.getShowListener(placementId).onAdVideoStart();
                mRouter.getShowListener(placementId).onAdShown();
            }
        }

        @Override
        public void onSkippedVideo() {

        }
    };

    @Override
    public boolean isReady() {
        if (isFlullSreenVideoAd == AppKeyManager.FULL_TYPE) {
            return mFullScreenVideoAd != null && !isAdsTimeOut() && mFullScreenVideoAd.isAdEnable();
        }else {
            return mKsInterstitialAd != null && !isAdsTimeOut();
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mFullScreenVideoAd != null) {
            mFullScreenVideoAd.setFullScreenVideoAdInteractionListener(null);
            mFullScreenVideoAd = null;
        }

        if (mKsInterstitialAd != null) {
            mKsInterstitialAd.setAdInteractionListener(null);
            mKsInterstitialAd = null;
        }

        if (placementId != null)
            mRouter.removeListeners(placementId);
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
