package com.tradplus.crosspro.ui;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.common.serialization.JSON;
import com.tradplus.ads.common.util.BitmapUtil;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.CommonUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.base.network.util.ImageLoader;
import com.tradplus.ads.base.network.util.ResourceEntry;
import com.tradplus.crosspro.ui.util.ViewUtil;

import static com.tradplus.ads.base.util.TradPlusDataConstants.DEVICE_TYPE_MOBILE;
import static com.tradplus.ads.base.util.TradPlusDataConstants.SCREEN_LAND_TYPE;


public class EndCardView extends RelativeLayout {
    private OnEndCardListener mListener;
    private int mWidth;
    private int mHeight;


    private int mBlurBgIndex = 0;
    private int mEndCardIndex = 1;
    private int mCloseButtonIndex = 2;
    private int mOrientation;

    private ImageView mEndCardIv;
    private RoundImageView bgIv;
    private int direction;

    public EndCardView(ViewGroup container, int width, int height, CPAdResponse cpAdResponse, int orientation,OnEndCardListener listener,int direction) {
        super(container.getContext());
        this.mListener = listener;
        this.mOrientation = orientation;
        this.direction = direction;

        this.mWidth = width;
        this.mHeight = height;

        init();
        attachTo(container);

        loadBitmap(cpAdResponse);
    }

    private String getEndCardWithDeviceType(CPAdResponse cpAdResponse, int direction) {
        LogUtil.ownShow("getEnd_cardcpAdResponse = " + JSON.toJSONString(cpAdResponse));
        TPDataManager tpDataManager = TPDataManager.getInstance();
        String device_type = tpDataManager.getDeviceType();
            if (TextUtils.equals(device_type, DEVICE_TYPE_MOBILE)) {
                String landUrl;
                if (mOrientation == SCREEN_LAND_TYPE) {
                    landUrl = getEndCardByIndex(cpAdResponse, 1);
                    if (TextUtils.isEmpty(landUrl)) {
                        mEndCardIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        landUrl = getEndCardByIndex(cpAdResponse, 0);
                    }
                } else {

                    landUrl = getEndCardByIndex(cpAdResponse, 0);
                    if (TextUtils.isEmpty(landUrl)) {
                        mEndCardIv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        landUrl = getEndCardByIndex(cpAdResponse, 1);
                    }
                }
                return landUrl;
            } else {
                String landUrl;
                if (mOrientation == SCREEN_LAND_TYPE) {
                    landUrl = getEndCardByIndex(cpAdResponse, 3);
                    if (TextUtils.isEmpty(landUrl)) {
                        landUrl = getEndCardByIndex(cpAdResponse, 2);
                    }
                } else {
                    landUrl = getEndCardByIndex(cpAdResponse, 2);
                    if (TextUtils.isEmpty(landUrl)) {
                        landUrl = getEndCardByIndex(cpAdResponse, 3);
                    }
                }
                return landUrl;
            }
    }

    private String getEndCardByIndex(CPAdResponse cpAdResponse,int index){
        for(int i = 0; i < cpAdResponse.getEnd_card().size();i++){
            if(cpAdResponse.getEnd_card().get(i).getType().equals((index+1)+"")){
                return cpAdResponse.getEnd_card().get(i).getUrl();
            }
        }
        return "";
    }

    private void loadBitmap(final CPAdResponse cpAdResponse) {
        try {
            ImageLoader.getInstance(getContext()).load(new ResourceEntry(ResourceEntry.INTERNAL_CACHE_TYPE, getEndCardWithDeviceType(cpAdResponse,direction)), mWidth, mHeight, new ImageLoader.ImageLoaderListener() {
                @Override
                public void onSuccess(String url, Bitmap bitmap) {
                    if (TextUtils.equals(url, getEndCardWithDeviceType(cpAdResponse,direction))) {
                        mEndCardIv.setImageBitmap(bitmap);
                        Bitmap blurBitmap = BitmapUtil.blurBitmap(getContext(), bitmap);
                        bgIv.setImageBitmap(blurBitmap);
                    }
                }

                @Override
                public void onFail(String url, String errorMsg) {
                    LogUtil.ownShow("getend_card url = "+url+" emsg = "+errorMsg);
                }
            });
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        bgIv = new RoundImageView(getContext());
        bgIv.setScaleType(ImageView.ScaleType.CENTER_CROP);

        mEndCardIv = new RoundImageView(getContext());

        RelativeLayout.LayoutParams rl_bg = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        RelativeLayout.LayoutParams rl_endcard = new RelativeLayout.LayoutParams(mWidth, mHeight);
        rl_endcard.addRule(RelativeLayout.CENTER_IN_PARENT);

        addView(bgIv, mBlurBgIndex, rl_bg);
        addView(mEndCardIv, mEndCardIndex, rl_endcard);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickEndCard();
                }
            }
        });

        initCloseButton();
    }

    private void initCloseButton() {

        if (getChildAt(mCloseButtonIndex) != null) {
            removeViewAt(mCloseButtonIndex);
        }

        ImageView mCloseBtn = new ImageView(getContext());
        mCloseBtn.setImageResource(CommonUtil.getResId(getContext(), "cp_video_close", "drawable"));

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 29, getContext().getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getContext().getResources().getDisplayMetrics());
        int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 19, getContext().getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(size, size);
        rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rl.rightMargin = rightMargin;
        rl.topMargin = topMargin;
        addView(mCloseBtn, mCloseButtonIndex, rl);

        //扩大点击区域
        ViewUtil.expandTouchArea(mCloseBtn, size / 2);

        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onCloseEndCard();
                }
            }
        });
    }

    private void attachTo(ViewGroup container) {
        if (container.getChildCount() == 2) {
            container.removeViewAt(0);
        }

        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(this, 0, rl);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public interface OnEndCardListener {
        void onClickEndCard();

        void onCloseEndCard();
    }
}
