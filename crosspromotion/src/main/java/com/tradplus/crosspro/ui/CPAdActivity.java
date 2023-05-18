package com.tradplus.crosspro.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.CommonUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;

import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;
import static com.tradplus.ads.base.common.TPError.IC_NOCOMPLETED;

public class CPAdActivity extends Activity {
    private static final String TAG = CPAdActivity.class.getSimpleName();

    private int mScreenWidth, mScreenHeight;
    private CPAdResponse cpAdResponse;


    private static final String EXTRA_IS_SHOW_END_CARD = "extra_is_show_end_card";
    private static final String EXTRA_CPADRESPONSE_AD = "extra_adResponse_ad";
    private static final String EXTRA_REQUEST_ID = "extra_request_id";
    private static final String EXTRA_ORIENTATION_AD = "extra_orientation";
    private static final String EXTRA_FULLSCREEN_AD = "extra_full_screen";
    private static final String EXTRA_INTERSTITIAL_AD = "extra_isinterstitial";
    private static final String EXTRA_DIRECTION = "extra_direction";
    private static final String EXTRA_ADSOURCEID_AD = "extra_adsourceid";
    private static String campaignId;
    private static String adId;
    private CPAdResponse mCPAdResponse;
    private int mOrientation;
    private int mfullScreen;
    private String adSourceId;

    private RelativeLayout mRoot;
    private InterstitialView interstitialView;


    /**
     * start activity
     */
    public static void start(Context context, CPAdResponse cpAdResponse, int orientation, long timeStamp, String adSourceId, int full_screen, boolean isInterstitial, int direction) {
        Intent intent = new Intent();
        campaignId = cpAdResponse.getCampaign_id();
        adId = cpAdResponse.getAd_id();

        intent.setClass(context, CPAdActivity.class);

        intent.putExtra(EXTRA_CPADRESPONSE_AD, cpAdResponse);
        intent.putExtra("timeStamp", timeStamp);
        intent.putExtra(EXTRA_ORIENTATION_AD, orientation);
        intent.putExtra(EXTRA_ADSOURCEID_AD, adSourceId);
        intent.putExtra(EXTRA_FULLSCREEN_AD, full_screen);
        intent.putExtra(EXTRA_INTERSTITIAL_AD, isInterstitial);
        intent.putExtra(EXTRA_DIRECTION, direction);


        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * start activity
     */
    public static void start(Context context, CPAdResponse cpAdResponse, int orientation, long timeStamp, String adSourceId) {
        start(context, cpAdResponse, orientation, timeStamp, adSourceId, 1, false, 0);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readSaveInstance(savedInstanceState);

        setContentView(getLayoutIdByAdFormat());

        init();
    }

    private void readSaveInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (interstitialView != null) {
                interstitialView.setShowEndCard(savedInstanceState.getBoolean(EXTRA_IS_SHOW_END_CARD));
            }
        }
    }

    private int getLayoutIdByAdFormat() {
        return CommonUtil.getResId(this, "cp_activity_ad", "layout");

    }


    private void init() {
        getScreenParams();
        cpAdResponse = (CPAdResponse) getIntent().getSerializableExtra(EXTRA_CPADRESPONSE_AD);

        mOrientation = getIntent().getIntExtra(EXTRA_ORIENTATION_AD, 0);

        mfullScreen = getIntent().getIntExtra(EXTRA_FULLSCREEN_AD, 0);
        adSourceId = getIntent().getStringExtra(EXTRA_ADSOURCEID_AD);
        boolean isInterstitial = getIntent().getBooleanExtra(EXTRA_INTERSTITIAL_AD, false);
        long timeStamp = getIntent().getLongExtra("timeStamp", 0);
        int direction = getIntent().getIntExtra(EXTRA_DIRECTION, 0);

        mRoot = findViewById(CommonUtil.getResId(this, "cp_rl_root", "id"));




        interstitialView = new InterstitialView(this);
        interstitialView.setCpAdResponse(cpAdResponse);
        interstitialView.setmOrientation(mOrientation);
        interstitialView.setAdSourceId(adSourceId);
        interstitialView.setInterstitial(isInterstitial);
        interstitialView.setTimeStamp(timeStamp);
        interstitialView.setMfullScreen(mfullScreen);
        interstitialView.setmScreenWidth(mScreenWidth);
        interstitialView.setDirection(direction);
        interstitialView.setmScreenHeight(mScreenHeight);
        interstitialView.setOnViewFinish(new InterstitialView.OnViewFinish() {
            @Override
            public void onFinish() {
                finish();
            }
        });
        interstitialView.initView();

        mRoot.addView(interstitialView);
    }


    private void getScreenParams() {
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        if (mScreenWidth > mScreenHeight) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogUtil.ownShow("onSaveInstanceState...");
        if (interstitialView != null) {
            if (interstitialView.isShowEndCard()) {
                LogUtil.ownShow("onSaveInstanceState... mIsShowEndCard - true");
                outState.putBoolean(EXTRA_IS_SHOW_END_CARD, true);
            }
        }
    }

    protected void onResume() {
        super.onResume();
        Log.i("CrossPro", "onResume: ");
        try {
            if (interstitialView != null) {
                if (interstitialView.getmPlayerView() != null && !interstitialView.getmPlayerView().isPlaying()) {
                    interstitialView.getmPlayerView().start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("CrossPro", "onResume: ");
        if (interstitialView != null) {
            if (interstitialView.getmPlayerView() != null) {
                interstitialView.getmPlayerView().pause();
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.i("CrossPro", "onDestroy: ");
        if (interstitialView != null) {
            if (interstitialView.getCpClickController() != null) {
                interstitialView.getCpClickController().cancelClick();
            }
            if (interstitialView.getVideoPlayFinish() == 0 && interstitialView.getVideoPlayCompletion() == 0 && !TextUtils.isEmpty(cpAdResponse.getVideo_url())) {
                EventSendMessageUtil.getInstance().sendAdVideoClose(this, campaignId, adId, IC_NOCOMPLETED, adSourceId);
            }else{
                EventSendMessageUtil.getInstance().sendAdVideoClose(this, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_SUCCESS, adSourceId);
            }

//            if(interstitialView.isInterstitial()){


//            }

        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
