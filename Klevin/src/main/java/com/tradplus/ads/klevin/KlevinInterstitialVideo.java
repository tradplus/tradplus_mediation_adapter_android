package com.tradplus.ads.klevin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.klevin.KlevinManager;
import com.tencent.klevin.ads.ad.RewardAd;
import com.tencent.klevin.ads.ad.RewardAdRequest;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class KlevinInterstitialVideo extends TPRewardAdapter {

    private String placementId;
    private int mPostId;
    private RewardAd mRewardAd;
    private KlevinInterstitialCallbackRouter mCallbackRouter;
    private boolean isVideoSoundEnable = true; // true（自动播放时静音）; 1 :静音
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private int rewardTime = 30; // 官方给出默认值
    private int rewardTrigger = RewardAdRequest.TRIGGER_OTHER; // 官方给出默认值
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private int ecpmLevel;
    public static final String TAG = "KlevinInterstitialVideo";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        String videoMute;
        if (extrasAreValid(tpParams)) {
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            // 视频静音 指定自动播放时是否静音: 1 自动播放时静音；2 自动播放时有声
            videoMute = tpParams.get(AppKeyManager.VIDEO_MUTE);

            if (!TextUtils.isEmpty(placementId)) {
                mPostId = Integer.parseInt(placementId);
            }

            if (!TextUtils.isEmpty(videoMute)) {
                if (!AppKeyManager.VIDEO_MUTE_YES.equals(videoMute)) {
                    isVideoSoundEnable = false; // true（自动播放时静音）；false(自动播放有声)
                    Log.i(TAG, "isVideoSoundEnable: " + isVideoSoundEnable);
                }
            }

            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }

        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("", ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                if (mLoadAdapterListener != null)
                    mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(KlevinConstant.KLEVIN_REWARD_TIME)) {
                Object object = userParams.get(KlevinConstant.KLEVIN_REWARD_TIME);
                rewardTime = Integer.parseInt(String.valueOf(object));
                Log.i(TAG, "rewardTime: " + rewardTime);
            }

            if (userParams.containsKey(KlevinConstant.KLEVIN_REWARD_TRIGGER)) {
                Object object = userParams.get(KlevinConstant.KLEVIN_REWARD_TRIGGER);
                rewardTrigger = Integer.parseInt(String.valueOf(object));
                Log.i(TAG, "rewardTrigger: " + rewardTrigger);
            }
        }

//        mPostId = 30031;

        mCallbackRouter = KlevinInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(placementId, mLoadAdapterListener);

        KlevinInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitialVideo();
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

    private void requestInterstitialVideo() {
        if (isC2SBidding && isBiddingLoaded) {
            if (mRewardAd != null && mCallbackRouter != null &&
                    mCallbackRouter.getListener(placementId) != null) {
                setNetworkObjectAd(mRewardAd);
                mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
            }
            return;
        }

        Log.i(TAG, "requestInterstitialVideo: " + placementId);
        RewardAdRequest.Builder rewardAdBuilder = new RewardAdRequest.Builder();
        rewardAdBuilder.autoMute(isVideoSoundEnable)//【可选】设置是否自动静音，默认false【有声】，true【静音】
                .setRewardTime(rewardTime)//【可选】设置激励卡秒，激励发放时间。不配置时默认为视频长度。必须大于3s
                .setRewardTrigger(rewardTrigger)//【可选】激励类型
                .setPosId(mPostId)//【必须】激励视频广告位id
                .setAdCount(1);//【可选】广告个数，默认1


        RewardAd.load(rewardAdBuilder.build(), new RewardAd.RewardAdLoadListener() {

            public void onAdLoadError(int err, String msg) {
                // 加载失败，err是错误码，msg是描述信息
                Log.i(TAG, "onAdLoadError err: " + err + " " + msg);
                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        onC2STokenListener.onC2SBiddingFailed(err + "", msg);
                    }
                    return;
                }

                if (mCallbackRouter.getListener(placementId) != null)
                    mCallbackRouter.getListener(placementId)
                            .loadAdapterLoadFailed(KlevinErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, err, msg));

            }

            public void onVideoPrepared(RewardAd ad) {
                // 2.1开始支持在线播放功能，该回调在2.1版本开始引入
                // 广告信息已解析完成，参数ad为激励视频广告实例
                // 在此回调中触发展示广告，则优先使用在线播放的方式加载广告
                Log.i(TAG, "onVideoPrepared");
                mRewardAd = ad;

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        ecpmLevel = mRewardAd.getECPM();
                        Log.i(TAG, "激励视频 bid price: " + ecpmLevel);
                        if (TextUtils.isEmpty(ecpmLevel + "")) {
                            onC2STokenListener.onC2SBiddingFailed("", "ecpmLevel is empty");
                            return;
                        }
                        onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                    }
                    isBiddingLoaded = true;
                    return;
                }

                if (mCallbackRouter.getListener(placementId) != null) {
                    setNetworkObjectAd(ad);
                    mCallbackRouter.getListener(placementId).loadAdapterLoaded(null);
                }

            }

            public void onAdLoaded(RewardAd ad) {
                // 广告素材下载完成，参数ad为激励视频广告实例
                // 在此回调中触发展示广告，则使用本地播放的方式读取广告
//                Log.i(TAG, "onAdLoaded");
//                mRewardAd = ad;
            }
        });
    }

    @Override
    public void showAd() {
        if (mShowListener != null && mCallbackRouter != null) {
            mCallbackRouter.addShowListener(placementId, mShowListener);
        }

        if (mRewardAd != null && mRewardAd.isValid()) {
            //isValid接口2.1以上版本支持, 2.1以下移除调用。
            //设置激励视频广告展示回调
            mRewardAd.setListener(new RewardAd.RewardAdListener() {
                @Override
                public void onAdSkip() { //用户跳过广告回调
                    Log.i(TAG, "onAdSkip");
                    alwaysRewardUser = false;
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId).onRewardSkip();

                }

                public void onReward() { //激励发放回调
                    hasGrantedReward = true;
                }

                public void onVideoComplete() { //视频播放完成回调
                    Log.i(TAG, "onVideoComplete");
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId).onAdVideoEnd();
                }

                public void onAdShow() { //广告曝光回调
                    Log.i(TAG, "onAdShow");
                    if (mCallbackRouter.getShowListener(placementId) != null) {
                        mCallbackRouter.getShowListener(placementId).onAdShown();
                        mCallbackRouter.getShowListener(placementId).onAdVideoStart();
                    }
                }

                public void onAdClick() { //广告点击回调
                    Log.i(TAG, "onAdClick");
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId).onAdVideoClicked();
                }

                public void onAdClosed() { //广告关闭回调
                    if (mCallbackRouter.getShowListener(placementId) != null) {
                        if (hasGrantedReward || alwaysRewardUser) {
                            Log.i(TAG, "onReward");
                            mCallbackRouter.getShowListener(placementId).onReward();
                        }
                        Log.i(TAG, "onAdClosed");
                        mCallbackRouter.getShowListener(placementId).onAdClosed();
                    }

                }

                public void onAdError(int err, String msg) {
                    //广告展示失败回调
                    Log.i(TAG, "onAdError err: " + err + " " + msg);
                    if (mCallbackRouter.getShowListener(placementId) != null)
                        mCallbackRouter.getShowListener(placementId)
                                .onAdVideoError(KlevinErrorUtil.getTradPlusErrorCode(SHOW_FAILED, err, msg));
                }

                @Override
                public void onAdDetailClosed(int i) {

                }
            });

            if (ecpmLevel > 0) {
                Log.i(TAG, "sendWinNotificationWithPrice: " + ecpmLevel);
                mRewardAd.sendWinNotificationWithPrice(ecpmLevel);
            }

            mRewardAd.show(); //展示激励视频广告
        } else {
            if (mCallbackRouter.getShowListener(placementId) != null)
                mCallbackRouter.getShowListener(placementId).onAdVideoError(new TPError(UNSPECIFIED));
        }
    }

    @Override
    public boolean isReady() {
        return mRewardAd != null && mRewardAd.isValid();
    }

    @Override
    public void clean() {
        super.clean();
        if (mRewardAd != null) {
            mRewardAd.setListener(null);
            mRewardAd = null;
        }

        if (mCallbackRouter != null)
            mCallbackRouter.removeListeners(placementId);

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
