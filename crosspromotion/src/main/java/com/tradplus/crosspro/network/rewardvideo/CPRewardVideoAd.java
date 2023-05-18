package com.tradplus.crosspro.network.rewardvideo;

import static com.tradplus.ads.base.common.TPError.EC_ENDCARD_MISSING;
import static com.tradplus.ads.base.common.TPError.EC_SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;
import static com.tradplus.ads.base.common.TPError.EC_UNSPECIFIED;
import static com.tradplus.ads.base.common.TPError.EC_VIDEO_MISSING;
import static com.tradplus.ads.base.common.TPError.IF_NOFILL;
import static com.tradplus.ads.base.util.TradPlusDataConstants.SCREEN_LAND_TYPE;
import static com.tradplus.ads.base.util.TradPlusDataConstants.SCREEN_PORT_TYPE;
import static com.tradplus.crosspro.common.CPConst.DEFAULT_EXPRETIME;
import static com.tradplus.crosspro.common.CPConst.FORMAT.REWARDEDVIDEO_FORMAT;
import static com.tradplus.crosspro.network.base.CPErrorCode.noADError;

import android.content.Context;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.event.TPPushCenter;
import com.tradplus.ads.common.util.DeviceUtils;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.network.CPErrorUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;
import com.tradplus.ads.pushcenter.event.request.EventLoadEndRequest;
import com.tradplus.ads.pushcenter.event.request.EventReadyRequest;
import com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.tradplus.crosspro.manager.CPAdConfigController;
import com.tradplus.crosspro.manager.CPAdManager;
import com.tradplus.crosspro.manager.CPAdMessager;
import com.tradplus.crosspro.manager.resource.CPLoader;
import com.tradplus.crosspro.manager.resource.CPResourceStatus;
import com.tradplus.crosspro.network.base.CPBaseAd;
import com.tradplus.crosspro.network.base.CPError;
import com.tradplus.crosspro.network.base.CPErrorCode;
import com.tradplus.crosspro.ui.CPAdActivity;

public class CPRewardVideoAd extends CPBaseAd {

    private CPRewardVideoAdListener cpRewardVideoAdListener;
    private String mAdId;
    private int direction;

    public CPRewardVideoAd(Context context, String campaignId, String adSourceId) {
        super(context, campaignId, adSourceId);
    }

    @Override
    public void load() {
        LogUtil.ownShow("OpenAPIStart...");

        CPAdConfigController cpAdConfigController = new CPAdConfigController();
        cpAdConfigController.setOnConfigListener(new CPAdConfigController.OnConfigListener() {
            @Override
            public void onSuccess(String pid) {
                mAdId = CPAdManager.getInstance(getContext()).getCpAdConfig(campaignId).getAd_id();
                TPDataManager.getInstance().putIds(adSourceId);
                //5700
                EventSendMessageUtil.getInstance().sendLoadAdNetworkStart(getContext(), campaignId, adSourceId);

                CPAdManager.getInstance(getContext()).load(campaignId, new CPLoader.CPLoaderListener() {
                    @Override
                    public void onSuccess() {
                        if (cpRewardVideoAdListener != null) {
                            cpRewardVideoAdListener.onInterstitialLoaded();
                        }

                    }

                    @Override
                    public void onFailed(CPError msg) {
                        if (cpRewardVideoAdListener != null) {
                            cpRewardVideoAdListener.onInterstitialFailed(CPErrorUtil.getTradPlusErrorCode(msg));
                        }
                    }
                }, adSourceId);

            }

            @Override
            public void onError(int code, String msg) {
                if (cpRewardVideoAdListener != null) {
                    //5100
                    EventSendMessageUtil.getInstance().sendOpenAPIStart(getContext(), "", adSourceId, "");
                    //5700
                    EventSendMessageUtil.getInstance().sendLoadAdNetworkStart(getContext(), campaignId, adSourceId);
                    //5800 配置获取失败，无法拿到adid
                    EventLoadEndRequest eventLoadEndRequest = new EventLoadEndRequest(getContext(), EventPushMessageUtils.EventPushStats.EV_LOAD_AD_END.getValue());
                    eventLoadEndRequest.setCampaign_id(campaignId);
                    eventLoadEndRequest.setAsu_id(adSourceId);

                    eventLoadEndRequest.setError_code(TPError.parseErrorCode(code));

                    long loadTime = RequestUtils.getInstance().countRuntime(eventLoadEndRequest.getCreateTime());
                    eventLoadEndRequest.setLoad_time(loadTime + "");
                    TPPushCenter.getInstance().saveCrossEvent(eventLoadEndRequest);
                    cpRewardVideoAdListener.onInterstitialFailed(CPErrorUtil.getErrorCode(code, msg));
                }
            }
        });
        cpAdConfigController.loadConfig(getContext(), campaignId, adSourceId, REWARDEDVIDEO_FORMAT, 0);


    }

    public long getExpreTime() {
        CPAdResponse _cpAdResponse = CPAdManager.getInstance(getContext()).getCpAdConfig(campaignId);
        if (_cpAdResponse != null) {
            if (_cpAdResponse.getAd_expire_time() > 0) {
                return _cpAdResponse.getAd_expire_time() * 1000;
            }
        }
        return DEFAULT_EXPRETIME;
    }

    @Override
    public void show() {
        //6000
        EventSendMessageUtil.getInstance().sendShowAdStart(getContext(), campaignId, mAdId, adSourceId);

        try {
            CPAdResponse _cpAdResponse = CPAdManager.getInstance(getContext()).getCpAdConfig(campaignId);
            if (mContext == null || _cpAdResponse == null) {
                if (cpRewardVideoAdListener != null) {
                    cpRewardVideoAdListener.onVideoShowFailed(CPErrorCode.get(noADError, CPErrorCode.fail_null_context));
                    EventSendMessageUtil.getInstance().sendShowEndAd(getContext(), campaignId, mAdId, EC_SHOW_FAILED, adSourceId);
                }
                return;
            }

            long timeStamp = System.currentTimeMillis();
            CPAdMessager.getInstance().setListener(_cpAdResponse.getKey() + timeStamp, new CPAdMessager.OnEventListener() {
                @Override
                public void onShow() {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onInterstitialShown();
                    }
                }

                @Override
                public void onVideoShowFailed(CPError error) {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onVideoShowFailed(error);
                    }
                }

                @Override
                public void onVideoPlayStart() {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onVideoAdPlayStart();
                    }
                }

                @Override
                public void onVideoPlayEnd() {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onVideoAdPlayEnd();
                    }
                }

                @Override
                public void onReward() {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onRewarded();
                    }
                }

                @Override
                public void onClose() {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onInterstitialDismissed();
                    }
                }

                @Override
                public void onClick() {
                    if (cpRewardVideoAdListener != null) {
                        cpRewardVideoAdListener.onInterstitialClicked();
                    }
                }
            });
            boolean orientation = DeviceUtils.isScreenLandscapeOrientation(getContext());
            CPAdActivity.start(getContext(), _cpAdResponse, orientation ? SCREEN_LAND_TYPE : SCREEN_PORT_TYPE, timeStamp, adSourceId, 1, false, direction);
        } catch (Exception e) {
            e.printStackTrace();
            if (cpRewardVideoAdListener != null) {
                cpRewardVideoAdListener.onVideoShowFailed(CPErrorCode.get(CPErrorCode.unknow, e.getMessage()));
                EventSendMessageUtil.getInstance().sendShowEndAd(getContext(), campaignId, mAdId, EC_UNSPECIFIED, adSourceId);
            }
        }
    }

    @Override
    public boolean isReady() {
        EventReadyRequest eventReadyRequest = new EventReadyRequest(getContext(), EventPushMessageUtils.EventPushStats.EV_ISREADY.getValue());
        if (mContext == null) {
            eventReadyRequest.setIs_ad_ready(IF_NOFILL);
            eventReadyRequest.setAsu_id(adSourceId);
            eventReadyRequest.setCampaign_id(campaignId);
            eventReadyRequest.setAd_id(mAdId);
            TPPushCenter.getInstance().saveCrossEvent(eventReadyRequest);
            return false;
        }
        try {
            if (checkIsReadyParams()) {
                CPAdResponse _cpAdResponse = CPAdManager.getInstance(mContext).getCpAdConfig(campaignId);
                boolean endCardReady = CPResourceStatus.isEndCardExist(_cpAdResponse);
                boolean videoReady = CPResourceStatus.isVideoExist(_cpAdResponse);
                String errorCode;
                if (!endCardReady) {
                    errorCode = EC_ENDCARD_MISSING;
                } else if (!videoReady) {
                    errorCode = EC_VIDEO_MISSING;
                } else {
                    errorCode = EC_SUCCESS;
                }
                eventReadyRequest.setIs_ad_ready(errorCode);
                eventReadyRequest.setAsu_id(adSourceId);
                eventReadyRequest.setCampaign_id(campaignId);
                eventReadyRequest.setAd_id(mAdId);
                TPPushCenter.getInstance().saveCrossEvent(eventReadyRequest);
                return endCardReady && videoReady;
            }
        } catch (Exception e) {
            e.printStackTrace();
            eventReadyRequest.setIs_ad_ready(IF_NOFILL);
            eventReadyRequest.setAsu_id(adSourceId);
            eventReadyRequest.setCampaign_id(campaignId);
            eventReadyRequest.setAd_id(mAdId);
            TPPushCenter.getInstance().saveCrossEvent(eventReadyRequest);
        }

        return false;
    }

    public CPRewardVideoAdListener getCpRewardVideoAdListener() {
        return cpRewardVideoAdListener;
    }

    public void setCpRewardVideoAdListener(CPRewardVideoAdListener cpRewardVideoAdListener) {
        this.cpRewardVideoAdListener = cpRewardVideoAdListener;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }
}
