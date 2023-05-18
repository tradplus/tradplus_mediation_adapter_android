package com.tradplus.crosspro.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.common.util.ResourceUtils;
import com.tradplus.ads.base.CommonUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;

import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;


public class HalfScreenDialog extends Dialog {
    private Context context;
    private InterstitialView interstitialView;
    private View.OnClickListener confirmClickListener;
    private RelativeLayout mRoot;


    public HalfScreenDialog(Context context) {
        super(context);
        this.context = context;
    }

    public HalfScreenDialog(Context context, int themeResId) {
        super(context, themeResId);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ResourceUtils.getLayoutIdByName(context, "cp_activity_ad"));
        mRoot = findViewById(CommonUtil.getResId(context, "cp_rl_root", "id"));
        mRoot.addView(interstitialView);
        setWindowSize();
    }

    public void start(final CPAdResponse cpAdResponse, int orientation, long timeStamp, final String adSourceId, boolean isInterstitial, int screenWidth, int screenHeight, int direction) {

        interstitialView = new InterstitialView(context);
        interstitialView.setCpAdResponse(cpAdResponse);
        interstitialView.setmOrientation(orientation);
        interstitialView.setAdSourceId(adSourceId);
        interstitialView.setInterstitial(isInterstitial);
        interstitialView.setTimeStamp(timeStamp);
        interstitialView.setDirection(direction);
        interstitialView.setmScreenWidth(screenWidth);
        interstitialView.setmScreenHeight(screenHeight);
        interstitialView.setOnViewFinish(new InterstitialView.OnViewFinish() {
            @Override
            public void onFinish() {
                EventSendMessageUtil.getInstance().sendAdVideoClose(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);
                HalfScreenDialog.this.dismiss();
            }
        });
        interstitialView.initView();

    }

    private void setWindowSize() {
        DisplayMetrics dm = new DisplayMetrics();
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        int height, width;
        if (activity == null) {
            height = 1920;
            width = 1080;
        } else {
            WindowManager m = activity.getWindowManager();
            m.getDefaultDisplay().getMetrics(dm);
            height = dm.heightPixels;
            width = dm.widthPixels;
        }

        // 为获取屏幕宽、高
        WindowManager.LayoutParams p = getWindow().getAttributes(); // 获取对话框当前的參数值
        p.height = (int) (height * 0.7); //高度设置为屏幕的1.0
        p.width = (int) (width * 0.7); // 宽度设置为屏幕的0.85
        p.dimAmount = 0.5f; // 设置黑暗度
        getWindow().setAttributes(p);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
