package com.tradplus.mytarget;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;



import com.my.target.ads.MyTargetView;
import com.my.target.common.MyTargetManager;
import com.my.target.common.MyTargetVersion;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MyTargetBanner extends TPBannerAdapter {
    public static final String TAG = "MyTargetBanner";
    private static final int ADSIZE_320X50 = 1;
    private static final int ADSIZE_320X250 = 2;
    private static final int ADSIZE_728X90 = 3;
    private MyTargetView myTargetView;
    private TPBannerAdImpl tpBannerAd;
    private String mSlotId;
    private String mAdsize;
    private String payload;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mSlotId = tpParams.get(AppKeyManager.AD_SLOT_ID);
            String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mAdsize = tpParams.get(AppKeyManager.ADSIZE + placementId);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        }
//        mSlotId = "854796";

        MyTargetInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestBanner(Context context) {
        myTargetView = new MyTargetView(context);
        Log.i("banner size", "MyTarget loadAdView mSlotId: " + mSlotId + ":mAdsize:" + mAdsize);
        myTargetView.setSlotId(Integer.parseInt(mSlotId));
        if (Integer.parseInt(mAdsize) == ADSIZE_320X50) {
            myTargetView.setAdSize(MyTargetView.AdSize.ADSIZE_320x50);
        } else if (Integer.parseInt(mAdsize) == ADSIZE_320X250) {
            myTargetView.setAdSize(MyTargetView.AdSize.ADSIZE_300x250);
        } else if (Integer.parseInt(mAdsize) == ADSIZE_728X90) {
            myTargetView.setAdSize(MyTargetView.AdSize.ADSIZE_728x90);
        }


        myTargetView.setListener(myTargetViewListener);
        if(TextUtils.isEmpty(payload)) {
            myTargetView.load();
        }else{
            myTargetView.loadFromBid(payload);
        }
    }

    MyTargetView.MyTargetViewListener myTargetViewListener = new MyTargetView.MyTargetViewListener() {
        @Override
        public void onLoad(MyTargetView myTargetView) {
            Log.i(TAG, "onLoad: ");
            if (mLoadAdapterListener != null && myTargetView != null) {
                tpBannerAd = new TPBannerAdImpl(null, myTargetView);
                mLoadAdapterListener.loadAdapterLoaded(tpBannerAd);
            }
        }

        @Override
        public void onNoAd(String s, MyTargetView myTargetView) {
            Log.i(TAG, "onNoAd: " + s);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(TPError.NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onShow(MyTargetView myTargetView) {
            Log.i(TAG, "onShow: ");
            //不回调
            if (tpBannerAd != null)
                tpBannerAd.adShown();
        }

        @Override
        public void onClick(MyTargetView myTargetView) {
            Log.i(TAG, "onClick: ");
            if (tpBannerAd != null) {
                tpBannerAd.adClicked();
            }
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (myTargetView != null) {
            myTargetView.setListener(null);
            myTargetView.destroy();
            myTargetView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MYTARGET);
    }

    @Override
    public String getNetworkVersion() {
        return MyTargetVersion.VERSION;
    }

    @Override
    public String getBiddingToken() {
        Context context = GlobalTradPlus.getInstance().getContext();
        return MyTargetManager.getBidderToken(context);
    }
}
