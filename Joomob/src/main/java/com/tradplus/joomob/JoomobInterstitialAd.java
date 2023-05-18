package com.tradplus.joomob;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.joomob.sdk.common.ads.AdError;
import com.joomob.sdk.common.ads.JMADManager;
import com.joomob.sdk.common.ads.JmAdSlot;
import com.joomob.sdk.common.ads.listener.JmInsertListener;
import com.joomob.sdk.common.ads.listener.JmInsertVideoListener;
import com.joomob.sdk.common.proxy.IInsertAd;
import com.joomob.sdk.common.proxy.IInsertVideoAd;
import com.joomob.sdk.common.proxy.JMAD;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interstitial.TPInterstitialAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TPContextUtils;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class JoomobInterstitialAd extends TPInterstitialAdapter {
    public static final String TAG = "JoomobInterstitialAd";
    public static final int FULLSTYPE = 1;
    private String mAppId;
    private String mSlotId;
    private JoomobInterstitialCallbackRouter mCallbackRouter;
    private JMAD mJmad;
    private IInsertAd mIInsertAd;
    private Context mCxt;
    private int fullScreenType;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Log.i(TAG, "loadInterstitial: ");
        mCxt = context;
        if (mCxt instanceof Application) {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            return;
        }
        if (serverExtras != null && serverExtras.size() > 0) {
            mAppId = serverExtras.get(AppKeyManager.APP_ID);
            mSlotId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            fullScreenType = Integer.parseInt(serverExtras.get(AppKeyManager.FULL_SCREEN_TYPE));
        }
//        mAppId = "2001920";
//        mSlotId="3001785433";
        mCallbackRouter = JoomobInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addJoomobRewardListener(mSlotId, mLoadAdapterListener);

        if (!AppKeyManager.getInstance().isInited(mAppId, AppKeyManager.AdType.SHARE)) {
            JoomobInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess: ");
                    requestInterstital(context);
                }

                @Override
                public void onFailed(String code, String msg) {

                }
            });
        } else {
            requestInterstital(context);
        }

    }

    private void requestInterstital(Context context) {
        mJmad = JMADManager.getInstance().create();
        JmAdSlot adSlot = new JmAdSlot.Builder().setSlotId(mSlotId).build();

        if (fullScreenType == FULLSTYPE) {
            mJmad.loadInsertVideo((Activity) context, adSlot, jmInsertVideoListener);
        } else {
            mJmad.loadInsert((Activity) context, adSlot, jmInsertListener);
        }
    }

    private IInsertVideoAd mInsertVideoAd;
    private JmInsertVideoListener jmInsertVideoListener = new JmInsertVideoListener() {
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
            Log.i(TAG, "onAdError: ");
            if (mCallbackRouter.getShowListener(mSlotId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(adError.getErrorCode()+"");
                tpError.setErrorMessage(adError.getErrorMsg());
                mCallbackRouter.getShowListener(mSlotId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onAdVideoStartPlay() {
            Log.i(TAG, "onAdVideoStartPlay: ");
        }

        @Override
        public void onVideoComplete() {
            Log.i(TAG, "onVideoComplete: ");
        }

        @Override
        public void onAdReceive() {
            Log.i(TAG, "onAdReceive: ");
        }

        @Override
        public void onVideoCached(IInsertVideoAd iInsertVideoAd) {
            Log.i(TAG, "onVideoCached: ");
            if (iInsertVideoAd != null) {
                mInsertVideoAd = iInsertVideoAd;
            }
            if (mCallbackRouter.getListener(mSlotId) != null) {
                mCallbackRouter.getListener(mSlotId) .loadAdapterLoaded(null);
            }
        }
    };
    JmInsertListener jmInsertListener = new JmInsertListener() {

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
            Log.i(TAG, "onAdError: ");
            if (mCallbackRouter.getShowListener(mSlotId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(adError.getErrorCode()+"");
                tpError.setErrorMessage(adError.getErrorMsg());
                mCallbackRouter.getShowListener(mSlotId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onRequestSuccess(IInsertAd iInsertAd) {
            Log.i(TAG, "onRequestSuccess: " + iInsertAd);
            if (mCallbackRouter.getListener(mSlotId) != null) {
                mCallbackRouter.getListener(mSlotId).loadAdapterLoaded(null);
            }
            if (iInsertAd != null) {
                mIInsertAd = iInsertAd;
            }
        }
    };

    @Override
    public void showAd() {
        Log.i(TAG, "showInterstitial: " + mIInsertAd + "::" + mJmad);
        if (mCallbackRouter != null && mShowListener != null) {
            mCallbackRouter.addShowListener(mSlotId, mShowListener);
        }

        Context context = mCxt;
        final Context _ct = TPContextUtils.getInstance(context).compareContext(context);
        if (_ct != null) {
            com.joomob.sdk.core.inner.a.a((Activity) _ct);
            if (fullScreenType == AppKeyManager.FULL_TYPE) {
                if (mJmad != null) {
                    mJmad.showInsertVideo((Activity) _ct);
                }
            } else {
                if (mIInsertAd != null) {
                    mIInsertAd.show();
                }
            }

        } else {
//            mShowListener = mCallbackRouter.getCEIListener(mSlotId);
            mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
        }

    }

    @Override
    public void clean() {
        super.clean();
        Log.i(TAG, "onInvalidate: ");
        if (mIInsertAd != null) {
            mIInsertAd.destroy();
        }
        if (mInsertVideoAd != null) {
            mInsertVideoAd.destroy();
        }
    }

    @Override
    public boolean isReady() {
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
