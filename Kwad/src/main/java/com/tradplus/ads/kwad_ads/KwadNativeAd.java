package com.tradplus.ads.kwad_ads;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsDrawAd;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsImage;
import com.kwad.sdk.api.KsNativeAd;
import com.kwad.sdk.api.model.AdSourceLogoType;
import com.kwad.sdk.api.model.KsNativeConvertType;
import com.kwad.sdk.api.model.MaterialType;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPImageLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KwadNativeAd extends TPBaseAd {

    private KsNativeAd mKsNativeAd;
    private View feedView;
    private KsDrawAd mKsDrawAd;
    private KsFeedAd mKsFeedAd;
    private TPNativeAdView mTPNativeAdView;
    private List<View> viewList;
    private static final String TAG = "KwadNative";
    private int isRender = -1;

    public KwadNativeAd(Context context, KsFeedAd ksFeedAd) {
        this.mKsFeedAd = ksFeedAd;
    }

    public KwadNativeAd(Context context, KsNativeAd ksNativeAd) {
        this.mKsNativeAd = ksNativeAd;
        initNativeAd(context);
    }

    public KwadNativeAd(KsDrawAd ksDrawAd) {
        this.mKsDrawAd = ksDrawAd;
    }

    public void setDrawViews(List<View> views) {
        viewList = views;
    }

    public void setKsFeedAdView(View view) {
        feedView = view;
    }

    public void setRenderType(int type) {
        isRender = type;
    }

    private void initNativeAd(Context context) {
        mTPNativeAdView = setMiitInfo(mKsNativeAd);

        // 下载类型的AppName，⾮下载返回为空
        String appName = mKsNativeAd.getAppName();
        if (!TextUtils.isEmpty(appName)) {
            mTPNativeAdView.setTitle(appName);
        } else {
            // 获取⾮下载⼴告的产品名称
            String productName = mKsNativeAd.getProductName();
            if (!TextUtils.isEmpty(productName)) {
                mTPNativeAdView.setTitle(productName);
            }
        }

        String adDescription = mKsNativeAd.getAdDescription();
        if (!TextUtils.isEmpty(adDescription)) {
            mTPNativeAdView.setSubTitle(adDescription);
        }

        String actionDescription = mKsNativeAd.getActionDescription();
        if (!TextUtils.isEmpty(actionDescription)) {
            mTPNativeAdView.setCallToAction(actionDescription);
        }

        // ⼴告来源，可能为空
        String adSource = mKsNativeAd.getAdSource();
        Log.i(TAG, "adSource: " + adSource);
        if (!TextUtils.isEmpty(adSource)) {
            mTPNativeAdView.setAdSource(adSource);
        }

        // 下载类型的AppIcon，⾮下载返回为空
        String appIconUrl = mKsNativeAd.getAppIconUrl();
        if (!TextUtils.isEmpty(appIconUrl)) {
            mTPNativeAdView.setIconImageUrl(appIconUrl);
        }

        String videoUrl = mKsNativeAd.getVideoUrl();
        if (!TextUtils.isEmpty(videoUrl)) {
            mTPNativeAdView.setVideoUrl(videoUrl);
        }

        // 应⽤下载评分，取值0-5.0; ⾮下载返回为0
        float appScore = mKsNativeAd.getAppScore();
        Log.i("StarRating", "kuaishou StarRating: " + appScore);
        if (appScore > 0) {
            mTPNativeAdView.setStarRating(Double.valueOf(appScore));
        }

        // 获取⼴告⻆标的logo
        String adSourceLogoUrl = mKsNativeAd.getAdSourceLogoUrl(AdSourceLogoType.GREY);
        if (!TextUtils.isEmpty(adSourceLogoUrl)) {
            Log.i(TAG, "adSourceLogoUrl: " + adSourceLogoUrl);
            mTPNativeAdView.setAdChoiceUrl(adSourceLogoUrl);
        }

        List<KsImage> ksImages = mKsNativeAd.getImageList();
        if (ksImages != null && ksImages.size() > 0) {
            mTPNativeAdView.setPicObject(new ArrayList<Object>(ksImages));
        }
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if (!TextUtils.isEmpty(mTPNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTPNativeAdView.getIconImageUrl());
        }

        if (!TextUtils.isEmpty(mTPNativeAdView.getMainImageUrl())) {
            downloadImgUrls.add(mTPNativeAdView.getMainImageUrl());
        }
        return super.getDownloadImgUrls();
    }

    private TPNativeAdView setMiitInfo(KsNativeAd ksNativeAd) {
        TPNativeAdView nativeAdView = new TPNativeAdView();

        String appName = ksNativeAd.getAppName();
        if (!TextUtils.isEmpty(appName)) {
            nativeAdView.setAppName(appName);
        }

        String corporationName = ksNativeAd.getCorporationName();
        if (!TextUtils.isEmpty(corporationName)) {
            nativeAdView.setAuthorName(corporationName);
        }

        long appPackageSize = ksNativeAd.getAppPackageSize();
        nativeAdView.setPackageSizeBytes(appPackageSize);

        String appPrivacyUrl = ksNativeAd.getAppPrivacyUrl();
        if (!TextUtils.isEmpty(appPrivacyUrl)) {
            nativeAdView.setPermissionsUrl(appPrivacyUrl);
        }

        String permissionInfo = ksNativeAd.getPermissionInfo();
        if (!TextUtils.isEmpty(permissionInfo)) {
            nativeAdView.setPrivacyAgreement(permissionInfo);
        }

        String appVersion = ksNativeAd.getAppVersion();
        if (!TextUtils.isEmpty(appVersion)) {
            nativeAdView.setVersionName(appVersion);
        }

        return nativeAdView;
    }


    @Override
    public Object getNetworkObj() {
        if (mKsNativeAd != null) {
            return mKsNativeAd; // 自渲染
        } else if (mKsFeedAd != null) {
            return mKsFeedAd; // 模版
        } else if (mKsDrawAd != null) {
            return mKsDrawAd;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mKsNativeAd == null) return;

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            mKsNativeAd.registerViewForInteraction(viewGroup, clickViews, mListener);
        } else {
            // 点击转换view的集合，传入的view点击时会触发转换操作：app下载， 打开h5页面
            Map<View, Integer> clickViewMap = new HashMap<>();

            View callToAction = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_CALLTOACTION);
            if (callToAction != null) {
                clickViewMap.put(callToAction, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
            }

            View iconView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ICON);
            if (iconView != null) {
                clickViewMap.put(iconView, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
            }

            View titleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_TITLE);
            if (titleView != null) {
                clickViewMap.put(titleView, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
            }

            View subTitleView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_SUBTITLE);
            if (subTitleView != null) {
                clickViewMap.put(subTitleView, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
            }

            View adChoice = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES);
            if (adChoice != null) {
                clickViewMap.put(adChoice, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
            }

            if (mKsNativeAd.getMaterialType() == MaterialType.VIDEO) {
                Log.i(TAG, "initNativeAd:VIDEO");
                KsAdVideoPlayConfig videoPlayConfig = new KsAdVideoPlayConfig.Builder().videoSoundEnable(false)// 有声播放
                        .dataFlowAutoStart(true)// 流量下自动播放
                        .build();
                View mView = mKsNativeAd.getVideoView(activity, videoPlayConfig);

                View viewWithTag = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_IMAGE);
                if (viewWithTag != null) {
                    ViewGroup.LayoutParams params = viewWithTag.getLayoutParams();
                    ViewParent viewParent = viewWithTag.getParent();
                    if (viewParent != null && params != null) {
                        ((ViewGroup) viewParent).removeView(viewWithTag);
                        ((ViewGroup) viewParent).addView(mView, params);
                        if (clickViews.contains(mView)) {
                            clickViews.remove(mView);
                            clickViews.add(mView);
                        }
                    }
                }

            } else if (mKsNativeAd.getMaterialType() == MaterialType.SINGLE_IMG) {
                Log.i(TAG, "initNativeAd: IMG");
                ImageView viewWithTag = (ImageView) viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_IMAGE);
                if (viewWithTag != null) {
                    final List<KsImage> imageList = mKsNativeAd.getImageList();
                    if (imageList != null && imageList.get(0) != null && imageList.get(0).getImageUrl() != null) {
                        TPImageLoader.getInstance().loadImage(viewWithTag, imageList.get(0).getImageUrl());
                        clickViewMap.put(viewWithTag, KsNativeConvertType.SHOW_DOWNLOAD_TIPS_DIALOG);
                    }
                }
            }

            // 如果是自定义弹窗，请使用下面的配置
            // 注册View的点击，点击后触发转化
            if (mKsNativeAd != null) {
                mKsNativeAd.registerViewForInteraction(activity, viewGroup, clickViewMap, mListener);
            }

        }
    }


    private final KsNativeAd.AdInteractionListener mListener = new KsNativeAd.AdInteractionListener() {
        @Override
        public void onAdClicked(View view, KsNativeAd ksNativeAd) {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) mShowListener.onAdClicked();
        }

        @Override
        public void onAdShow(KsNativeAd ksNativeAd) {
            Log.i(TAG, "onAdShow: ");
            if (mShowListener != null) mShowListener.onAdShown();
        }

        /*
         * @return  返回为true, 则只会给媒体弹出回调，SDK的默认弹窗逻辑不会执行
         * @return  返回为true 返回为 false, 则使用SDK默认的合规弹窗。
         * 弹出弹窗dialog后， 用户确认下载，则媒体需要回调 OnClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
         * 弹出弹窗dialog后， 用户点击取消，则媒体需要回调 OnClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
         * */
        @Override
        public boolean handleDownloadDialog(DialogInterface.OnClickListener onClickListener) {
            return false;
        }

        @Override
        public void onDownloadTipsDialogShow() {
            Log.i(TAG, "onDownloadTipsDialogShow: 广告展示下载合规弹窗");
        }

        @Override
        public void onDownloadTipsDialogDismiss() {
            Log.i(TAG, "onDownloadTipsDialogShow:广告关闭下载合规弹窗");

        }
    };

    public void onAdViewClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void onAdViewExpanded() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdVideoStart() {
        if (mShowListener != null) {
            mShowListener.onAdVideoStart();
        }
    }

    public void onAdVideoEnd() {
        if (mShowListener != null) {
            mShowListener.onAdVideoEnd();
        }
    }

    public void onAdClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (isRender == TPBaseAd.AD_TYPE_NATIVE_EXPRESS) {
            return AD_TYPE_NATIVE_EXPRESS; //模版
        } else if (isRender == TPBaseAd.AD_TYPE_NATIVE_LIST) {
            return AD_TYPE_NATIVE_LIST; //DrawList
        } else {
            return AD_TYPE_NORMAL_NATIVE;//自渲染
        }
    }

    @Override
    public View getRenderView() {
        return feedView;
    }

    @Override
    public List<View> getMediaViews() {
        return viewList;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return null;
    }

    @Override
    public void clean() {
        if (mKsNativeAd != null) {
            mKsNativeAd.setVideoPlayListener(null);
            mKsNativeAd.setDownloadListener(null);
            mKsNativeAd = null;
        }

        if (mKsFeedAd != null) {
            mKsFeedAd.setAdInteractionListener(null);
            mKsFeedAd = null;
        }

        if (mKsDrawAd != null) {
            mKsDrawAd.setAdInteractionListener(null);
            mKsDrawAd = null;
        }
    }
}
