package com.tradplus.ads.tapjoy;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.tapjoy.TJActionRequest;
import com.tapjoy.TJAwardCurrencyListener;
import com.tapjoy.TJEarnedCurrencyListener;
import com.tapjoy.TJError;
import com.tapjoy.TJGetCurrencyBalanceListener;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.TJSetUserIDListener;
import com.tapjoy.TJSpendCurrencyListener;
import com.tapjoy.Tapjoy;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.offerwall.TPOfferWallAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class TapJoyOfferWall extends TPOfferWallAdapter {
    private static final String TAG = "TapJoyOfferWall";
    private String appId, placementId, mSdkKey;
    private TJPlacement mPlacement;
    private Handler mHandler;
    private TapjoyInterstitialCallbackRouter tapjoyTCbR;
    private boolean onVideoStart = true;
    private boolean isReady;

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        mHandler = new Handler();

        tapjoyTCbR = TapjoyInterstitialCallbackRouter.getInstance();

        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mSdkKey = tpParams.get(AppKeyManager.SDK_KEY);
        } else {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        tapjoyTCbR = TapjoyInterstitialCallbackRouter.getInstance();
        tapjoyTCbR.addListener(placementId, mLoadAdapterListener);


        if (!Tapjoy.isConnected()) {
            TapjoyInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    requestAd();
                }

                @Override
                public void onFailed(String code, String msg) {
                    if (mLoadAdapterListener != null) {
                        TPError tpError = new TPError(INIT_FAILED);
                        tpError.setErrorCode(code);
                        tpError.setErrorMessage(msg);
                        mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                    }
                }
            });
        } else {
            requestAd();
        }
    }

    private void requestAd() {
        // Setup listener for Tapjoy currency callbacks
        // must be called after requestTapjoyConnect.
        Tapjoy.setEarnedCurrencyListener(new TJEarnedCurrencyListener() {
            @Override
            public void onEarnedCurrency(String currencyName, int amount) {
                Log.i(TAG, "all currencyName == " + currencyName + " ,amount ==" + amount);

            }
        });

        mPlacement = Tapjoy.getPlacement(placementId, placementListener);
        mPlacement.setVideoListener(placementVideoListener);
        mPlacement.requestContent();


    }

    TJPlacementListener placementListener = new TJPlacementListener() {
        @Override
        public void onRequestSuccess(final TJPlacement tjPlacement) {
            Log.i(TAG, "onRequestSuccess: ");
        }

        @Override
        public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
            Log.i(TAG, "onRequestFailure: ");
            if (tapjoyTCbR.getListener(tjPlacement.getName()) != null)
                tapjoyTCbR.getListener(tjPlacement.getName()).loadAdapterLoadFailed(TapjoyErrorUtil.getTradPlusErrorCode(NETWORK_NO_FILL, tjError));

        }

        @Override
        public void onContentReady(TJPlacement tjPlacement) {
            Log.i(TAG, "onContentReady: ");
            isReady = true;
            if (tapjoyTCbR.getListener(tjPlacement.getName()) != null)
                tapjoyTCbR.getListener(tjPlacement.getName()).loadAdapterLoaded(null);
        }

        @Override
        public void onContentShow(TJPlacement tjPlacement) {
            Log.i(TAG, "onContentShow: ");
            if (tapjoyTCbR.getShowListener(tjPlacement.getName()) != null)
                tapjoyTCbR.getShowListener(tjPlacement.getName()).onAdShown();
        }

        @Override
        public void onContentDismiss(final TJPlacement tjPlacement) {
            Log.i(TAG, "onContentDismiss: ");
            if (tapjoyTCbR.getShowListener(tjPlacement.getName()) != null)
                tapjoyTCbR.getShowListener(tjPlacement.getName()).onAdClosed();
        }

        @Override
        public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {
            Log.i(TAG, "onPurchaseRequest: ");
        }

        @Override
        public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {
            Log.i(TAG, "onRewardRequest: ");
        }

        @Override
        public void onClick(TJPlacement tjPlacement) {
            Log.i(TAG, "onClick: ");
            if (tapjoyTCbR.getShowListener(tjPlacement.getName()) != null)
                tapjoyTCbR.getShowListener(tjPlacement.getName()).onAdVideoClicked();
        }
    };

    TJPlacementVideoListener placementVideoListener = new TJPlacementVideoListener() {
        @Override
        public void onVideoComplete(final TJPlacement tjPlacement) {

        }

        @Override
        public void onVideoError(TJPlacement tjPlacement, String s) {
            Log.i(TAG, "onVideoStart: ");
            if (tapjoyTCbR.getShowListener(placementId) != null) {
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage(s);
                tapjoyTCbR.getShowListener(placementId).onAdVideoError(tpError);
            }
        }

        @Override
        public void onVideoStart(TJPlacement tjPlacement) {
            Log.i(TAG, "onVideoStart: ");
            if (onVideoStart) {
                onVideoStart = false;


                if (tapjoyTCbR.getShowListener(placementId) != null)
                    tapjoyTCbR.getShowListener(placementId).onAdVideoStart();
            }


        }
    };

    private boolean extrasAreValid(final Map<String, String> tpParams) {
        return tpParams.containsKey(AppKeyManager.APP_ID) && tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID);
    }

    @Override
    public void showAd() {
        if (tapjoyTCbR != null && mShowListener != null) {
            tapjoyTCbR.addShowListener(placementId, mShowListener);

            if (mPlacement != null) {
                mPlacement.showContent();
            }
        }

    }

    @Override
    public void getCurrencyBalance() {
        Tapjoy.getCurrencyBalance(new TJGetCurrencyBalanceListener() {
            @Override
            public void onGetCurrencyBalanceResponse(String s, int i) {
                if (mBalanceListener != null) {
                    mBalanceListener.currencyBalanceSuccess(i, s);
                }
                Log.i(TAG, "GetCurrencyBalance placementId ：" + s + " amout = " + i);
            }

            @Override
            public void onGetCurrencyBalanceResponseFailure(String s) {
                Log.i(TAG, "GetCurrencyBalance failed placementId ：" + s);
                if (mBalanceListener != null) {
                    mBalanceListener.currencyBalanceFailed(s);
                }
            }
        });
    }

    @Override
    public void spendCurrency(int count) {
        Tapjoy.spendCurrency(count, new TJSpendCurrencyListener() {
            @Override
            public void onSpendCurrencyResponse(String s, int i) {
                Log.i(TAG, "spendCurrency placementId ：" + s + " amout = " + i);
                if (mBalanceListener != null) {
                    mBalanceListener.spendCurrencySuccess(i, s);
                }
            }

            @Override
            public void onSpendCurrencyResponseFailure(String s) {
                Log.i(TAG, "spendCurrency failed placementId ：" + s);
                if (mBalanceListener != null) {
                    mBalanceListener.spendCurrencyFailed(s);
                }
            }
        });
    }

    @Override
    public void awardCurrency(int amount) {
        Tapjoy.awardCurrency(amount, new TJAwardCurrencyListener() {
            @Override
            public void onAwardCurrencyResponse(String s, int i) {
                Log.i(TAG, "AwardCurrency placementId ：" + s + " amout = " + i);
                if (mBalanceListener != null) {
                    mBalanceListener.awardCurrencySuccess(i, s);
                }
            }

            @Override
            public void onAwardCurrencyResponseFailure(String s) {
                Log.i(TAG, "AwardCurrency faied placementId ：" + s);
                if (mBalanceListener != null) {
                    mBalanceListener.awardCurrencyFailed(s);
                }
            }
        });
    }

    @Override
    public void setUserId(String userId) {
        Tapjoy.setUserID(userId, new TJSetUserIDListener() {
            @Override
            public void onSetUserIDSuccess() {
                Log.i(TAG, "onSetUserIDSuccess: ");
                if (mBalanceListener != null) {
                    mBalanceListener.setUserIdSuccess();
                }
            }

            @Override
            public void onSetUserIDFailure(String s) {
                Log.i(TAG, "onSetUserIDFailure: " +s);
                if (mBalanceListener != null) {
                    mBalanceListener.setUserIdFailed(s);
                }
            }
        });
    }

    @Override
    public boolean isReady() {
        if (mPlacement == null) {
            return false;
        } else {
            return mPlacement.isContentAvailable();
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TAPJOY);
    }

    @Override
    public String getNetworkVersion() {
        return Tapjoy.getVersion();
    }

}
