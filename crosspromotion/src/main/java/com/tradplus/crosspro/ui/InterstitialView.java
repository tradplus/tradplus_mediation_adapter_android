package com.tradplus.crosspro.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.common.TaskUtils;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.common.util.ResourceUtils;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.CommonUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;
import com.tradplus.ads.pushcenter.event.request.EventShowEndRequest;
import com.tradplus.crosspro.manager.CPAdManager;
import com.tradplus.crosspro.manager.CPAdMessager;
import com.tradplus.crosspro.manager.CPClickController;
import com.tradplus.crosspro.network.base.CPError;

import java.util.ArrayList;
import java.util.List;

import static com.tradplus.ads.base.common.TPError.EC_NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.EC_NOTREADY;
import static com.tradplus.ads.base.common.TPError.EC_PLAY_VIDEO_FAILED;
import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;
import static com.tradplus.ads.base.common.TPError.EC_VIDEO_MISSING;
import static com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils.EventPushStats.EV_CLICK_PUSH_FAILED;
import static com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils.EventPushStats.EV_SHOW_PUSH_FAILED;
import static com.tradplus.crosspro.common.CPConst.ENDCARDCLICKAREA_FULLSCREEN;
import static com.tradplus.crosspro.network.base.CPErrorCode.noADError;
import static com.tradplus.crosspro.network.base.CPErrorCode.rewardedVideoPlayError;
import static com.tradplus.crosspro.network.base.CPErrorCode.rewardedVideoPlayVideoMissing;
import static com.tradplus.crosspro.network.base.CPErrorCode.timeOutError;

public class InterstitialView extends LinearLayout {
    private static final String TAG = CPAdActivity.class.getSimpleName();
    private RelativeLayout mRoot;
    private Context context;
    private CPAdMessager.OnEventListener mListener;
    private OnViewFinish onViewFinish;

    public int getmScreenWidth() {
        return mScreenWidth;
    }

    public void setmScreenWidth(int mScreenWidth) {
        this.mScreenWidth = mScreenWidth;
    }

    public int getmScreenHeight() {
        return mScreenHeight;
    }

    public void setmScreenHeight(int mScreenHeight) {
        this.mScreenHeight = mScreenHeight;
    }

    public CPAdResponse getCpAdResponse() {
        return cpAdResponse;
    }

    public void setCpAdResponse(CPAdResponse cpAdResponse) {
        this.cpAdResponse = cpAdResponse;
    }


    public int getmOrientation() {
        return mOrientation;
    }

    public void setmOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    public String getAdSourceId() {
        return adSourceId;
    }

    public void setAdSourceId(String adSourceId) {
        this.adSourceId = adSourceId;
    }

    public boolean isInterstitial() {
        return isInterstitial;
    }

    public void setInterstitial(boolean interstitial) {
        isInterstitial = interstitial;
    }

    public PlayerView getmPlayerView() {
        return mPlayerView;
    }

    public void setmPlayerView(PlayerView mPlayerView) {
        this.mPlayerView = mPlayerView;
    }

    public CPClickController getCpClickController() {
        return cpClickController;
    }

    public void setCpClickController(CPClickController cpClickController) {
        this.cpClickController = cpClickController;
    }

    public boolean isShowEndCard() {
        return isShowEndCard;
    }

    public void setShowEndCard(boolean showEndCard) {
        isShowEndCard = showEndCard;
    }

    public int getVideoPlayFinish() {
        return videoPlayFinish;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setVideoPlayFinish(int videoPlayFinish) {
        this.videoPlayFinish = videoPlayFinish;
    }

    public int getVideoPlayCompletion() {
        return videoPlayCompletion;
    }

    public void setVideoPlayCompletion(int videoPlayCompletion) {
        this.videoPlayCompletion = videoPlayCompletion;
    }

    private int mScreenWidth, mScreenHeight;
    private CPAdResponse cpAdResponse;
    private int mOrientation;
    private int mfullScreen;
    private String adSourceId;
    private long timeStamp;
    private boolean isInterstitial;
    private EndCardView endCardView;
    private RelativeLayout unFullRoot;
    private boolean isClicking;
    private PlayerView mPlayerView;
    private CPClickController cpClickController;
    private LoadingView mLoadingView;
    private boolean isShowEndCard;
    private int direction;

    public InterstitialView(Context context) {
        super(context);
        this.context = context;
    }

    public InterstitialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public InterstitialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void initView() {

        inflate(context, ResourceUtils.getLayoutIdByName(context, "cp_activity_ad"), this);

        init();
    }

    private void init() {
        mRoot = findViewById(CommonUtil.getResId(context, "cp_rl_root", "id"));

        mListener = CPAdMessager.getInstance().getListener(cpAdResponse.getKey() + timeStamp);
        if (TextUtils.isEmpty(cpAdResponse.getVideo_url())) {
            showEndCard();
            EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);
            notifyShow();
        } else {
            if (isShowEndCard) {
                showEndCard();

            } else {
                initPlayer();

            }
        }

        initBannerView();
        initEndCardBannerView();
    }


    private void initBannerView() {
        if (mfullScreen == 1 && isInterstitial()) {
            bannerView = new BannerView(context, new BannerView.OnBannerClickListener() {
                @Override
                public void onClick() {
                    InterstitialView.this.onClick();
                }
            });
            bannerView.initView(mRoot, cpAdResponse);
        }
    }
    private BannerView bannerView;
    private EndCardBannerView endCardBannerView;
    private void initEndCardBannerView() {
        if (mfullScreen == 1 && isInterstitial()) {
            endCardBannerView = new EndCardBannerView(context, new BannerView.OnBannerClickListener() {
                @Override
                public void onClick() {
                    InterstitialView.this.onClick();
                }
            });
            endCardBannerView.initView(mRoot, cpAdResponse);
        }
    }


    private void hideLoading() {
        if (mLoadingView != null) {
            mLoadingView.hide();
        }
    }

    int videoPlayFinish = 0;
    int videoPlayCompletion = 0;

    private List<String> replanceTrackIds(List<String> list) {
        List<String> _list = new ArrayList<>();
        TPDataManager tpDataManager = TPDataManager.getInstance();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                String url = list.get(i).replace(
                        "__TP_REQ_ID__", tpDataManager.getIds(adSourceId).getRequest_id())
                        .replace("__TP_IMP_ID__", tpDataManager.getIds(adSourceId).getImpression_id())
                        .replace("__TP_CLK_ID__", tpDataManager.getIds(adSourceId).getClick_id());
                LogUtil.ownShow("cross pro url = " + url);
                _list.add(url);
            }
        }
        return _list;
    }

    private void initPlayer() {
        if (mfullScreen == 1) {
            mRoot.setBackgroundColor(this.getResources().getColor(android.R.color.black));
        }
        mPlayerView = new PlayerView(mRoot, new PlayerView.OnPlayerListener() {
            @Override
            public void onVideoPlayStart() {
                LogUtil.ownShow("onVideoPlayStart...");
                EventSendMessageUtil.getInstance().sendAdVideoStart(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId);
                EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);

                notifyShow();
                notifyVideoPlayStart();
            }

            @Override
            public void onVideoUpdateProgress(int progress) {
//                if (mBannerView == null && mShowBannerTime >= 0 && progress >= mShowBannerTime) {
//                    showBannerView();
//                }
            }

            @Override
            public void onVideoPlayEnd() {
                //视频关闭
                videoPlayCompletion = 1;
                LogUtil.ownShow("onVideoPlayEnd...");
//                if (videoPlayFinish == 0)
//                    EventSendMessageUtil.getInstance().sendAdVideoClose(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), IC_NOCOMPLETED, adSourceId);

            }

            @Override
            public void onVideoPlayCompletion() {
                videoEnd(false);
            }

            @Override
            public void onVideoShowFailed(CPError error) {
                notifyShowFailedAndFinish(error);
                Log.i(TAG, "onVideoShowFailed: errorCode :" + error.getCode() + ", errorMsg :" + error.getDesc());
                showFailed(error);

            }

            @Override
            public void onVideoPlayProgress(int progressArea) {
                switch (progressArea) {
                    case 25:
                        LogUtil.ownShow("onVideoProgress25.......");
                        EventSendMessageUtil.getInstance().sendAdVideoProgress25(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId);
                        break;
                    case 50:
                        LogUtil.ownShow("onVideoProgress50.......");
                        EventSendMessageUtil.getInstance().sendAdVideoProgress50(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId);
                        break;
                    case 75:
                        LogUtil.ownShow("onVideoProgress75.......");
                        EventSendMessageUtil.getInstance().sendAdVideoProgress75(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId);
                        break;
                }
            }

            @Override
            public void onVideoCloseClick() {
                if (mPlayerView != null) {
                    mPlayerView.stop();
                }

                showEndCard();
            }

            @Override
            public void onVideoClick() {
                if (cpAdResponse != null && cpAdResponse.getVideo_click() == 1) {
                    onClick();
                }
            }

            @Override
            public void onVideoSkip() {
                mPlayerView.stop();
                videoEnd(true);

            }
        }, TradPlus.invoker().getChinaHandler() != null,isInterstitial);

        mPlayerView.setSetting(cpAdResponse);
        mPlayerView.load(cpAdResponse.getVideo_url());
    }

    private void videoEnd(boolean isSkip){
        videoPlayFinish = 1;
        videoPlayCompletion = 1;
        //视频播放完成
        LogUtil.ownShow("onVideoPlayCompletion...");
        if (mListener != null && !isSkip) {
            mListener.onVideoPlayEnd();
        }

        if (mListener != null) {
//            EventSendMessageUtil.getInstance().sendAdVideoClose(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), IC_COMPLETED, adSourceId);
            if (!isInterstitial)
                EventSendMessageUtil.getInstance().sendAdVideoReward(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId);
            mListener.onReward();
        }
        showEndCard();
    }

    private void showFailed(CPError error) {
        if (error.getCode().equals(rewardedVideoPlayVideoMissing)) {
            EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_VIDEO_MISSING, adSourceId);
        } else if (error.getCode().equals(rewardedVideoPlayError)) {
            EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_PLAY_VIDEO_FAILED, adSourceId);
        } else if (error.getCode().equals(timeOutError)) {
            EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_NETWORK_TIMEOUT, adSourceId);
        } else if (error.getCode().equals(noADError)) {
            EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_NOTREADY, adSourceId);
        }
    }


    private void notifyShow() {
        if (cpAdResponse != null && context != null) {
            sendTrackStart(context, false);
            EventShowEndRequest _eventShowEndRequest = new EventShowEndRequest(context, EV_SHOW_PUSH_FAILED.getValue());
            _eventShowEndRequest.setCampaign_id(cpAdResponse.getCampaign_id());
            _eventShowEndRequest.setAd_id(cpAdResponse.getAd_id());
            _eventShowEndRequest.setAsu_id(adSourceId);
            EventSendMessageUtil.getInstance().pushTrackToServer(context, replanceTrackIds(cpAdResponse.getImp_track_url_list()), _eventShowEndRequest);
        }
        if (mListener != null) {
            mListener.onShow();
        }

    }

    private void notifyVideoPlayStart() {
        if (mListener != null) {
            mListener.onVideoPlayStart();
        }
    }

    private void notifyShowFailedAndFinish(CPError error) {
        if (mListener != null) {
            mListener.onVideoShowFailed(error);
        }
        if(onViewFinish != null){
            onViewFinish.onFinish();
        }
    }

    private void showEndCard() {
        LogUtil.ownShow("showEndCard.......");
        isShowEndCard = true;
        endCardView = new EndCardView(mRoot, mScreenWidth, mScreenHeight, cpAdResponse, mOrientation, new EndCardView.OnEndCardListener() {
            @Override
            public void onClickEndCard() {
                LogUtil.ownShow("onClickEndCard: ");

                if (cpAdResponse != null && TextUtils.equals(cpAdResponse.getEnd_card_click_area(), ENDCARDCLICKAREA_FULLSCREEN)) {
                    onClick();
                }
            }

            @Override
            public void onCloseEndCard() {
                LogUtil.ownShow("onCloseEndCard.......");
                if (mListener != null) {
                    mListener.onClose();
                }
                if(onViewFinish != null){
                    onViewFinish.onFinish();
                }

            }
        },direction);

        if (mPlayerView != null) {
            mRoot.removeView(mPlayerView);
            mPlayerView = null;
        }
        if(mfullScreen == 1 && isInterstitial()) {
            if(bannerView != null) {
                bannerView.setVisibility(GONE);
            }
            if (endCardBannerView != null && endCardBannerView.canShow()) {
                endCardBannerView.setVisibility(VISIBLE);
            }
        }
    }

    public OnViewFinish getOnViewFinish() {
        return onViewFinish;
    }

    public void setOnViewFinish(OnViewFinish onViewFinish) {
        this.onViewFinish = onViewFinish;
    }

    public void setMfullScreen(int mfullScreen) {
        this.mfullScreen = mfullScreen;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public interface OnViewFinish{
        void onFinish();
    }

    private void sendTrackStart(Context context, boolean isClick) {
        List<String> _list = replanceTrackIds(isClick ? cpAdResponse.getClick_track_url_list() : cpAdResponse.getImp_track_url_list());
        if (_list != null) {
            for (int i = 0; i < _list.size(); i++) {
                EventSendMessageUtil.getInstance().sendThirdCheckStart(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId, isClick, _list.get(i));
            }
        }

    }

    private void onClick() {
        LogUtil.ownShow("click 。。。。。");
        EventSendMessageUtil.getInstance().sendClickAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adSourceId);
        if (isClicking) {
            LogUtil.ownShow("during click 。。。。。");
            return;
        }
        if (cpAdResponse == null) {
            return;
        }

        if (mListener != null) {
            mListener.onClick();

        }
        if (cpAdResponse != null && context != null) {
            sendTrackStart(context, true);
            EventShowEndRequest _eventShowEndRequest = new EventShowEndRequest(context, EV_CLICK_PUSH_FAILED.getValue());
            _eventShowEndRequest.setCampaign_id(cpAdResponse.getCampaign_id());
            _eventShowEndRequest.setAd_id(cpAdResponse.getAd_id());
            _eventShowEndRequest.setAsu_id(adSourceId);
            EventSendMessageUtil.getInstance().pushTrackToServer(context, replanceTrackIds(cpAdResponse.getClick_track_url_list()), _eventShowEndRequest);
        }

        cpClickController = new CPClickController(context, cpAdResponse, adSourceId);
        //TODO requestid
        cpClickController.startClick("", new CPClickController.ClickStatusCallback() {
            @Override
            public void clickStart() {
                isClicking = true;
                showLoading();
            }

            @Override
            public void clickEnd() {
                isClicking = false;
                TaskUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                    }
                });
            }

            @Override
            public void downloadApp(final String url) {

                TaskUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                        CPAdManager.getInstance(context).startDownloadApp(cpAdResponse.getCampaign_id(), cpAdResponse, url, adSourceId);
                    }
                });
            }
        });

    }


    private void showLoading() {
        if (mLoadingView == null) {
            mLoadingView = new LoadingView(mRoot);
        }
        mLoadingView.startLoading();
    }


}
