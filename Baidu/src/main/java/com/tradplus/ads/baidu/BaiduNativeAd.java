package com.tradplus.ads.baidu;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.baidu.mobads.sdk.api.ExpressResponse;
import com.baidu.mobads.sdk.api.FeedNativeView;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.XNativeView;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class BaiduNativeAd extends TPBaseAd {

    private int isRender;
    private TPNativeAdView mTPNativeAdView;
    private NativeResponse mNativeResponse;
    private XNativeView mXNativeView;
    private View mExpressAdView;// 模版View
    private ExpressResponse mExpressResponse; // 模版
    private RelativeLayout mRelativeLayout;
    private static final String TAG = "BaiduNative";


    public BaiduNativeAd(NativeResponse nativeResponse, Activity activity) {
        this.mNativeResponse = nativeResponse;
        initViewData(nativeResponse, activity);
    }

    public BaiduNativeAd(ExpressResponse expressResponse, Activity activity) {
        this.mExpressResponse = expressResponse;
        if (expressResponse.getExpressAdView() == null) {
            if (mShowListener != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage("ExpressAdView为空");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }
        mRelativeLayout = new RelativeLayout(activity);
        mRelativeLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mExpressAdView = expressResponse.getExpressAdView();
        mExpressResponse.setAdDislikeListener(new ExpressResponse.ExpressDislikeListener() {
            @Override
            public void onDislikeWindowShow() {

            }

            @Override
            public void onDislikeItemClick(String s) {
                Log.i(TAG, "onDislikeItemClick: ");
                mRelativeLayout.removeView(mExpressAdView);
                mRelativeLayout.removeAllViews();

                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }

            @Override
            public void onDislikeWindowClose() {

            }
        });

        mRelativeLayout.addView(mExpressAdView);
    }

    private void initViewData(NativeResponse nativeResponse, Activity activity) {
        mTPNativeAdView = new TPNativeAdView();

        String iconUrl = nativeResponse.getIconUrl();
        if (!TextUtils.isEmpty(iconUrl)) {
            mTPNativeAdView.setIconImageUrl(iconUrl);
        }

        String imageUrl = nativeResponse.getImageUrl();
        List<String> multiPicUrls = nativeResponse.getMultiPicUrls();
        if (multiPicUrls != null && multiPicUrls.size() > 2) {
            // 返回多张图
            mTPNativeAdView.setMainImageUrl(multiPicUrls.get(0));
            mTPNativeAdView.setPicUrls(multiPicUrls);
        }

        if (!TextUtils.isEmpty(imageUrl)) {
            // 返回一张大图
            mTPNativeAdView.setMainImageUrl(imageUrl);
        }

        String baiduLogoUrl = nativeResponse.getBaiduLogoUrl();
        if (!TextUtils.isEmpty(baiduLogoUrl)) {
            Log.i(TAG, "BaiduLogo: " + baiduLogoUrl);
            mTPNativeAdView.setAdChoiceUrl(baiduLogoUrl);
        }

        String desc = nativeResponse.getDesc();
        if (!TextUtils.isEmpty(desc)) {
            mTPNativeAdView.setSubTitle(desc);
        }

        String title = nativeResponse.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String brandName = nativeResponse.getBrandName();
        Log.i(TAG, "brandName: " + brandName);
        if (!TextUtils.isEmpty(brandName)) {
            mTPNativeAdView.setAdSource(brandName);
        }

        String btnText = getBtnText(nativeResponse);
        if (!TextUtils.isEmpty(btnText)) {
            mTPNativeAdView.setCallToAction(btnText);
        }

        NativeResponse.MaterialType materialType = mNativeResponse.getMaterialType();
        Log.i(TAG, "materialType: " + materialType);

        if (materialType == NativeResponse.MaterialType.VIDEO) {
            mXNativeView = new XNativeView(activity);
            mXNativeView.setNativeItem(mNativeResponse);
            mXNativeView.setUseDownloadFrame(true);
            mTPNativeAdView.setMediaView(mXNativeView);
        }

    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        if (mTPNativeAdView != null) {
            downloadImgUrls.clear();

            if (!TextUtils.isEmpty(mTPNativeAdView.getIconImageUrl())) {
                downloadImgUrls.add(mTPNativeAdView.getIconImageUrl());
            }

            if (!TextUtils.isEmpty(mTPNativeAdView.getMainImageUrl())) {
                downloadImgUrls.add(mTPNativeAdView.getMainImageUrl());
            }

            if (mTPNativeAdView.getPicUrls() != null && mTPNativeAdView.getPicUrls().size() > 0) {
                for (int i = 0; i < mTPNativeAdView.getPicUrls().size(); i++) {
                    downloadImgUrls.add(mTPNativeAdView.getPicUrls().get(i));
                }
            }
        }

        return super.getDownloadImgUrls();
    }


    public void setRenderType(int type) {
        isRender = type;
    }

    @Override
    public Object getNetworkObj() {
        if (isRender == TPBaseAd.AD_TYPE_NORMAL_NATIVE) {
            return mNativeResponse;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
    }

    public void registerClickAfterRender(ViewGroup viewGroup, ArrayList<View> clickViews) {
        /**
         * 广告数据渲染完毕，View展示的时候使用NativeResponse调用registerViewForInteraction来发送曝光
         * registerViewForInteraction()与BaiduNativeManager配套使用
         * 警告：调用该函数来发送展现，勿漏！
         *
         * view: 广告容器或广告View（曝光计费，必传）
         * clickViews: 可点击的View，默认展示下载整改弹框
         * creativeViews: 带有下载引导文案的View，默认不会触发下载整改弹框
         * interactionListener: 曝光、点击回调
         */
        if (mNativeResponse != null) {
            mNativeResponse.registerViewForInteraction(viewGroup, clickViews, null, new NativeResponse.AdInteractionListener() {
                @Override
                public void onAdClick() {
                    Log.i(TAG, "onAdClick: ");
                    if (mShowListener != null) {
                        mShowListener.onAdClicked();
                    }
                }

                @Override
                public void onADExposed() {
                    Log.i(TAG, "onADExposed: ");
                    if (mShowListener != null) {
                        mShowListener.onAdShown();
                    }
                }

                @Override
                public void onADExposureFailed(int i) {

                }

                @Override
                public void onADStatusChanged() {

                }

                @Override
                public void onAdUnionClick() {

                }
            });

            // 给隐私、权限注册事件回调
            mNativeResponse.setAdPrivacyListener(new NativeResponse.AdDownloadWindowListener() {
                @Override
                public void adDownloadWindowShow() {
                    Log.i(TAG, "adDownloadWindowShow: ");
                    // 使弹窗出现时暂停
                    mXNativeView.pause();
                }

                @Override
                public void adDownloadWindowClose() {
                    Log.i(TAG, "adDownloadWindowClose: ");
                    // 使弹窗消失时继续
                    mXNativeView.resume();
                }

                @Override
                public void onADPrivacyClick() {

                }

                @Override
                public void onADPermissionShow() {

                }

                @Override
                public void onADPermissionClose() {

                }
            });
        }

        if (mXNativeView != null) {
            mXNativeView.setNativeViewClickListener(new XNativeView.INativeViewClickListener() {
                @Override
                public void onNativeViewClick(XNativeView xNativeView) {
                    Log.i(TAG, "onNativeViewClick: ");
                    if (mShowListener != null) {
                        mShowListener.onAdClicked();
                    }
                }
            });

            // 尝试播放，判断是否需要自动播放
            mXNativeView.render();
        }
    }


    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (isRender == TPBaseAd.AD_TYPE_NATIVE_EXPRESS) {
            return AD_TYPE_NATIVE_EXPRESS; //模版，下发isRender == 1
        } else {
            return AD_TYPE_NORMAL_NATIVE; //自渲染 ，下发isRender == 2
        }
    }

    @Override
    public View getRenderView() {
        return mRelativeLayout;
    }

    @Override
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return null;
    }

    public void onAdClosed() {
        if (mShowListener != null) {
            mShowListener.onAdClosed();
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

    @Override
    public void clean() {
    }

    // 获取安装状态、下载进度所对应的按钮文案
    private String getBtnText(NativeResponse nrAd) {
        if (nrAd == null) {
            return "";
        }
        String actButtonString = nrAd.getActButtonString();
        if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD || nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_DEEP_LINK) {
            int status = nrAd.getDownloadStatus();
            if (status >= 0 && status <= 100) {
                return "下载中：" + status + "%";
            } else if (status == 101) {
                return "点击安装";
            } else if (status == 102) {
                return "继续下载";
            } else if (status == 103) {
                return "点击启动";
            } else if (status == 104) {
                return "重新下载";
            } else {
                if (!TextUtils.isEmpty(actButtonString)) {
                    return actButtonString;
                }
                return "点击下载";
            }
        }
        if (!TextUtils.isEmpty(actButtonString)) {
            return actButtonString;
        }
        return "查看详情";
    }
}
