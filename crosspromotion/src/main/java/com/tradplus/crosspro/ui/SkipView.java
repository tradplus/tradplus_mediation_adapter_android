package com.tradplus.crosspro.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tradplus.ads.common.util.ResourceUtils;
import com.tradplus.ads.base.TradPlus;

public class SkipView extends LinearLayout {
    private Context context;
    private TextView cp_tv_skip;
    private LinearLayout cp_layout_skip;

    public SkipView(Context context) {
        super(context);
    }

    public SkipView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Context context, final PlayerView.OnPlayerListener listener) {
        this.context = context;
        inflate(context, ResourceUtils.getLayoutIdByName(context, "cp_layout_skip"), this);
        cp_tv_skip = findViewById(ResourceUtils.getViewIdByName(context, "cp_tv_skip"));
        cp_layout_skip = findViewById(ResourceUtils.getViewIdByName(context, "cp_layout_skip"));
        if (TradPlus.invoker().getChinaHandler() != null) {
            cp_tv_skip.setText("跳过");

        } else {
            cp_tv_skip.setText("Skip");
        }

        cp_layout_skip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onVideoSkip();
            }
        });

    }

    public void showView() {
        cp_layout_skip.setVisibility(VISIBLE);
    }

    public void hideView() {
        cp_layout_skip.setVisibility(GONE);
    }
}
