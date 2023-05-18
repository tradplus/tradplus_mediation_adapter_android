package com.tradplus.ads.verve;

import static android.view.Gravity.CENTER;

import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;

import net.pubnative.lite.sdk.models.NativeAd;

import java.util.ArrayList;
import java.util.List;

public class VerveNativeAd extends TPBaseAd {

    private static final String TAG = "VerveNative";
    private TPNativeAdView mTPNativeAdView;
    private NativeAd mNativeAd;
    private View mAdChoiceView;
    private Context mContext;

    public VerveNativeAd(Context context, NativeAd nativeAd) {
        mContext = context;
        mNativeAd = nativeAd;
        initView(context, nativeAd);
    }

    private void initView(Context context, NativeAd nativeAd) {
        mTPNativeAdView = new TPNativeAdView();
        if (nativeAd.getDescription() != null) {
            mTPNativeAdView.setSubTitle(nativeAd.getDescription());
        }

        Log.i(TAG, "CAT: " + nativeAd.getCallToActionText());
        if (nativeAd.getCallToActionText() != null) {
            mTPNativeAdView.setCallToAction(nativeAd.getCallToActionText());
        }

        if (nativeAd.getTitle() != null) {
            mTPNativeAdView.setTitle(nativeAd.getTitle());
        }

        if (nativeAd.getIconUrl() != null) {
            mTPNativeAdView.setIconImageUrl(nativeAd.getIconUrl());
        }

        if (nativeAd.getContentInfo(context) != null) {
            mAdChoiceView = nativeAd.getContentInfo(mContext);

        }

        if (nativeAd.getBannerUrl() != null) {
            mTPNativeAdView.setMainImageUrl(nativeAd.getBannerUrl());
        }

    }

    @Override
    public ArrayList<String> getDownloadImgUrls() {
        if (mNativeAd != null) {
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

    @Override
    public Object getNetworkObj() {
        return mNativeAd;
    }

    @Override
    public void registerClickView(ViewGroup viewGroup, ArrayList<View> clickViews) {
        if (mNativeAd == null) {
            TPError tpError = new TPError(UNSPECIFIED);
            Log.i(TAG, "registerClickView Failed, NativeAd == null");
            tpError.setErrorMessage("registerClickView Failed, NativeAd == null");
            mShowListener.onAdVideoError(tpError);
            return;
        }

        FrameLayout adChoicesView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_ADCHOICES);
        if (adChoicesView != null && mAdChoiceView != null) {
            adChoicesView.removeAllViews();
            adChoicesView.addView(mAdChoiceView, 0);
        }

        View ctaView = viewGroup.findViewWithTag(TPBaseAd.NATIVE_AD_TAG_CALLTOACTION);
        if (ctaView != null) {
            ctaView.setClickable(false);
        }

        mNativeAd.startTracking(viewGroup, new NativeAd.Listener() {
            @Override
            public void onAdImpression(NativeAd ad, View view) {
                Log.i(TAG, "onAdImpression: ");
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
        });
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
    public List<View> getMediaViews() {
        return null;
    }

    @Override
    public ViewGroup getCustomAdContainer() {
        return null;
    }

    @Override
    public View getRenderView() {
        return null;
    }

    @Override
    public void clean() {
        // Finally stop tracking the ad when the activity or fragment are destroyed
        if (mNativeAd != null) {
            mNativeAd.stopTracking();
            mNativeAd = null;
        }
    }
}
