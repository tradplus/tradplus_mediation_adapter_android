package com.tradplus.ads.mintegral;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.mbridge.msdk.foundation.entity.CampaignEx;
import com.mbridge.msdk.nativex.view.MBMediaView;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.MBNativeAdvancedHandler;
import com.mbridge.msdk.out.MBNativeHandler;
import com.mbridge.msdk.out.OnMBMediaViewListenerPlus;
import com.mbridge.msdk.widget.MBAdChoice;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;

import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.CENTER;

public class MIntegralNativeAd extends TPBaseAd {

    private TPNativeAdView mTpNativeAdView;
    private Campaign mCampaignBean;
    private MBNativeHandler mNativeHandler;
    private MBBidNativeHandler mBidNativeHandler;
    private ViewGroup mAdvancedNativeView; // 模版
    private int isRender;
    private RelativeLayout mRelativeLayout;
    private MBNativeAdvancedHandler mMBNativeAdvancedHandler;//自动渲染（模版）Native
    private MBAdChoice mbAdChoice;
    private static final String TAG = "MTGCNNative";

    public MIntegralNativeAd(MBNativeAdvancedHandler mbNativeAdvancedHandler, ViewGroup advancedNativeView, Context context) {
        this.mMBNativeAdvancedHandler = mbNativeAdvancedHandler;
        mAdvancedNativeView = advancedNativeView;
        mRelativeLayout = new RelativeLayout(context);
        mRelativeLayout.setGravity(CENTER);
        mRelativeLayout.addView(mAdvancedNativeView);
    }

    public MIntegralNativeAd(Campaign campaign, Context context, MBNativeHandler tgNativeHandler, boolean isVideoSoundOnOff) {
        if (tgNativeHandler == null || campaign == null || context == null) {
            return;
        }
        mNativeHandler = tgNativeHandler;
        mCampaignBean = campaign;
        initAdViewData(campaign, context, isVideoSoundOnOff);
    }

    public MIntegralNativeAd(Campaign campaign, Context context, MBBidNativeHandler tgNativeHandler, boolean isVideoSoundOnOff) {
        if (tgNativeHandler == null || campaign == null || context == null) {
            return;
        }
        mBidNativeHandler = tgNativeHandler;
        mCampaignBean = campaign;
        initAdViewData(campaign, context, isVideoSoundOnOff);
    }

    public void setRenderType(int type) {
        isRender = type;
    }


    private void initAdViewData(Campaign nativeAd, Context context, boolean isVideoSoundOnOff) {
        mTpNativeAdView = new TPNativeAdView();

        String appName = nativeAd.getAppName();
        if (!TextUtils.isEmpty(appName)) {
            mTpNativeAdView.setTitle(appName);
        }

        String appDesc = nativeAd.getAppDesc();
        if (!TextUtils.isEmpty(appDesc)) {
            mTpNativeAdView.setSubTitle(appDesc);
        }

        String adCall = nativeAd.getAdCall();
        if (!TextUtils.isEmpty(adCall)) {
            mTpNativeAdView.setCallToAction(adCall);
        }

        String iconUrl = nativeAd.getIconUrl();
        if (!TextUtils.isEmpty(iconUrl)) {
            mTpNativeAdView.setIconImageUrl(iconUrl);
        }

        double rating = nativeAd.getRating();
        Log.i("StarRating", "MTG StarRating: " + rating);
        if (rating > 0) {
            mTpNativeAdView.setStarRating(rating);
        }

        CampaignEx campaignEx = (CampaignEx) nativeAd;

        if (campaignEx.getVideoUrlEncode() != null && campaignEx.getVideoUrlEncode().length() > 0) {
            MBMediaView mbMediaView = new MBMediaView(context);
            Log.i(TAG, "MBMediaView: " + mbMediaView);
            mbMediaView.setNativeAd(nativeAd);
            // 设置在视频是否允许刷新
            mbMediaView.setAllowVideoRefresh(false);
            // 开启或关闭 视频声音
            mbMediaView.setVideoSoundOnOff(isVideoSoundOnOff);
            mbMediaView.setOnMediaViewListener(new OnMBMediaViewListenerPlus() {
                @Override
                public void onEnterFullscreen() {
                    Log.i(TAG, "onEnterFullscreen:  ");
                }

                @Override
                public void onExitFullscreen() {
                    Log.i(TAG, "onExitFullscreen: Mediaview退出全屏模式时调用");
                }

                @Override
                public void onStartRedirection(Campaign campaign, String s) {
                    //广告开始跳转重定向时调用
                }

                @Override
                public void onFinishRedirection(Campaign campaign, String s) {
                    //广告完成跳转重定向后调用
                }

                @Override
                public void onRedirectionFailed(Campaign campaign, String s) {
                    //广告跳转重定向失败时调用
                }

                @Override
                public void onVideoAdClicked(Campaign campaign) {
                    Log.i(TAG, "onVideoAdClicked: ");
                    if (mShowListener != null) {
                        mShowListener.onAdClicked();
                    }
                }

                @Override
                public void onVideoStart() {
                    Log.i(TAG, "onVideoStart: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoStart();
                    }
                }

                @Override
                public void onVideoComplete() {
                    //广告视频完全播放完后调用，但广告没有关闭
                    Log.i(TAG, "onVideoComplete: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoEnd();
                    }
                }
            });
            // 设置MBMediaView是否可以全屏显示
            mbMediaView.setIsAllowFullScreen(true);
            mTpNativeAdView.setMediaView(mbMediaView);
        } else {
            String imageUrl = nativeAd.getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
                mTpNativeAdView.setMainImageUrl(imageUrl);
            }
        }

    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if (!TextUtils.isEmpty(mTpNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTpNativeAdView.getIconImageUrl());
        }
        if (!TextUtils.isEmpty(mTpNativeAdView.getMainImageUrl())) {
            downloadImgUrls.add(mTpNativeAdView.getMainImageUrl());
        }
        return super.getDownloadImgUrls();
    }

    @Override
    public Object getNetworkObj() {
        if (mAdvancedNativeView != null) {
            return mAdvancedNativeView;
        } else if (mCampaignBean != null) {
            return mCampaignBean;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        FrameLayout adChoicesView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES);
        if(viewGroup.getContext() != null) {
            mbAdChoice = new MBAdChoice(viewGroup.getContext());
            if(mCampaignBean != null) {
                mbAdChoice.setCampaign(mCampaignBean);
            }
        }

        if (adChoicesView != null && mbAdChoice != null) {
            adChoicesView.removeAllViews();
            adChoicesView.addView(mbAdChoice, 0);
        }

        if (mNativeHandler != null) {
            mNativeHandler.registerView(viewGroup, clickViews, mCampaignBean);
        }

        if (mBidNativeHandler != null) {
            mBidNativeHandler.registerView(viewGroup, clickViews, mCampaignBean);
        }
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTpNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (isRender == TPBaseAd.AD_TYPE_NATIVE_EXPRESS) {
            return AD_TYPE_NATIVE_EXPRESS;//模版
        } else {
            return AD_TYPE_NORMAL_NATIVE; //自渲染
        }
    }

    @Override
    public View getRenderView() {
        if (mRelativeLayout != null) {
            return mRelativeLayout;
        } else {
            return null;
        }
    }

    @Override
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return null;
    }

    @Override
    public void onPause() {
        Log.i(TAG,"onPause");
        if(mMBNativeAdvancedHandler != null) {
            mMBNativeAdvancedHandler.onPause();
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG,"onResume");
        if(mMBNativeAdvancedHandler != null) {
            mMBNativeAdvancedHandler.onResume();
        }
    }

    @Override
    public void clean() {
        if (mRelativeLayout != null) {
            Log.i(TAG, "clean:mRelativeLayout");
            mRelativeLayout.removeAllViews();
        }

        if (mAdvancedNativeView != null) {
            mAdvancedNativeView.removeAllViews();
            mAdvancedNativeView = null;
        }

        if (mMBNativeAdvancedHandler != null) {
            mMBNativeAdvancedHandler.setAdListener(null);
            mMBNativeAdvancedHandler.release();
            mMBNativeAdvancedHandler = null;
        }
    }

    public void onAdShown() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void onAdClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }
}
