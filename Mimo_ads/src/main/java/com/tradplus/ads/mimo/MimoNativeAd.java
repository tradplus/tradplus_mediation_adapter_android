package com.tradplus.ads.mimo;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.miui.zeus.mimo.sdk.FeedAd;
import com.miui.zeus.mimo.sdk.NativeAd;
import com.miui.zeus.mimo.sdk.NativeAdData;
import com.miui.zeus.mimo.sdk.TemplateAd;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class MimoNativeAd extends TPBaseAd {

    private int mIsRender;
    private NativeAd mNativeAd; //自渲染
    private TemplateAd mTemplateAd; //模版
    private FrameLayout mFrameLayout;
    private TPNativeAdView mTPNativeAdView;
    private FeedAd mFeedAd; // 模版 原生视频
    private Activity mActivity;
    private boolean isAdShown = true;
    public static final String TAG = "MimoNative";

    public MimoNativeAd(FeedAd feedAd, Activity activity, int isRender) {
        //模版视频
        mFeedAd = feedAd;
        mActivity = activity;
        mIsRender = isRender;
        mFrameLayout = new FrameLayout(activity);
        View adView = mFeedAd.getAdView();
        if (adView == null) {
            TPError tpError = new TPError(TPError.SHOW_FAILED);
            tpError.setErrorMessage("mFeedAd.getAdView() == null");
            mShowListener.onAdVideoError(tpError);
            return;
        }
        mFrameLayout.addView(adView);
    }

    public MimoNativeAd(NativeAd nativeAd, NativeAdData nativeAdData, int isRender) {
        // 自渲染
        mNativeAd = nativeAd;
        mIsRender = isRender;
        initNative(nativeAdData);
    }

    public MimoNativeAd(TemplateAd templateAd, Context context, int isRender) {
        // 模版 图片
        mTemplateAd = templateAd;
        mFrameLayout = new FrameLayout(context);
        mIsRender = isRender;
        templateAd.show(mFrameLayout, mTemplateAdInteractionListener);
    }

    private void initNative(NativeAdData nativeAdData) {
        mTPNativeAdView = new TPNativeAdView();

        if (nativeAdData.getTitle() != null) {
            mTPNativeAdView.setTitle(nativeAdData.getTitle());
        }

        if (nativeAdData.getDesc() != null) {
            mTPNativeAdView.setSubTitle(nativeAdData.getDesc());
        }
        if (nativeAdData.getButtonText() != null && !TextUtils.isEmpty(nativeAdData.getButtonText())) {
            Log.i(TAG, "ButtonText: " + nativeAdData.getButtonText());
            mTPNativeAdView.setCallToAction(nativeAdData.getButtonText());
        } else {
            mTPNativeAdView.setCallToAction("立即下载");
        }

        if (nativeAdData.getIconUrl() != null && !TextUtils.isEmpty(nativeAdData.getIconUrl())) {
            mTPNativeAdView.setIconImageUrl(nativeAdData.getIconUrl());
        }

        if (nativeAdData.getAdMark() != null) { //广告标识
            Log.i(TAG, "initNative: " + nativeAdData.getAdMark());
            mTPNativeAdView.setAdChoiceUrl(nativeAdData.getAdMark());
        }

        if (getVideoStyle(nativeAdData)) {
            Log.i(TAG, "VideoUrl: " + nativeAdData.getVideoUrl());
            mTPNativeAdView.setVideoUrl(nativeAdData.getVideoUrl());
        } else {
            if (nativeAdData.getImageList() != null && nativeAdData.getImageList().size() > 0) {
                Log.i(TAG, "ImageUrl: " + nativeAdData.getImageList().get(0));
                List<String> imageList = nativeAdData.getImageList();
                mTPNativeAdView.setMainImageUrl(imageList.get(0));
            }
        }

        List<String> imageList = nativeAdData.getImageList();
        if (imageList != null && imageList.size() > 0) {
            mTPNativeAdView.setPicUrls(imageList);
        }
    }


    private boolean getVideoStyle(NativeAdData nativeAdData) {
        int style = nativeAdData.getAdStyle();
        Log.i(TAG, "AdStyle: " + style);
        // 视频 || 图片+视频混出
        if (style == NativeAdData.AD_STYLE_VIDEO || style == NativeAdData.AD_STYLE_IMAGE_AND_VIDEO) {
            return nativeAdData.getVideoUrl() != null && !TextUtils.isEmpty(nativeAdData.getVideoUrl());
        }
        return false;
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
        }
        return super.getDownloadImgUrls();
    }

    // 模版监听
    private final TemplateAd.TemplateAdInteractionListener mTemplateAdInteractionListener = new TemplateAd.TemplateAdInteractionListener() {
        @Override
        public void onAdShow() {
            // 因为mimo loaded后会立马调用onAdShow，TP端showlistener此时还没有生成
            // 就一直不回有1300回调，只能做延迟处理。
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "onAdShow: ");
                    if (mShowListener != null) {
                        mShowListener.onAdShown();
                    }
                }
            }, 1000);
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onAdDismissed() {
            Log.i(TAG, "onAdDismissed: ");
            if (mShowListener != null) {
                mShowListener.onAdClosed();
            }
        }

        @Override
        public void onAdRenderFailed(int code, String msg) {
            Log.i(TAG, "onAdRenderFailed: code ==" + code + " , msg ==" + msg);
            if (mShowListener != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(code + "");
                tpError.setErrorMessage(msg);
                mShowListener.onAdVideoError(tpError);
            }
        }
    };

    @Override
    public Object getNetworkObj() {
        if (mNativeAd != null) {
            return mNativeAd;
        }else if (mTemplateAd != null) {
            return mTemplateAd;
        }

        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mNativeAd != null) {
            for (View view : clickViews) {
                mNativeAd.registerAdView(view, new NativeAd.NativeAdInteractionListener() {
                    @Override
                    public void onAdClick() {
                        Log.i(TAG, "onAdClick: ");
                        if (mShowListener != null) {
                            mShowListener.onAdClicked();
                        }
                    }

                    @Override
                    public void onAdShow() {
                        if (isAdShown) {
                            // Mimo BUG 会多次回调onAdShow
                            Log.i(TAG, "onAdShow: ");
                            if (mShowListener != null) {
                                mShowListener.onAdShown();
                            }
                            isAdShown = false;
                        }
                    }
                });

            }
        }

        if (mFeedAd != null)
            mFeedAd.registerInteraction(mActivity, mFrameLayout, new FeedAd.FeedInteractionListener() {
                @Override
                public void onAdShow() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "onAdShow: ");
                            if (mShowListener != null) {
                                mShowListener.onAdShown();
                            }
                        }
                    }, 1000);

                }

                @Override
                public void onAdClick() {
                    Log.i(TAG, "onAdClick: ");
                    if (mShowListener != null) {
                        mShowListener.onAdClicked();
                    }
                }

                @Override
                public void onAdClosed() {
                    Log.i(TAG, "onAdClosed: ");
                    if (mShowListener != null) {
                        mShowListener.onAdClosed();
                    }
                }

                @Override
                public void onRenderFail(int errorCode, String message) {
                    Log.i(TAG, "onRenderFail: errorCode :" + errorCode + " , message :" + message);
                    if (mShowListener != null) {
                        TPError tpError = new TPError(SHOW_FAILED);
                        tpError.setErrorCode(errorCode + "");
                        tpError.setErrorMessage(message);
                        mShowListener.onAdVideoError(tpError);
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
                public void onVideoPause() {

                }

                @Override
                public void onVideoResume() {

                }
            });

    }


    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (mIsRender == TPBaseAd.AD_TYPE_NORMAL_NATIVE) {
            return AD_TYPE_NORMAL_NATIVE; //自渲染 ，下发isRender == 2
        } else {
            return AD_TYPE_NATIVE_EXPRESS; //模版
        }
    }

    @Override
    public View getRenderView() {
        return mFrameLayout == null ? null : mFrameLayout;
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
//        if (mNativeAd != null) {
//            mNativeAd.destroy();
//            mNativeAd = null;
//        }
//
//        if (mTemplateAd != null) {
//            mTemplateAd.destroy();
//            mTemplateAd = null;
//        }
//
//        if (mFeedAd != null) {
//            mFeedAd.destroy();
//            mFeedAd = null;
//        }
//
//        if (mFrameLayout != null) {
//            mFrameLayout.removeAllViews();
//        }
    }
}
