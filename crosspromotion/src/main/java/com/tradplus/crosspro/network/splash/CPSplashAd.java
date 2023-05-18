package com.tradplus.crosspro.network.splash;

import static com.tradplus.crosspro.common.CPConst.DEFAULT_EXPRETIME;
import static com.tradplus.crosspro.common.CPConst.FORMAT.SPLASH_FORMAT;

import android.content.Context;
import android.view.View;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.event.TPPushCenter;
import com.tradplus.ads.network.CPErrorUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;
import com.tradplus.ads.pushcenter.event.request.EventLoadEndRequest;
import com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.tradplus.crosspro.manager.CPAdConfigController;
import com.tradplus.crosspro.manager.CPAdManager;
import com.tradplus.crosspro.manager.resource.CPLoader;
import com.tradplus.crosspro.network.base.CPBaseAd;
import com.tradplus.crosspro.network.base.CPError;
import com.tradplus.crosspro.ui.EndCardView;
import com.tradplus.crosspro.ui.SplashView;

public class CPSplashAd extends CPBaseAd {

    private CPSplashAdListener cpSplashAdListener;
    private String mAdId;

    private int countdown_time;
    private int is_skipable;
    private int direction;


    public CPSplashAd(Context context, String campaignId, String adSourceId, int countdown_time, int is_skipable, int direction) {
        super(context, campaignId, adSourceId);
        this.countdown_time = countdown_time;
        this.is_skipable = is_skipable;
        this.direction = direction;
    }

    @Override
    public void load() {
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
                        if (cpSplashAdListener != null) {
                            cpSplashAdListener.onInterstitialLoaded();
                        }

                    }

                    @Override
                    public void onFailed(CPError msg) {
                        if (cpSplashAdListener != null) {
                            cpSplashAdListener.onInterstitialFailed(CPErrorUtil.getTradPlusErrorCode(msg));
                        }
                    }
                }, adSourceId);

            }

            @Override
            public void onError(int code, String msg) {
                if (cpSplashAdListener != null) {
                    //5100
                    EventSendMessageUtil.getInstance().sendOpenAPIStart(getContext(), "", adSourceId, "");
                    //5700
                    EventSendMessageUtil.getInstance().sendLoadAdNetworkStart(getContext(), campaignId, adSourceId);
                    //5800 配置获取失败，无法拿到adid
                    EventLoadEndRequest eventLoadEndRequest = new EventLoadEndRequest(getContext(), EventPushMessageUtils.EventPushStats.EV_LOAD_AD_END.getValue());
                    eventLoadEndRequest.setCampaign_id(campaignId);
                    eventLoadEndRequest.setAsu_id(adSourceId);

                    eventLoadEndRequest.setError_code(TPError.parseErrorCode(code));
                    eventLoadEndRequest.setError_message(msg);

                    long loadTime = RequestUtils.getInstance().countRuntime(eventLoadEndRequest.getCreateTime());
                    eventLoadEndRequest.setLoad_time(loadTime + "");
                    TPPushCenter.getInstance().saveCrossEvent(eventLoadEndRequest);
                    cpSplashAdListener.onInterstitialFailed(CPErrorUtil.getErrorCode(code,msg));
                }
            }
        });
        cpAdConfigController.loadConfig(getContext(), campaignId, adSourceId, SPLASH_FORMAT, direction, -1);
    }

    @Override
    public void show() {

    }

    public View getSplashView(OnSplashShownListener onSplashShownListener) {
        SplashView splashView = new SplashView(mContext);
        splashView.initView(mContext, campaignId, countdown_time, is_skipable, direction, adSourceId, new EndCardView.OnEndCardListener() {
            @Override
            public void onClickEndCard() {
                cpSplashAdListener.onInterstitialClicked();
            }

            @Override
            public void onCloseEndCard() {
                cpSplashAdListener.onInterstitialDismissed();
            }
        }, onSplashShownListener);
        return splashView;
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

    public interface OnSplashShownListener {
        void onShown();
    }

    @Override
    public boolean isReady() {
        return true;
    }


    public void setCpSplashAdListener(CPSplashAdListener cpSplashAdListener) {
        this.cpSplashAdListener = cpSplashAdListener;
    }
}
