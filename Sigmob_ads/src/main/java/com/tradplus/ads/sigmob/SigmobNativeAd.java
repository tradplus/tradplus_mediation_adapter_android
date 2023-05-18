package com.tradplus.ads.sigmob;

import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sigmob.windad.WindAdError;
import com.sigmob.windad.natives.NativeADEventListener;
import com.sigmob.windad.natives.NativeAdPatternType;
import com.sigmob.windad.natives.WindNativeAdData;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class SigmobNativeAd extends TPBaseAd {

    private WindNativeAdData mNativeADData;
    private Context mContext;
    private TPNativeAdView mTPNativeAdView;
    private FrameLayout frameLayout;
    private int adPatternType;
    private boolean isAdShown = true;
    public static final String TAG = "SigmobNative";

    public SigmobNativeAd(Context context, WindNativeAdData nativeADData) {
        mContext = context;
        mNativeADData = nativeADData;
        initNative(context, nativeADData);
    }

    private void initNative(Context context, WindNativeAdData nativeADData) {
        mTPNativeAdView = new TPNativeAdView();
        adPatternType = mNativeADData.getAdPatternType();
        Log.i(TAG, "AdPatternType: " + adPatternType);

        if (nativeADData.getTitle() != null) {
            mTPNativeAdView.setTitle(nativeADData.getTitle());
        }

        if (nativeADData.getDesc() != null) {
            mTPNativeAdView.setSubTitle(nativeADData.getDesc());
        }

        if (nativeADData.getCTAText() != null) {
            mTPNativeAdView.setCallToAction(nativeADData.getCTAText());
        }

        if (nativeADData.getIconUrl() != null) {
            mTPNativeAdView.setIconImageUrl(nativeADData.getIconUrl());
        }
        if (nativeADData.getAdLogo() != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(nativeADData.getAdLogo());
            mTPNativeAdView.setAdChoiceImage(bitmapDrawable);
        }

        frameLayout = new FrameLayout(mContext);
        if (adPatternType == NativeAdPatternType.NATIVE_VIDEO_AD){
            mTPNativeAdView.setMediaView(frameLayout);
        }
    }

    @Override
    public Object getNetworkObj() {
        return mNativeADData == null ? null : mNativeADData;
    }


    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mNativeADData == null) return;

        //重要! 这个涉及到广告计费，必须正确调用。convertView必须使用ViewGroup。
        //作为creativeViewList传入，点击不进入详情页，直接下载或进入落地页，视频和图文广告均生效
        /**
         * @param view                  自渲染的根View
         * @param clickableViews        可点击的View的列表
         * @param creativeViewList      用于下载或者拨打电话的View
         * @param disLikeView           dislike按钮
         * @param nativeAdEventListener 点击回调
         */
        mNativeADData.bindViewForInteraction(viewGroup, clickViews, clickViews, null, mNativeADEventListener);
        ImageView tp_image = (ImageView)viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_IMAGE);

        //需要等到bindViewForInteraction后再去添加media
        if (adPatternType == NativeAdPatternType.NATIVE_VIDEO_AD) {
            // @param mediaLayout 装video的容器
            mNativeADData.bindMediaView(frameLayout, new WindNativeAdData.NativeADMediaListener() {
                @Override
                public void onVideoLoad() {

                }

                @Override
                public void onVideoError(WindAdError windAdError) {
                    Log.i(TAG, "onVideoError: code:"+ windAdError.getErrorCode() + " msg:"+windAdError.getMessage());
                    if (mShowListener != null) {
                        TPError tpError = new TPError(SHOW_FAILED);
                        tpError.setErrorCode(windAdError.getErrorCode()+"");
                        tpError.setErrorMessage(windAdError.getMessage());
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

                @Override
                public void onVideoCompleted() {
                    Log.i(TAG, "onVideoCompleted: ");
                    if (mShowListener != null) {
                        mShowListener.onAdVideoEnd();
                    }
                }
            });
        } else {
            List<ImageView> imageViews = new ArrayList<>();
            if (tp_image != null) {
                imageViews.add(tp_image);
                Log.i(TAG, "tp_image: " +tp_image);
                mNativeADData.bindImageViews(imageViews, 0);
            }
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity != null) {
            //设置dislike弹窗
            mNativeADData.setDislikeInteractionCallback(activity, new WindNativeAdData.DislikeInteractionCallback() {
                @Override
                public void onShow() {
                    Log.i(TAG, "onShow: ");
                }

                @Override
                public void onSelected(int position, String value, boolean enforce) {
                    Log.i(TAG, "onSelected: " + position + ":" + value + ":" + enforce);
//                if (windContainer != null) {
//                    windContainer.removeAllViews();
//                }
                    if (mShowListener != null) {
                        mShowListener.onAdClosed();
                    }
                }

                @Override
                public void onCancel() {
                    Log.i(TAG, "onADExposed: ");
                }
            });
        }


    }

    private final NativeADEventListener mNativeADEventListener = new NativeADEventListener() {
        @Override
        public void onAdExposed() {
            Log.i(TAG, "onAdExposed: ");
            if (isAdShown) {
                if (mShowListener != null) {
                    mShowListener.onAdShown();
                }
                isAdShown = false;
            }
        }

        @Override
        public void onAdClicked() {
            Log.i(TAG, "onAdClicked: ");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onAdDetailShow() {

        }

        @Override
        public void onAdDetailDismiss() {

        }

        @Override
        public void onAdError(WindAdError windAdError) {
            Log.i(TAG, "onADError: code == " + windAdError.getErrorCode() + " , Msg ==" + windAdError.getMessage());
            if (mShowListener != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorCode(windAdError.getErrorCode() + "");
                tpError.setErrorMessage(windAdError.getMessage());
                mShowListener.onAdVideoError(tpError);
            }
        }
    };

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        return AD_TYPE_NORMAL_NATIVE; //自渲染
    }

    @Override
    public View getRenderView() {
        return null;
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
        if (mNativeADData != null) {
            mNativeADData.destroy();
        }
    }
}
