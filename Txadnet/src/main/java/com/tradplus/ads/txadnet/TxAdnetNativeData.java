package com.tradplus.ads.txadnet;

import static android.view.Gravity.CENTER;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.ads.nativ.NativeUnifiedADAppMiitInfo;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.ArrayList;
import java.util.List;

public class TxAdnetNativeData extends TPBaseAd {
    private int isRender;
    private TPNativeAdView mTPNativeAdView;
    private NativeUnifiedADData nativeUnifiedADData;
    private List<NativeUnifiedADData> nativeUnifiedADDataList;
    private NativeAdContainer mNativeAdContainer;
    private NativeExpressADView mNativeExpressADView;
    private RelativeLayout mRelativeLayout;
    private Context mCtx;
    private static final String TAG = "GDTNativeAd";
    private MediaView mediaView;
    private boolean mIsVideoSoundEnable;
    private VideoOption mVideoOption;

    public TxAdnetNativeData(NativeUnifiedADData adData, Context context, boolean isVideoSoundEnable) {
        if (adData == null || context == null) {
            return;
        }


        mCtx = context;
        mNativeAdContainer = new NativeAdContainer(context);
        mIsVideoSoundEnable = isVideoSoundEnable;
        initViewData(adData, context);
    }

    public TxAdnetNativeData(List<NativeUnifiedADData> list, Context context, boolean isVideoSoundEnable) {
        if (list == null || context == null) {
            return;
        }


        mCtx = context;
        mIsVideoSoundEnable = isVideoSoundEnable;
        nativeUnifiedADDataList = list;
    }

    public TxAdnetNativeData(Context context, NativeExpressADView nativeExpressADView) {
        mNativeExpressADView = nativeExpressADView;
        mRelativeLayout = new RelativeLayout(context);
        mRelativeLayout.addView(nativeExpressADView);
        mRelativeLayout.setGravity(CENTER);
    }

    private void initViewData(NativeUnifiedADData nativeData, Context context) {
        nativeUnifiedADData = nativeData;
        mTPNativeAdView = setMiitInfo(nativeData);

        String title = nativeData.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String desc = nativeData.getDesc();
        if (!TextUtils.isEmpty(desc)) {
            mTPNativeAdView.setSubTitle(desc);
        }

        String buttonText = nativeData.getButtonText();
        if (!TextUtils.isEmpty(buttonText)) {
            // 是否是微信原生页广告
            if (nativeData.isWeChatCanvasAd()) {
                // 只有加入白名单的应用支持该类型的广告
                // 如果是该类型广告建议将广告容器中按钮的文案设置为“去微信看看”
                mTPNativeAdView.setCallToAction("去微信看看");
            } else {
                mTPNativeAdView.setCallToAction(buttonText);
            }
        }

        String iconUrl = nativeData.getIconUrl();
        if (!TextUtils.isEmpty(iconUrl)) {
            mTPNativeAdView.setIconImageUrl(iconUrl);
        }


        int adPatternType = nativeData.getAdPatternType();
        if (adPatternType == AdPatternType.NATIVE_VIDEO) {
            // 视频素材
            mediaView = new MediaView(mCtx);
            mediaView.setBackgroundColor(0xff000000);
            Log.i(TAG, "NATIVE_VIDEO");
            ViewGroup.LayoutParams _params = mediaView.getLayoutParams();
            if (_params == null) {
                _params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            mediaView.setLayoutParams(_params);
//            mNativeAdContainer.addView(mediaView);
            mTPNativeAdView.setMediaView(mediaView);
        }

        if (adPatternType == AdPatternType.NATIVE_2IMAGE_2TEXT ||
                adPatternType == AdPatternType.NATIVE_1IMAGE_2TEXT) {
            // 双图双文、单图双文
            Log.i(TAG, "IMAGE:");
            mTPNativeAdView.setMainImageUrl(nativeData.getImgUrl());
        }

        if (adPatternType == AdPatternType.NATIVE_3IMAGE ) {
            List<String> imgList = nativeData.getImgList();
            if (imgList != null && imgList.size() > 0) {
                mTPNativeAdView.setMainImageUrl(imgList.get(0));
                mTPNativeAdView.setPicUrls(imgList);
            }
        }
    }


    private TPNativeAdView setMiitInfo(NativeUnifiedADData nativeData) {
        TPNativeAdView nativeAdView = new TPNativeAdView();
        NativeUnifiedADAppMiitInfo nativeUnifiedADAppMiitInfo = nativeData.getAppMiitInfo();
        if (nativeUnifiedADAppMiitInfo != null) {
            nativeAdView.setAppName(nativeUnifiedADAppMiitInfo.getAppName());
            nativeAdView.setAuthorName(nativeUnifiedADAppMiitInfo.getAuthorName());
            nativeAdView.setPackageSizeBytes(nativeUnifiedADAppMiitInfo.getPackageSizeBytes());
            nativeAdView.setPermissionsUrl(nativeUnifiedADAppMiitInfo.getPermissionsUrl());
            nativeAdView.setPrivacyAgreement(nativeUnifiedADAppMiitInfo.getPrivacyAgreement());
            nativeAdView.setVersionName(nativeUnifiedADAppMiitInfo.getVersionName());
        }

        return nativeAdView;
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        if (nativeUnifiedADData != null) {
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

    public void setRenderType(int type) {
        isRender = type;
    }

    public void setVideoOption(VideoOption videoOption) {
        mVideoOption = videoOption;
    }

    @Override
    public Object getNetworkObj() {
        if (nativeUnifiedADData != null) {
            return nativeUnifiedADData;
        } else if (mNativeExpressADView != null) {
            return mNativeExpressADView;
        } else if (nativeUnifiedADDataList != null) {
            return nativeUnifiedADDataList;
        }
        return null;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {

    }

    @Override
    public void registerClickAfterRender(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (isRender == AppKeyManager.TEMPLATE_RENDERING_NO || isRender == AppKeyManager.TEMPLATE_PATCH_RENDERING_NO) {
            if (nativeUnifiedADData != null && mNativeAdContainer != null) {
                // 最后两个list，List clickViews, List customClickViews
                // 开发者不能对clickViews和customClickViews设置OnClickListener，会影响点击事件的上报
                // clickViews为触发广告点击行为的View，必须在container中，不然不会响应点击事件
                // customClickViews点击可以直接下载或进入落地页
                nativeUnifiedADData.bindAdToView(mCtx, mNativeAdContainer, null, clickViews, null);

                if (nativeUnifiedADData.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
                    try {
                        // 视频广告，注册mMediaView的点击事件
                        nativeUnifiedADData.bindMediaView(mediaView, mVideoOption, nativeADMediaListener);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    public void registerClickAfterRender(NativeUnifiedADData nativeUnifiedADData, NativeAdContainer nativeAdContainer, MediaView mediaView, NativeADMediaListener nativeADMediaListener, ArrayList<View> clickViews) {
        if (nativeUnifiedADData != null) {
            nativeUnifiedADData.bindAdToView(mCtx, nativeAdContainer, null, null, clickViews);
            if (mediaView != null) {
                try {
                    nativeUnifiedADData.bindMediaView(mediaView, mVideoOption, nativeADMediaListener == null ? this.nativeADMediaListener : nativeADMediaListener);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

        }
    }


    // 视频相关回调
    final NativeADMediaListener nativeADMediaListener = new NativeADMediaListener() {
        @Override
        public void onVideoInit() {
            Log.d(TAG, "onVideoInit: ");
        }

        @Override
        public void onVideoLoading() {
            Log.d(TAG, "onVideoLoading: ");
        }

        @Override
        public void onVideoReady() {
            Log.d(TAG, "onVideoReady: ");
        }

        @Override
        public void onVideoLoaded(int videoDuration) {
            Log.d(TAG, "onVideoLoaded: ");

        }

        @Override
        public void onVideoStart() {
            Log.d(TAG, "onVideoStart: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoStart();
            }
        }

        @Override
        public void onVideoStop() {
            Log.i(TAG, "onVideoStop: ");
        }

        @Override
        public void onVideoPause() {
            Log.d(TAG, "onVideoPause: ");
        }

        @Override
        public void onVideoResume() {
            Log.d(TAG, "onVideoResume: ");
        }

        @Override
        public void onVideoClicked() {
            //自渲染视频广告中，如果setEnableUserControl设置为true时，用户点击视频区域时将收到此回调

            // 此时并不是广告点击

            Log.i(TAG, "onVideoClicked: ");
//            if (mShowListener != null) {
//                mShowListener.onAdClicked();
//            }
        }

        @Override
        public void onVideoCompleted() {
            Log.d(TAG, "onVideoCompleted: ");
            if (mShowListener != null) {
                mShowListener.onAdVideoEnd();
            }
        }

        @Override
        public void onVideoError(AdError error) {
            Log.d(TAG, "onVideoError: " + error.getErrorCode() + ", msg :" + error.getErrorMsg());
            if (mShowListener != null) {
                TPError tpError = new TPError(TPError.SHOW_FAILED);
                tpError.setErrorCode(error.getErrorCode() + "");
                tpError.setErrorMessage(error.getErrorMsg() + "");
                mShowListener.onAdVideoError(tpError);
            }
        }
    };

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        if (nativeUnifiedADData != null && nativeUnifiedADData.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
            nativeUnifiedADData.pauseVideo();
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        if (nativeUnifiedADData != null && nativeUnifiedADData.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
            nativeUnifiedADData.resume();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    nativeUnifiedADData.resumeVideo();
                }
            }, 1000);

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

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        if (nativeUnifiedADDataList != null) {
            return AD_TYPE_NATIVE_LIST;
        } else {
            if (isRender == AppKeyManager.TEMPLATE_RENDERING_NO || isRender == AppKeyManager.TEMPLATE_PATCH_RENDERING_NO) {
                return AD_TYPE_NORMAL_NATIVE; //自渲染 ，下发isRender == 2 5
            } else {
                return AD_TYPE_NATIVE_EXPRESS; //模版，下发isRender == 1 4
            }
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
    public List<Object> getUnifiedDrawAdData() {
        if (nativeUnifiedADDataList != null) {
            for (int i = 0; i < nativeUnifiedADDataList.size(); i++) {
                drawAdObject.add(nativeUnifiedADDataList.get(i));
            }
        }

        return super.getUnifiedDrawAdData();
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        if (nativeUnifiedADData != null) {
            return mNativeAdContainer;
        }
        return null;
    }

    @Override
    public void clean() {
        if (mNativeExpressADView != null) {
            mNativeExpressADView.setMediaListener(null);
            mNativeExpressADView.destroy();
            mNativeExpressADView = null;
        }

        if (nativeUnifiedADData != null) {
            nativeUnifiedADData.setNativeAdEventListener(null);
            nativeUnifiedADData.destroy();
            nativeUnifiedADData = null;
        }

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

    public void adVideoStart() {
        if (mShowListener != null) {
            mShowListener.onAdVideoEnd();
        }
    }

    public void adVideoEnd() {
        if (mShowListener != null) {
            mShowListener.onAdVideoEnd();
        }
    }

    @Override
    public void setNetworkExtObj(Object obj) {
        Log.i(TAG, "setNetworkExtObj: ");
        if (obj instanceof DownloadConfirmListener) {
            Log.i(TAG, "DownloadConfirmListener: ");
            if (nativeUnifiedADData != null) {
                nativeUnifiedADData.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }

            if (mNativeExpressADView != null) {
                mNativeExpressADView.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }
        }

    }

}
