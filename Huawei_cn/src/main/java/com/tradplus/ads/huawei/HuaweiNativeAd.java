package com.tradplus.ads.huawei;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.hms.ads.AdvertiserInfo;
import com.huawei.hms.ads.AppDownloadButton;
import com.huawei.hms.ads.AppDownloadButtonStyle;
import com.huawei.hms.ads.Image;
import com.huawei.hms.ads.VideoOperator;
import com.huawei.hms.ads.nativead.DislikeAdReason;
import com.huawei.hms.ads.nativead.MediaView;
import com.huawei.hms.ads.nativead.NativeAd;
import com.huawei.hms.ads.nativead.NativeView;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.common.util.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

public class HuaweiNativeAd extends TPBaseAd {

    private TPNativeAdView mTpNativeAdView;
    private NativeAd mNativeAd;
    private NativeView mNativeAdContainer;
    private Context mContext;
    private static final String TAG = "HuaweiCnNative";
    private int NATIVE_TYPE = AD_TYPE_NORMAL_NATIVE;
    private int mTemplateType;
    private int mAppAutoInstall;
    private AppDownloadButton appDownloadButton;

    public HuaweiNativeAd(Context context, NativeAd nativeAd, int type) {
        this.mNativeAd = nativeAd;
        this.mContext = context;

        NATIVE_TYPE = type;
    }

    public HuaweiNativeAd(Context context, NativeAd nativeAd) {
        this.mNativeAd = nativeAd;
        this.mContext = context;
        initNativeAdView(nativeAd);
    }

    public void setTemplateType(int templateType) {
        mTemplateType = templateType;
    }


    public void setAppAutoInstall(int install) {
        mAppAutoInstall = install;
        Log.i(TAG, "setAppAutoInstall: " + install);
    }

    private void initNativeAdView(NativeAd nativeAd) {
        mTpNativeAdView = setMiitInfo(nativeAd);

        if (nativeAd.getTitle() != null) {
            mTpNativeAdView.setTitle(nativeAd.getTitle());
        }

        if (nativeAd.getDescription() != null) {
            mTpNativeAdView.setSubTitle(nativeAd.getDescription());
        }

        if (nativeAd.getAdSource() != null) {
            mTpNativeAdView.setAdSource(nativeAd.getAdSource());
        }

        if (nativeAd.getCallToAction() != null) {
            mTpNativeAdView.setCallToAction(nativeAd.getCallToAction());
        }

        Double rating = nativeAd.getRating();
        Log.i("StarRating", "Huawei StarRating: " + rating);
        if (rating != null) {
            mTpNativeAdView.setStarRating(rating);
        }

        List<Image> images = nativeAd.getImages();
        if (images != null && images.size() > 0) {
            mTpNativeAdView.setPicObject(new ArrayList<Object>(images));
        }

        if (nativeAd.isCustomDislikeThisAdEnabled()) {
            List<DislikeAdReason> dislikeAdReasons = nativeAd.getDislikeAdReasons();
            Log.i(TAG, "dislikeAdReasons: " + dislikeAdReasons);
            mTpNativeAdView.setDislikeReason(new ArrayList<Object>(dislikeAdReasons));
        }

        mNativeAdContainer = new NativeView(mContext);
        MediaView mediaView = new MediaView(mContext);
        mediaView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mNativeAdContainer.setMediaView(mediaView);
        mNativeAdContainer.getMediaView().setMediaContent(nativeAd.getMediaContent());
        mTpNativeAdView.setMediaView(mediaView);

        int creativeType = mNativeAd.getCreativeType();
        Log.i(TAG, "Native ad createType is " + creativeType);
        if (creativeType >= 101 && creativeType <= 108) {
            appDownloadButton = new AppDownloadButton(mContext);
            appDownloadButton.setFixedWidth(true);
            mTpNativeAdView.setAppDownloadButton(appDownloadButton);
            Log.i(TAG, "using AppDownloadButton: " + appDownloadButton);
        }

        VideoOperator videoOperator = nativeAd.getVideoOperator();
        if (videoOperator.hasVideo()) {
            // Add a video lifecycle event listener.
            videoOperator.setVideoLifecycleListener(videoLifecycleListener);
        }

        Log.i(TAG, "initNativeAdView: ");

    }

    private VideoOperator.VideoLifecycleListener videoLifecycleListener = new VideoOperator.VideoLifecycleListener() {
        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoStart();
            }
        }

        @Override
        public void onVideoPlay() {
            Log.i(TAG, "onVideoPlay: ");

        }

        @Override
        public void onVideoEnd() {
            Log.i(TAG, "onVideoEnd: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }

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

    public void onAdClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    @Override
    public Object getNetworkObj() {
        if (mNativeAd != null) {
            return mNativeAd;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
    }

    @Override
    public void registerClickAfterRender(ViewGroup viewGroup, ArrayList<View> clickViews) {
        for (View childView : clickViews) {
            if (childView instanceof ImageView) {
            } else if (childView instanceof Button || childView instanceof TextView) {
                String text = ((TextView) childView).getText().toString();
                if (mNativeAd != null && mTpNativeAdView != null) {
                    if (text.equals(mNativeAd.getTitle())) {
                        mNativeAdContainer.setTitleView(childView);
                    }
                    if (text.equals(mNativeAd.getDescription())) {
                        mNativeAdContainer.setDescriptionView(childView);
                    }
                    if (text.equals(mNativeAd.getCallToAction())) {
                        mNativeAdContainer.setCallToActionView(childView);
                    }
                }
            }
        }

        if (mNativeAdContainer != null) {
            mNativeAdContainer.setNativeAd(mNativeAd);

            try {
                if (mAppAutoInstall != 0 && appDownloadButton != null) {
                    boolean register = mNativeAdContainer.register(appDownloadButton);
                    if (register) {
                        Log.i(TAG, "appDownloadButton register success");
                        appDownloadButton.refreshAppStatus();
                    }
                }
            }catch (Throwable throwable) {

            }
        }
    }

    private TPNativeAdView setMiitInfo(NativeAd nativeAd) {
        TPNativeAdView nativeAdView = new TPNativeAdView();
        List<AdvertiserInfo> advertiserInfo = nativeAd.getAdvertiserInfo();
        Log.i(TAG, "advertiserInfo: " + advertiserInfo);
        if (advertiserInfo != null && advertiserInfo.size() > 0) {
            AdvertiserInfo advertiserInfo1 = advertiserInfo.get(0);
            if (advertiserInfo1 != null) {
                nativeAdView.setHuaweiAdInfoKey(advertiserInfo1.getKey());
                nativeAdView.setHuaweiAdInfoSeq(advertiserInfo1.getSeq());
                nativeAdView.setHuaweiAdInfoValue(advertiserInfo1.getValue());
            }
        }
        return nativeAdView;
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTpNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        return NATIVE_TYPE;
    }

    @Override
    public View getRenderView() {
        return NATIVE_TYPE == AD_TYPE_NATIVE_EXPRESS ? initTempleteNativeAdView(mNativeAd) : null;
    }

    @Override
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return mNativeAdContainer;
    }

    @Override
    public void clean() {
        if (appDownloadButton != null) {
            if (appDownloadButton.getParent() != null && appDownloadButton.getParent() instanceof ViewGroup) {
                ((ViewGroup)appDownloadButton.getParent()).removeView(appDownloadButton);
            }

            appDownloadButton.setVisibility(View.GONE);
            appDownloadButton = null;
        }

        if (mNativeAd != null) {
            mNativeAd.destroy();
            mNativeAd = null;
        }

        mTpNativeAdView = null;
    }


    public NativeView initTempleteNativeAdView(NativeAd nativeAd) {
        String layoutName = "tp_native_video_template";
        NativeView _nativeView;
        if (mTemplateType == 2) {
            layoutName = "tp_native_small_template";
        } else if (mTemplateType == 3) {
            layoutName = "tp_native_three_images_template";
        }
        Log.i(TAG, "TemplateType is: " + layoutName);
        _nativeView = (NativeView) LayoutInflater.from(mContext).inflate((ResourceUtils.getLayoutIdByName(mContext, layoutName)), null, false);

        if (mTemplateType == 3) {
            createSmallImageAdView(_nativeView, nativeAd);
        } else {
            _nativeView.setMediaView((MediaView) _nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_ad_media")));
            _nativeView.getMediaView().setMediaContent(nativeAd.getMediaContent());
        }

        // Register a native ad material view.
        _nativeView.setTitleView(_nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_ad_title")));
        _nativeView.setAdSourceView(_nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_ad_source")));
        _nativeView.setCallToActionView(_nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_ad_call_to_action")));

        // Populate a native ad material view.
        ((TextView) _nativeView.getTitleView()).setText(nativeAd.getTitle());

        if (null != nativeAd.getAdSource()) {
            ((TextView) _nativeView.getAdSourceView()).setText(nativeAd.getAdSource());
        }
        _nativeView.getAdSourceView().setVisibility(null != nativeAd.getAdSource() ? View.VISIBLE : View.INVISIBLE);

        if (null != nativeAd.getCallToAction()) {
            ((Button) _nativeView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }
        _nativeView.getCallToActionView().setVisibility(null != nativeAd.getCallToAction() ? View.VISIBLE : View.INVISIBLE);

        // Obtain a video controller.
        VideoOperator videoOperator = nativeAd.getVideoOperator();

        // Check whether a native ad contains video materials.
        if (videoOperator.hasVideo()) {
            // Add a video lifecycle event listener.
            videoOperator.setVideoLifecycleListener(videoLifecycleListener);
        }

        // Register a native ad object.
        _nativeView.setNativeAd(nativeAd);

        // using AppDownloadButton
        int creativeType = mNativeAd.getCreativeType();
        Log.i(TAG, "Native ad createType is " + creativeType);
        if (mAppAutoInstall != 0 && (creativeType >= 101 && creativeType <= 108)) {
            createAppDownloadButtonView(_nativeView);
        }

        return _nativeView;
    }

    private void createAppDownloadButtonView(NativeView nativeView) {
        Log.i(TAG, "using AppDownloadButton: ");
        appDownloadButton = nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_app_download_btn"));
        appDownloadButton.setAppDownloadButtonStyle(new MyAppDownloadStyle(mContext));
        boolean register = nativeView.register(appDownloadButton);
        if (register) {
            Log.i(TAG, "appDownloadButton register success");
            appDownloadButton.setVisibility(View.VISIBLE);
            appDownloadButton.refreshAppStatus(); // 刷新下载按钮的状态。
            nativeView.getCallToActionView().setVisibility(View.GONE);
        } else {
            appDownloadButton.setVisibility(View.GONE);
            nativeView.getCallToActionView().setVisibility(View.VISIBLE);
        }
    }

    private void createSmallImageAdView(NativeView nativeView, NativeAd nativeAd) {
        Log.i(TAG, "using SmallImage template: ");
        ImageView imageView1 = nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_image_view_1"));
        ImageView imageView2 = nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_image_view_2"));
        ImageView imageView3 = nativeView.findViewById(ResourceUtils.getViewIdByName(mContext, "tp_image_view_3"));
        List<Image> images = nativeAd.getImages();
        if (images != null) {
            int size = images.size();
            Log.i(TAG, "List<Image> images size is : " + size);
            if (size >= 3) {
                imageView1.setImageDrawable(images.get(0).getDrawable());
                imageView2.setImageDrawable(images.get(1).getDrawable());
                imageView3.setImageDrawable(images.get(2).getDrawable());
            } else {
                imageView1.setImageDrawable(images.get(0).getDrawable());
                imageView2.setImageDrawable(images.get(0).getDrawable());
                imageView3.setImageDrawable(images.get(0).getDrawable());
            }
        }
    }


    public static class MyAppDownloadStyle extends AppDownloadButtonStyle {

        public MyAppDownloadStyle(Context context) {
            super(context);
            normalStyle.setTextColor(context.getResources().getColor(R.color.white));
            normalStyle.setBackground(context.getResources().getDrawable(R.drawable.tp_native_button_rounded_corners_shape));
            processingStyle.setTextColor(context.getResources().getColor(R.color.black));
        }
    }
}
