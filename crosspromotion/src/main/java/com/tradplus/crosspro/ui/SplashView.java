package com.tradplus.crosspro.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.common.TaskUtils;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.common.util.ResourceUtils;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.base.network.util.ImageLoader;
import com.tradplus.ads.base.network.util.ResourceEntry;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;
import com.tradplus.ads.pushcenter.event.request.EventShowEndRequest;
import com.tradplus.crosspro.R;
import com.tradplus.crosspro.manager.CPAdManager;
import com.tradplus.crosspro.manager.CPClickController;
import com.tradplus.crosspro.network.splash.CPSplashAd;

import java.util.ArrayList;
import java.util.List;

import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;
import static com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils.EventPushStats.EV_CLICK_PUSH_FAILED;
import static com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils.EventPushStats.EV_SHOW_PUSH_FAILED;


public class SplashView extends LinearLayout {
    private int mScreenWidth, mScreenHeight;
    private ImageView img_endcard;
    private Button view_countdown, view_skip;
    private ImageView img_bg,img_tips;
    private int countdown_time;
    private int direction;
    private EndCardView.OnEndCardListener mListener;
    private Handler handler;
    private boolean isSkip;
    private CPAdResponse cpAdResponse;
    private Context context;

    private CPClickController cpClickController;
    private String adsourceId;
    private CPSplashAd.OnSplashShownListener onSplashShownListener;


    public SplashView(Context context) {
        super(context);
        init(context);
    }

    public SplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        getScreenParams(context);
    }


    public void initView(final Context context, String campaignId, int countdowntime, int is_skipable, int direction, final String adsourceId, EndCardView.OnEndCardListener listener, CPSplashAd.OnSplashShownListener onSplashShownListener) {
        this.countdown_time = countdowntime;
        this.direction = direction;
        this.mListener = listener;
        this.adsourceId = adsourceId;
        this.onSplashShownListener = onSplashShownListener;

        handler = new Handler();


        inflate(context, ResourceUtils.getLayoutIdByName(context,"cp_layout_splash"), this);

        img_endcard = findViewById(ResourceUtils.getViewIdByName(context,"cp_img_end"));
        view_countdown = findViewById(ResourceUtils.getViewIdByName(context,"cp_view_countdown"));
        img_bg = findViewById(ResourceUtils.getViewIdByName(context,"cp_img_bg"));
        img_tips = findViewById(ResourceUtils.getViewIdByName(context,"cp_img_tips"));
        view_skip = findViewById(ResourceUtils.getViewIdByName(context,"cp_view_skip"));
        view_countdown.setText(countdown_time+"");
        setClickableBackground();
        setGlobalFocusChange();


        if(is_skipable == 1){
            view_skip.setVisibility(VISIBLE);
            view_skip.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    isSkip = true;
                    if(mListener != null){
                        EventSendMessageUtil.getInstance().sendAdVideoClose(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), "", adsourceId);
                        mListener.onCloseEndCard();
                    }
                }
            });
        }

        img_endcard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SplashView.this.onClick();
            }
        });
        cpAdResponse = CPAdManager.getInstance(getContext()).getCpAdConfig(campaignId);
        EventSendMessageUtil.getInstance().sendShowAdStart(getContext(),campaignId,cpAdResponse.getAd_id(),adsourceId);
        loadBitmap(cpAdResponse);
        EventSendMessageUtil.getInstance().sendShowEndAd(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_SUCCESS, adsourceId);
    }

    private Runnable countDownRunnable = new Runnable() {
        @Override
        public void run() {
            view_countdown.setVisibility(VISIBLE);
            countdown_time--;
            view_countdown.setText(countdown_time+"");
            if(countdown_time > 0 && !isSkip){
                countDown();
            }else{
                if(mListener != null && !isSkip){
                    EventSendMessageUtil.getInstance().sendAdVideoClose(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), EC_SUCCESS, adsourceId);
                    mListener.onCloseEndCard();
                }
            }
        }
    };

    private void setClickableBackground(){
        if(TradPlus.invoker().getChinaHandler()!=null){
            view_skip.setBackgroundResource(R.drawable.cp_btn_skip_zh_pressed);
            img_tips.setBackgroundResource(R.drawable.cp_ad_cn);
        }else{
            view_skip.setBackgroundResource(R.drawable.cp_btn_skip_pressed);
            img_tips.setBackgroundResource(R.drawable.cp_ad);
        }
    }

    private void countDown(){
        TaskUtils.runOnUiThread(countDownRunnable,1000);
    }

    private void loadBitmap(final CPAdResponse cpAdResponse) {
        try {
            String url = cpAdResponse.getEnd_card().get(0).getUrl();
            ImageLoader.getInstance(getContext()).load(new ResourceEntry(ResourceEntry.INTERNAL_CACHE_TYPE, url), mScreenWidth, mScreenHeight, new ImageLoader.ImageLoaderListener() {
                @Override
                public void onSuccess(String url, Bitmap bitmap) {
                    img_endcard.setImageBitmap(bitmap);
//                    Bitmap blurBitmap = BitmapUtil.blurBitmap(getContext(), bitmap);
//                    img_bg.setImageBitmap(blurBitmap);

                    sendTrackStart(context, false);
                    EventShowEndRequest _eventShowEndRequest = new EventShowEndRequest(context, EV_SHOW_PUSH_FAILED.getValue());
                    _eventShowEndRequest.setCampaign_id(cpAdResponse.getCampaign_id());
                    _eventShowEndRequest.setAd_id(cpAdResponse.getAd_id());
                    _eventShowEndRequest.setAsu_id(adsourceId);
                    EventSendMessageUtil.getInstance().pushTrackToServer(context, replanceTrackIds(cpAdResponse.getImp_track_url_list()), _eventShowEndRequest);

                    if(onSplashShownListener != null){
                        onSplashShownListener.onShown();
                    }
                    if(countdown_time > 0){

                        countDown();

                    }
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

    private boolean isClicking;


    private void onClick() {
        LogUtil.ownShow( "click 。。。。。");
        EventSendMessageUtil.getInstance().sendClickAd(context,cpAdResponse.getCampaign_id(),cpAdResponse.getAd_id(),adsourceId);
        if (isClicking) {
            LogUtil.ownShow( "during click 。。。。。");
            return;
        }
        if (cpAdResponse == null) {
            return;
        }

        if (mListener != null) {
            mListener.onClickEndCard();

        }
        if (cpAdResponse != null && context != null) {
            sendTrackStart(context,true);
            EventShowEndRequest _eventShowEndRequest = new EventShowEndRequest(context, EV_CLICK_PUSH_FAILED.getValue());
            _eventShowEndRequest.setCampaign_id(cpAdResponse.getCampaign_id());
            _eventShowEndRequest.setAd_id(cpAdResponse.getAd_id());
            _eventShowEndRequest.setAsu_id(adsourceId);
            EventSendMessageUtil.getInstance().pushTrackToServer(context, replanceTrackIds(cpAdResponse.getClick_track_url_list()),_eventShowEndRequest);
        }

        cpClickController = new CPClickController(context, cpAdResponse, adsourceId);
        cpClickController.startClick("", new CPClickController.ClickStatusCallback() {
            @Override
            public void clickStart() {
                isClicking = true;
//                showLoading();
            }

            @Override
            public void clickEnd() {
                isClicking = false;
                TaskUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        hideLoading();
                    }
                });
            }

            @Override
            public void downloadApp(final String url) {

                TaskUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        hideLoading();
                        CPAdManager.getInstance(context).startDownloadApp(cpAdResponse.getCampaign_id(), cpAdResponse, url,adsourceId);
                    }
                });
            }
        });

    }

    private List<String> replanceTrackIds(List<String> list) {
        List<String> _list = new ArrayList<>();
        TPDataManager tpDataManager = TPDataManager.getInstance();
        if(list != null) {
            for (int i = 0; i < list.size(); i++) {
                String url = list.get(i).replace(
                        "__TP_REQ_ID__",tpDataManager.getIds(adsourceId).getRequest_id())
                        .replace("__TP_IMP_ID__",tpDataManager.getIds(adsourceId).getImpression_id())
                        .replace("__TP_CLK_ID__",tpDataManager.getIds(adsourceId).getClick_id());
                LogUtil.ownShow("cross pro url = "+url);
                _list.add(url);
            }
        }
        return _list;
    }

    private void sendTrackStart(Context context,boolean isClick){
        List<String> _list = replanceTrackIds(isClick ? cpAdResponse.getClick_track_url_list() : cpAdResponse.getImp_track_url_list());
        if(_list != null) {
            for(int i = 0; i < _list.size();i++) {
                EventSendMessageUtil.getInstance().sendThirdCheckStart(context, cpAdResponse.getCampaign_id(), cpAdResponse.getAd_id(), adsourceId, isClick, _list.get(i));
            }
        }

    }

    private boolean isShowView;
    private void setGlobalFocusChange(){
        getViewTreeObserver().addOnGlobalFocusChangeListener(new ViewTreeObserver.OnGlobalFocusChangeListener() {
            @Override
            public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                if(!isShowView) {
                    isShowView = true;


                }
            }
        });
    }


    private void getScreenParams(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

    }

}
