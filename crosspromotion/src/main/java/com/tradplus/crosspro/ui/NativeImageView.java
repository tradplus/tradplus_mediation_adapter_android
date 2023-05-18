package com.tradplus.crosspro.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tradplus.ads.base.network.util.ImageLoader;
import com.tradplus.ads.base.network.util.ResourceEntry;

public class NativeImageView extends ImageView {

    public static final String TAG = NativeImageView.class.getSimpleName();

    public NativeImageView(Context context) {
        super(context);
    }

    public NativeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NativeImageView(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);
    }

    String mImageUrl;

    public void setImage(String url, int width, int height) {
        if (TextUtils.isEmpty(url)) {
            Log.e(TAG, "load: url is empty!");
            return;
        }
        mImageUrl = url;
        ImageLoader.getInstance(getContext()).load(new ResourceEntry(ResourceEntry.CUSTOM_IMAGE_CACHE_TYPE, url), width, height, new ImageLoader.ImageLoaderListener() {
            @Override
            public void onSuccess(String url, Bitmap bitmap) {
                if (TextUtils.equals(mImageUrl, url)) {
                    NativeImageView.this.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onFail(String url, String error) {
                Log.e(TAG, "load: image load fail:" + error);
            }
        });
    }

    public void setImage(String url) {
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            this.setImage(url, layoutParams.width, layoutParams.height);
        } else {
            this.setImage(url, width, height);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        try {
            super.dispatchDraw(canvas);
        } catch (Throwable e) {

        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        } catch (Throwable e) {

        }
    }
}
