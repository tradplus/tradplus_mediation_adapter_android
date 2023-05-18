package com.tradplus.mytarget;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.my.target.ads.InterstitialAd;
import com.my.target.common.MyTargetManager;
import com.my.target.common.MyTargetVersion;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class MyTargetInterstitial extends TPInterstitialAdapter {
    public static final String TAG = "MyTargetInterstitial";
    private InterstitialAd mInterstitialAd;
    private String mSlotId;
    private MyTargetInterstitialCallbackRouter mMyTatgetICbR;
    private String payload;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mSlotId = tpParams.get(AppKeyManager.AD_SLOT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
        }
//        mSlotId = "854793";

        mMyTatgetICbR = MyTargetInterstitialCallbackRouter.getInstance();
        mMyTatgetICbR.addListener(mSlotId, mLoadAdapterListener);

        MyTargetInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestInterstitial(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void requestInterstitial(Context context) {
        mInterstitialAd = new InterstitialAd(Integer.parseInt(mSlotId), context);
        mInterstitialAd.setListener(interstitialAdListener);

        if (TextUtils.isEmpty(payload)) {
            mInterstitialAd.load();
        } else {
            mInterstitialAd.loadFromBid(payload);
        }
    }

    InterstitialAd.InterstitialAdListener interstitialAdListener = new InterstitialAd.InterstitialAdListener() {
        @Override
        public void onLoad(InterstitialAd interstitialAd) {
            Log.i(TAG, "onLoad: ");
            if (mMyTatgetICbR.getListener(mSlotId) != null) {
                setNetworkObjectAd(mInterstitialAd);
                mMyTatgetICbR.getListener(mSlotId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onNoAd(String s, InterstitialAd interstitialAd) {
            Log.i(TAG, "onNoAd: ");
            if (mMyTatgetICbR.getListener(mSlotId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mMyTatgetICbR.getListener(mSlotId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onClick(InterstitialAd interstitialAd) {
            Log.i(TAG, "onClick: ");
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoClicked();
            }
        }

        @Override
        public void onDismiss(InterstitialAd interstitialAd) {
            Log.i(TAG, "onDismiss: ");
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdClosed();
            }
        }

        @Override
        public void onVideoCompleted(InterstitialAd interstitialAd) {
            Log.i(TAG, "onVideoCompleted: ");
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoEnd();
            }
        }

        @Override
        public void onDisplay(InterstitialAd interstitialAd) {
            Log.i(TAG, "onDisplay: ");
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoStart();
                mMyTatgetICbR.getShowListener(mSlotId).onAdShown();
            }
        }
    };

    @Override
    public void showAd() {
        Log.i(TAG, "showAd: ");
        if (mShowListener != null)
            mMyTatgetICbR.addShowListener(mSlotId, mShowListener);


        if (mInterstitialAd != null) {
            mInterstitialAd.show();
        } else {
            if (mMyTatgetICbR.getShowListener(mSlotId) != null) {
                mMyTatgetICbR.getShowListener(mSlotId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public void clean() {
        super.clean();

        if (mSlotId != null) {
            mMyTatgetICbR.removeListeners(mSlotId);
        }

        if (mInterstitialAd != null) {
            mInterstitialAd.setListener(null);
            mInterstitialAd.destroy();
            mInterstitialAd = null;
        }
    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
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
