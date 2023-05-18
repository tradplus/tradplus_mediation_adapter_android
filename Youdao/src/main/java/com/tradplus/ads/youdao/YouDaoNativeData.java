package com.tradplus.ads.youdao;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdView;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.youdao.sdk.common.YouDaoAd;
import com.youdao.sdk.nativeads.NativeResponse;

import java.util.ArrayList;
import java.util.List;

public class YouDaoNativeData extends TPBaseAd {

    private NativeResponse mNativeResponse;
    private TPNativeAdView mTPNativeAdView;
    private static final String TAG = "YouDaoNative";

    public YouDaoNativeData(NativeResponse nativeResponse) {
        if (nativeResponse != null) {
            initViewData(nativeResponse);
        }
    }

    private void initViewData(NativeResponse nativeData) {
        mNativeResponse = nativeData;
        mTPNativeAdView = new TPNativeAdView();

        String title = nativeData.getTitle();
        if (!TextUtils.isEmpty(title)) {
            mTPNativeAdView.setTitle(title);
        }

        String text = nativeData.getText();
        if (!TextUtils.isEmpty(text)) {
            mTPNativeAdView.setSubTitle(text);
        }

        String callToAction = nativeData.getCallToAction();
        if (!TextUtils.isEmpty(callToAction)) {
            mTPNativeAdView.setCallToAction(callToAction);
        }

        String iconImageUrl = nativeData.getIconImageUrl();
        if (!TextUtils.isEmpty(iconImageUrl)) {
            mTPNativeAdView.setIconImageUrl(iconImageUrl);
        }

        String mainImageUrl = nativeData.getMainImageUrl();
        if (!TextUtils.isEmpty(mainImageUrl)) {
            mTPNativeAdView.setMainImageUrl(mainImageUrl);
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

    @Override
    public Object getNetworkObj() {
        if (mNativeResponse != null) {
            return mNativeResponse;
        }
        return null;
    }

    @Override
    public void registerClickView(final ViewGroup viewGroup, ArrayList<View> clickViews) {
        mNativeResponse.realRecordImpression(viewGroup);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: ");
                if (mNativeResponse != null) {
                    mNativeResponse.handleClick(view);
                }
            }
        };

        for (View view : clickViews) {
            view.setOnClickListener(listener);
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
}
