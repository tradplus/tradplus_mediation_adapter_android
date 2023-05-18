package com.tradplus.ads.fpangolin;

import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGImageItem;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGMediaView;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionListener;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import java.util.ArrayList;
import java.util.List;

public class ToutiaoNativeAd extends TPBaseAd {

    private PAGNativeAd mPAGNativeAd;
    private TPNativeAdView mTPNativeAdView;
    public static final String TAG = "PangleNative";
    private int onAdShow = 0;

    public ToutiaoNativeAd(PAGNativeAd pagNativeAd) {
        mPAGNativeAd = pagNativeAd;

        PAGNativeAdData nativeAdData = pagNativeAd.getNativeAdData();
        if (nativeAdData == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(UNSPECIFIED));
            }
            Log.i(TAG, "onAdLoaded ,but nativeAdData == null");
            return;
        }

        initViewData(nativeAdData);

    }


    private void initViewData(PAGNativeAdData nativeAdData) {
        mTPNativeAdView = new TPNativeAdView();

        String title = nativeAdData.getTitle();
        if (title != null) {
            mTPNativeAdView.setTitle(title);
        }

        ImageView adLogoView = (ImageView)nativeAdData.getAdLogoView();
        if (adLogoView != null) {
            mTPNativeAdView.setAdChoiceImage(adLogoView.getDrawable());
        }

        String description = nativeAdData.getDescription();
        if (description != null) {
            mTPNativeAdView.setSubTitle(description);
        }

        String buttonText = nativeAdData.getButtonText();
        if (buttonText != null) {
            mTPNativeAdView.setCallToAction(buttonText);
        }

        PAGImageItem icon = nativeAdData.getIcon();
        if (icon != null) {
            String imageUrl = icon.getImageUrl();
            if (imageUrl != null) {
                mTPNativeAdView.setIconImageUrl(imageUrl);
            } else {
                Log.i(TAG, "imageUrl == null");
            }
        } else {
            Log.i(TAG, "icon == null");
        }

        PAGMediaView mediaView = nativeAdData.getMediaView();
        //Returns an advertisement View of type PAGMediaView, where PAGMediaView may be a picture or a video View
        if (mediaView != null) {
            mTPNativeAdView.setMediaView(mediaView);
        }
    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        downloadImgUrls.clear();

        if (!TextUtils.isEmpty(mTPNativeAdView.getIconImageUrl())) {
            downloadImgUrls.add(mTPNativeAdView.getIconImageUrl());
        }

        return super.getDownloadImgUrls();
    }

    @Override
    public Object getNetworkObj() {
        return mPAGNativeAd == null ? null : mPAGNativeAd;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {

        if (mPAGNativeAd != null) {
            mPAGNativeAd.registerViewForInteraction(viewGroup, clickViews, clickViews, null, new PAGNativeAdInteractionListener() {
                @Override
                public void onAdShowed() {
                    Log.i(TAG, "onAdShowed: ");
                    if (mShowListener != null) {
                        mShowListener.onAdShown();
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
                public void onAdDismissed() {
                    Log.i(TAG, "onAdDismissed: ");
                    if (mShowListener != null) {
                        mShowListener.onAdClosed();
                    }
                }
            });
        }
    }

    @Override
    public TPNativeAdView getTPNativeView() {
        return mTPNativeAdView;
    }

    @Override
    public int getNativeAdType() {
        return AD_TYPE_NORMAL_NATIVE;
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

    }
}
