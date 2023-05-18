package com.tradplus.ads.klevin;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.tencent.klevin.ComplianceInfo;
import com.tencent.klevin.ads.ad.NativeAd;
import com.tencent.klevin.ads.ad.NativeExpressAd;
import com.tencent.klevin.ads.ad.NativeImage;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class KlevinNativeAd extends TPBaseAd {

    private NativeAd mNativeAd; // 自渲染
    private NativeExpressAd mNativeExpressAd; // 模版
    private View mView;// 模版
    private TPNativeAdView mTPNativeAdView;
    private int isRender;
    public static final String TAG = "KlevinNative";

    public KlevinNativeAd(NativeAd nativeAd, int type) {
        mNativeAd = nativeAd;
        isRender = type;
        initNativeAd(nativeAd);
    }

    public KlevinNativeAd(View view, int type) {
        mView = view;
        isRender = type;
    }

    private void initNativeAd(NativeAd nativeAd) {
        mTPNativeAdView = setMiitInfo(nativeAd); //合规五要素

        Bitmap adLogo = nativeAd.getAdLogo();
        if (adLogo != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(adLogo);
            Log.i(TAG, "initNativeAd: " + bitmapDrawable);
            mTPNativeAdView.setAdChoiceImage(bitmapDrawable);
        }

        String title = nativeAd.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String description = nativeAd.getDescription();
        if (!TextUtils.isEmpty(description)) {
            mTPNativeAdView.setSubTitle(description);
        }

        String icon = nativeAd.getIcon();
        if (!TextUtils.isEmpty(icon)) {
            mTPNativeAdView.setIconImageUrl(icon);
        }

        String downloadButtonLabel = nativeAd.getDownloadButtonLabel();
        if (!TextUtils.isEmpty(downloadButtonLabel)) {
            //下载按钮的相关文案
            mTPNativeAdView.setCallToAction(downloadButtonLabel);
        }

        List<NativeImage> imageList = nativeAd.getImageList();
        if (imageList != null && imageList.size() > 0) {
            mTPNativeAdView.setPicObject(new ArrayList<>(imageList));
        }

        View adView = nativeAd.getAdView();
        if (adView != null) {
            //通过getAdView()方法返回的广告组件得到有效曝光后会回调该方法，可用于判断广告组件是否正确被曝光
            //广告内容的可视性组件，如图片展示或视频播放，可直接加入到布局中
            mTPNativeAdView.setMediaView(adView);
        }
    }

    private TPNativeAdView setMiitInfo(NativeAd nativeData) {
        TPNativeAdView nativeAdView = new TPNativeAdView();
        ComplianceInfo complianceInfo = nativeData.getComplianceInfo();
        if (complianceInfo != null) {
            nativeAdView.setAppName(complianceInfo.getDeveloperName()); // 目标app的版本
            nativeAdView.setAuthorName(complianceInfo.getDeveloperName()); // 开发公司
            nativeAdView.setLastUpdateTime(complianceInfo.getLastUpdateTime());// 最新更新时间
            nativeAdView.setPermissionsUrl(complianceInfo.getPrivacyUrl()); // 隐私协议url
            nativeAdView.setPrivacyAgreement(complianceInfo.getPermissionUrl()); // 权限说明url
            nativeAdView.setVersionName(complianceInfo.getAppVersion()); // 目标app的版本
        }

        return nativeAdView;
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        if (mTPNativeAdView != null) {
            downloadImgUrls.clear();
            if (!TextUtils.isEmpty(mTPNativeAdView.getIconImageUrl())) {
                downloadImgUrls.add(mTPNativeAdView.getIconImageUrl());
            }
        }

        return super.getDownloadImgUrls();
    }

    @Override
    public Object getNetworkObj() {
        return mNativeAd != null ? mNativeAd : null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        // 注册广告点击事件，涉及广告计费，必须调用！而且不能再给注册的view设置OnClickListener
        if (mNativeAd == null) return;
        mNativeAd.registerAdInteractionViews(viewGroup, clickViews, new NativeAd.AdInteractionListener() {
            @Override
            public void onAdShow(NativeAd ad) {
                Log.i(TAG, "onAdShow: ");
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
            }

            @Override
            public void onAdClick(NativeAd ad, View view) {
                Log.i(TAG, "onAdClick: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onAdError(NativeAd ad, int err, String msg) {
                Log.i(TAG, "onAdError: errcode :" + err + ", msg :" + msg);
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(err + "");
                tpError.setErrorMessage(msg);
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(tpError);
                }
            }

            @Override
            public void onDetailClick(NativeAd nativeAd, View view) {

            }

            @Override
            public void onAdDetailClosed(NativeAd nativeAd, int i) {

            }
        });
    }


    public void onAdViewClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
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

    public void onAdViewExpanded() {
        if (mShowListener != null) {
            mShowListener.onAdShown();
        }
    }

    public void onAdViewClick() {
        if (mShowListener != null) {
            mShowListener.onAdClicked();
        }
    }

    public void onAdVideoError(TPError tpError) {
        if (mShowListener != null) {
            mShowListener.onAdVideoError(tpError);
        }
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (isRender == AD_TYPE_NORMAL_NATIVE) {
            return AD_TYPE_NORMAL_NATIVE; //自渲染
        } else {
            return AD_TYPE_NATIVE_EXPRESS; //模版
        }
    }

    @Override
    public View getRenderView() {
        return mView;
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
    public void clean() {
        if (mNativeAd != null) {
            mNativeAd.setVideoAdListener(null);
            mNativeAd.setDownloadListener(null);
            mNativeAd.destroy();
            mNativeAd = null;
        }
    }
}
