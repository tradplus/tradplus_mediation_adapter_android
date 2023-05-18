package com.tradplus.crosspro.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tradplus.ads.common.util.DeviceUtils;
import com.tradplus.ads.common.util.ResourceUtils;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.base.network.util.ImageLoader;
import com.tradplus.ads.base.network.util.ResourceEntry;

public class EndCardBannerView extends LinearLayout {
    private Context context;
    private ImageView img_icon;
    private TextView tv_title, tv_desc,tv_choice;
    private Button btn_click;
    private BannerView.OnBannerClickListener onBannerClickListener;

    public EndCardBannerView(Context context, BannerView.OnBannerClickListener onBannerClickListener) {
        super(context);
        this.onBannerClickListener = onBannerClickListener;
        init(context);
    }

    public EndCardBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        this.context = context;
        boolean isScreenLandscape = DeviceUtils.isScreenLandscapeOrientation(context);
        inflate(context, ResourceUtils.getLayoutIdByName(context,isScreenLandscape ? "cp_layout_endcard_banner_land" :"cp_layout_endcard_banner"), this);
        img_icon = findViewById(ResourceUtils.getViewIdByName(context,"cp_img_icon"));
        tv_title = findViewById(ResourceUtils.getViewIdByName(context,"cp_tv_title"));
        tv_desc = findViewById(ResourceUtils.getViewIdByName(context,"cp_tv_desc"));
        btn_click = findViewById(ResourceUtils.getViewIdByName(context,"cp_btn_click"));
        tv_choice = findViewById(ResourceUtils.getViewIdByName(context,"cp_tv_choice"));

    }

    public boolean canShow(){
        return !TextUtils.isEmpty(tv_title.getText().toString());
    }

    public void initView(ViewGroup container, CPAdResponse cpAdResponse) {
        tv_title.setText(cpAdResponse.getTitle());
        tv_desc.setText(cpAdResponse.getDescription());
        btn_click.setText(cpAdResponse.getButton());
        btn_click.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onBannerClickListener != null) {
                    onBannerClickListener.onClick();
                }
            }
        });


        if(TradPlus.invoker().getChinaHandler() != null){
            tv_choice.setText("广告");
        }else{
            tv_choice.setText("AD");
        }
        loadBitmap(cpAdResponse);
        attachTo(container);
    }

    private void attachTo(ViewGroup container) {
        int width = LayoutParams.MATCH_PARENT;

        RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        container.addView(this, rl);
        EndCardBannerView.this.setVisibility(GONE);
    }

    private void loadBitmap(final CPAdResponse cpAdResponse) {
        try {
            ViewGroup.LayoutParams lp = img_icon.getLayoutParams();
            ImageLoader.getInstance(getContext()).load(new ResourceEntry(ResourceEntry.INTERNAL_CACHE_TYPE, cpAdResponse.getIcon()), lp.width, lp.height, new ImageLoader.ImageLoaderListener() {
                @Override
                public void onSuccess(String url, Bitmap bitmap) {
                    img_icon.setImageBitmap(bitmap);
                }

                @Override
                public void onFail(String url, String errorMsg) {

                }
            });
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface OnBannerClickListener{
        void onClick();
    }
}
