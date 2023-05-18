package com.tradplus.ads.mintegral;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.out.BannerAdListener;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.banner.TPBannerAdImpl;
import com.tradplus.ads.base.adapter.banner.TPBannerAdapter;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;

import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.adapter.TPInitMediation.INIT_STATE_BIDDING;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class MIntegralBanner extends TPBannerAdapter {

    private MBBannerView mtgBannerView;
    private String mPlacementId;
    private String mUnitId;
    private String mAdSize;
    private BannerSize mBannerSize;
    private static final String TAG = "MTGCNBanner";
    private TPBannerAdImpl mBannerAd;
    private String payload;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mUnitId = tpParams.get(AppKeyManager.UNIT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            setAdHeightAndWidthByService(mPlacementId, tpParams);
            setDefaultAdSize(320, 50);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        setAdHeightAndWidthByUser(userParams);

        MintegralInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestBanner(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }
        });


    }

    private void requestBanner(Context context) {
        mtgBannerView = new MBBannerView(context);
        mBannerSize = new BannerSize(5, mAdWidth, mAdHeight);
        mtgBannerView.init(mBannerSize, mPlacementId, mUnitId);
        mtgBannerView.setRefreshTime(0);
        mtgBannerView.setAllowShowCloseBtn(true);
        mtgBannerView.setBannerAdListener(bannerAdListener);
        if (TextUtils.isEmpty(payload)) {
            mtgBannerView.load();
        } else {
            mtgBannerView.loadFromBid(payload);
        }
    }

    BannerAdListener bannerAdListener = new BannerAdListener() {
        @Override
        public void onLoadFailed(MBridgeIds mBridgeIds, String s) {
            Log.i(TAG, "onLoadFailed: errorMsg: " + s);
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onLoadSuccessed(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLoadSuccessed: ");
            if (mtgBannerView != null) {
                setBannerLayoutParams(mtgBannerView);
                mBannerAd = new TPBannerAdImpl(null, mtgBannerView);

                if (mLoadAdapterListener != null && mtgBannerView != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mBannerAd);
                }
            }
        }

        @Override
        public void onLogImpression(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLogImpression: ");
            if (mBannerAd != null) mBannerAd.adShown();
        }

        @Override
        public void onClick(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onClick: ");
            if (mBannerAd != null) {
                mBannerAd.adClicked();
            }
        }

        @Override
        public void onLeaveApp(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onLeaveApp: ");
        }

        @Override
        public void showFullScreen(MBridgeIds mBridgeIds) {
            Log.i(TAG, "showFullScreen: ");
        }

        @Override
        public void closeFullScreen(MBridgeIds mBridgeIds) {
            Log.i(TAG, "closeFullScreen: ");
        }

        @Override
        public void onCloseBanner(MBridgeIds mBridgeIds) {
            Log.i(TAG, "onCloseBanner: ");
            if (mBannerAd != null) mBannerAd.adClosed();
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "clean: ");
        if (mtgBannerView != null) {
            mtgBannerView.setBannerAdListener(null);
            mtgBannerView.release();
            mtgBannerView = null;
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MTG);
    }

    @Override
    public String getNetworkVersion() {
        return MBConfiguration.SDK_VERSION;
    }

    @Override
    public void getBiddingToken(final Context context, final Map<String, String> tpParams, final Map<String, Object> localParams, final OnS2STokenListener onS2STokenListener) {
        TPTaskManager.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                boolean initSuccess = true;
                String appKey = tpParams.get(AppKeyManager.APP_KEY);
                String appId = tpParams.get(AppKeyManager.APP_ID);

                if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appKey)) {
                    initSuccess = MintegralInitManager.isInited(appKey + appId);
                }

                final boolean finalInitSuccess = initSuccess;
                MintegralInitManager.getInstance().initSDK(context, localParams, tpParams, new TPInitMediation.InitCallback() {
                    @Override
                    public void onSuccess() {
                        String token = BidManager.getBuyerUid(context);
                        if (!finalInitSuccess) {
                            MintegralInitManager.getInstance().sendInitRequest(true, INIT_STATE_BIDDING);
                        }

                        if (onS2STokenListener != null) {
                            onS2STokenListener.onTokenResult(token, null);
                        }
                    }

                    @Override
                    public void onFailed(String code, String msg) {
                        MintegralInitManager.getInstance().sendInitRequest(false, INIT_STATE_BIDDING);
                        if (onS2STokenListener != null) {
                            onS2STokenListener.onTokenResult("", null);
                        }
                    }
                });
            }
        });

    }
}
