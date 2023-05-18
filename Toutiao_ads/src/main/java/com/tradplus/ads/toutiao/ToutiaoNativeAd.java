package com.tradplus.ads.toutiao;

import static android.view.Gravity.CENTER;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.adapter.nativead.TPNativeStream;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.ArrayList;
import java.util.List;

public class ToutiaoNativeAd extends TPBaseAd {

    private TTFeedAd mTTFeedAd;
    private TPNativeAdView mTPNativeAdView;
    private List<View> viewList;
    private View mAdView;
    private int isRender;
    public static final String TAG = "ToutiaoNative";
    private RelativeLayout mRelativeLayout;
    private ToutiaoStreamPlayer toutiaoStreamPlayer;


    public ToutiaoNativeAd(TTFeedAd mTtNativeExpressAd) {
        if (mTtNativeExpressAd != null) {
            initViewData(mTtNativeExpressAd);
        }
    }

    public ToutiaoNativeAd(Context context, View view) {
        mRelativeLayout = new RelativeLayout(context);
        mRelativeLayout.addView(view);
        mRelativeLayout.setGravity(CENTER);
        mAdView = view;
    }

    public ToutiaoNativeAd(List<View> views) {
        viewList = views;
    }


    public void setRenderType(int type) {
        isRender = type;
    }

    private void initViewData(TTFeedAd nativeData) {
        mTTFeedAd = nativeData;

        if(isRender == AppKeyManager.TEMPLATE_PATCH_VIDEO) {
            toutiaoStreamPlayer = new ToutiaoStreamPlayer(mTTFeedAd);
        }

        mTPNativeAdView = new TPNativeAdView();

        String title = nativeData.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String description = nativeData.getDescription();
        if (!TextUtils.isEmpty(description)) {
            mTPNativeAdView.setSubTitle(description);
        }

        String buttonText = nativeData.getButtonText();
        if (!TextUtils.isEmpty(buttonText)) {
            mTPNativeAdView.setCallToAction(buttonText);
        }

        TTImage icon = nativeData.getIcon();
        if (icon != null){
            String imageUrl = icon.getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
                mTPNativeAdView.setIconImageUrl(imageUrl);
            }
        }

        Bitmap adLogo = nativeData.getAdLogo();
        if (adLogo != null) {
            Log.i(TAG, "AdLogo: " + adLogo);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(adLogo);
            mTPNativeAdView.setAdChoiceImage(bitmapDrawable);
        }

        List<TTImage> imageList = nativeData.getImageList();
        if (imageList != null && imageList.size() > 0) {
            mTPNativeAdView.setPicObject(new ArrayList<Object>(imageList));
        }

        String source = nativeData.getSource();//广告来源
        Log.i(TAG, "source: " + source);
        if (!TextUtils.isEmpty(source)) {
            mTPNativeAdView.setAdSource(source);
        }

        int imageMode = nativeData.getImageMode();
        Log.i(TAG, "imageMode: " + imageMode);
        if (imageMode == TTAdConstant.IMAGE_MODE_VIDEO ||
                imageMode == TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL ||
                imageMode == TTAdConstant.IMAGE_MODE_LIVE) {
            View video = nativeData.getAdView();
            if (video != null) {
                mTPNativeAdView.setMediaView(video);
            }
        } else if (imageList != null && imageList.size() > 0 && imageList.get(0) != null) {
            String imageUrl = imageList.get(0).getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
                mTPNativeAdView.setMainImageUrl(imageUrl);
            }
        }

    }

    @Override
    public TPNativeStream getNativeStream() {
        return toutiaoStreamPlayer;
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();
        if (!TextUtils.isEmpty(mTPNativeAdView.getMainImageUrl())) {
            downloadImgUrls.add(mTPNativeAdView.getMainImageUrl());
        }
        if (!TextUtils.isEmpty(mTPNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTPNativeAdView.getIconImageUrl());
        }
        return super.getDownloadImgUrls();
    }

    @Override
    public Object getNetworkObj() {
        if (mTTFeedAd != null) {
            return mTTFeedAd;
        } else if (mAdView != null) {
            return mAdView;
        } else if (viewList != null) {
            return viewList;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mTTFeedAd != null) {
            mTTFeedAd.registerViewForInteraction(viewGroup, clickViews, clickViews, adInteractionListener);
        }
    }

    private final TTNativeAd.AdInteractionListener adInteractionListener = new TTNativeAd.AdInteractionListener() {
        @Override
        public void onAdClicked(View view, TTNativeAd ttNativeAd) {
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onAdCreativeClick(View view, TTNativeAd ttNativeAd) {
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onAdShow(TTNativeAd ttNativeAd) {
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }
    };

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        // 均按照TPBaseAd中定义的NativeType进行返回
        if (isRender == TPBaseAd.AD_TYPE_NORMAL_NATIVE || isRender == ToutiaoConstant.NATIVE_PATCH_VIDEO) {
            return AD_TYPE_NORMAL_NATIVE;//自渲染
        } else if (isRender == TPBaseAd.AD_TYPE_NATIVE_EXPRESS) {
            return AD_TYPE_NATIVE_EXPRESS;//模版
        } else {
            return AD_TYPE_NATIVE_LIST;
        }

    }

    @Override
    public View getRenderView() {
        return mRelativeLayout;
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

    }

    public void adClicked() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void adShown() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void adClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
        }
    }

    public void adShowFailed(TPError tpError) {
        if (mShowListener != null) {
            mShowListener.onAdVideoError(tpError);
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
}
