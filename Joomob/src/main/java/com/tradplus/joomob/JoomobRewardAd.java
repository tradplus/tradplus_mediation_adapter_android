package com.tradplus.joomob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.joomob.sdk.common.ads.AdError;
import com.joomob.sdk.common.ads.JMADManager;
import com.joomob.sdk.common.ads.JmAdSlot;
import com.joomob.sdk.common.ads.listener.JmRewardVideoListener;
import com.joomob.sdk.common.proxy.IRewardAd;
import com.joomob.sdk.common.proxy.JMAD;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TPContextUtils;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class JoomobRewardAd extends TPRewardAdapter {
    public static final String TAG = "JoomobReward";
    private String mAppId;
    private String mSlotId;
    private String mCurrentName;
    private String mAmount;
    private JoomobInterstitialCallbackRouter mCallbackRouter;
    private JMAD mJmad;
    private Context mCxt;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Log.i(TAG, "loadInterstitial: ");
        mCxt = context;
        final Context _ct = TPContextUtils.getInstance(context).compareContext(context);
        if (_ct == null) {

            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            return;
        }
        if (serverExtras != null && serverExtras.size() > 0) {
            mAppId = serverExtras.get(AppKeyManager.APP_ID);
            mSlotId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        }
//        mAppId = "2001920";
//        mSlotId = "3001785400";
        mCallbackRouter = JoomobInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addJoomobRewardListener(mSlotId, mLoadAdapterListener);

        if (!AppKeyManager.getInstance().isInited(mAppId, AppKeyManager.AdType.SHARE)) {
            JoomobInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess: ");
                    requestInterstitialVideo(context);
                }

                @Override
                public void onFailed(String code, String msg) {

                }
            });
        } else {
            requestInterstitialVideo(context);
        }

    }

    private void requestInterstitialVideo(Context context) {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        mJmad = JMADManager.getInstance().create();
        int orientation = context.getResources().getConfiguration().orientation;
        JmAdSlot jmAdSlot = new JmAdSlot.Builder().setSlotId(mSlotId).setOrientation(orientation).build();
        mJmad.loadRewardVideo(activity, jmAdSlot, new JmRewardVideoListener() {
            @Override
            public void onRewardVideoAdLoad() {
//                Log.i(TAG, "onRewardVideoAdLoad: ");

            }

            @Override
            public void onRewardVideoCached(IRewardAd iRewardAd) {
                Log.i(TAG, "onRewardVideoCached===: " + mLoadAdapterListener);
                if (mCallbackRouter.getListener(mSlotId) != null) {
                    mCallbackRouter.getListener(mSlotId).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onVideoComplete() {
                Log.i(TAG, "onVideoComplete: ");
            }

            @Override
            public void onDisplayAd() {
                Log.i(TAG, "onDisplayAd: ");
                if (mCallbackRouter.getShowListener(mSlotId) != null) {
                    mCallbackRouter.getShowListener(mSlotId).onAdVideoStart();
                }
            }

            @Override
            public void onClickAd() {
                Log.i(TAG, "onClickAd: ");
                if (mCallbackRouter.getShowListener(mSlotId) != null) {
                    mCallbackRouter.getShowListener(mSlotId).onAdVideoClicked();
                }
            }

            @Override
            public void onCloseAd() {
                Log.i(TAG, "onCloseAd: ");
                if (mCallbackRouter.getShowListener(mSlotId) != null) {
                    mCallbackRouter.getShowListener(mSlotId).onAdVideoEnd();
                }
            }

            @Override
            public void onAdError(AdError adError) {
                Log.i(TAG, "onAdError: code : " + adError.getErrorCode() + ", msg :" + adError.getErrorMsg());
                if (mCallbackRouter.getListener(mSlotId) != null) {
                    TPError tpError = new TPError(NETWORK_NO_FILL);
                    tpError.setErrorCode(adError.getErrorCode()+"");
                    tpError.setErrorMessage(adError.getErrorMsg());
                    mCallbackRouter.getListener(mSlotId).loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void onReward() {
                Log.i(TAG, "onReward: ");
                if (mCallbackRouter.getShowListener(mSlotId) != null) {
                    mCallbackRouter.getShowListener(mSlotId).onReward();
                }
            }
        });
    }

    @Override
    public void showAd() {
        Log.i(TAG, "showInterstitial: ");
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mSlotId, mShowListener);
        }
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity != null) {
            com.joomob.sdk.core.inner.a.a(activity);
            if (mJmad != null) {
                mJmad.showRewardVideo(activity);
            }
        } else {
            mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    @Override
    public void clean() {
        super.clean();
        if (mJmad != null) {
            mJmad.destroyRewardVideo();
        }
    }

    @Override
    public boolean isReady() {
        Log.i(TAG, "isReadyInterstitial: ");
        return true;
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_JULIANG);
    }

    @Override
    public String getNetworkVersion() {
        return "1.0.9.0";
    }

}
