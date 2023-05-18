package com.tradplus.ads.mintegral;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBSplashHandler;
import com.mbridge.msdk.out.MBSplashLoadListener;
import com.mbridge.msdk.out.MBSplashShowListener;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
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
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class MIntegralSplashAd extends TPSplashAdapter {
    private static final String TAG = "MTGCNSplash";
    private String mPlacementId;
    private String mUnitId;
    private Integer direction;
    private MBSplashHandler mbSplashHandler;
    private String payload;
    private int is_skipable;
    boolean allowSkip = true;
    private int countdown_time;
    private int mAppIcon;
    private int mAppIconWidth = 100;
    private int mAppIconHeight = 100;
    private int isCloseDestory;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null) {
            return;
        }


        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            mUnitId = tpParams.get(AppKeyManager.UNIT_ID);
            payload = tpParams.get(DataKeys.BIDDING_PAYLOAD);
            direction = Integer.valueOf(tpParams.get(MTGConstant.KEY_DIRECTION));
            is_skipable = Integer.parseInt(tpParams.get(MTGConstant.KEY_SKIP));
            countdown_time = Integer.parseInt(tpParams.get(MTGConstant.KEY_COUNTDOWN));

            if (is_skipable == MTGConstant.NO_SKIP) {
                allowSkip = false;
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(MTGConstant.APPICON_MTG)) {
                mAppIcon = (int) userParams.get(MTGConstant.APPICON_MTG);

                if (userParams.containsKey(MTGConstant.APPICON_WIDTH)) {
                    mAppIconWidth = (int) userParams.get(MTGConstant.APPICON_WIDTH);
                }

                if (userParams.containsKey(MTGConstant.APPICON_HEIGHT)) {
                    mAppIconHeight = (int) userParams.get(MTGConstant.APPICON_HEIGHT);
                }
            }

            if (userParams.containsKey(MTGConstant.CLOSE_MTG_DESTORY)) {
                isCloseDestory = (int) userParams.get(MTGConstant.CLOSE_MTG_DESTORY);
            }
        }

        MintegralInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestSplash(context);
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

    private void requestSplash(Context context) {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (direction == 2) {
            direction = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            direction = Configuration.ORIENTATION_PORTRAIT;
        }
        if (mAppIcon != 0) {
            mbSplashHandler = new MBSplashHandler(activity, mPlacementId, mUnitId, allowSkip, countdown_time, direction, mAppIconHeight, mAppIconWidth);
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(mAppIcon);
            mbSplashHandler.setLogoView(imageView, mAppIconWidth, mAppIconHeight);
            Log.i(TAG, "appIcon,IconWidth ：" + mAppIconWidth + ", AppIconHeight :" + mAppIconHeight);
        } else {
            mbSplashHandler = new MBSplashHandler(activity, mPlacementId, mUnitId, allowSkip, countdown_time);
            mbSplashHandler.setOrientation(direction);
        }

        mbSplashHandler.setSplashLoadListener(new MBSplashLoadListener() {
            @Override
            public void onLoadSuccessed(MBridgeIds mBridgeIds, int i) {
                Log.i(TAG, "onLoadSuccessed: ");
                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(mbSplashHandler);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }
            }

            @Override
            public void onLoadFailed(MBridgeIds mBridgeIds, String s, int i) {
                Log.i(TAG, "onLoadFailed: " + s + ":code:" + i);
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                tpError.setErrorCode(String.valueOf(i));
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }
            }

            @Override
            public void isSupportZoomOut(MBridgeIds mBridgeIds, boolean b) {

            }
        });

        mbSplashHandler.setSplashShowListener(new MBSplashShowListener() {
            @Override
            public void onShowSuccessed(MBridgeIds mBridgeIds) {
                Log.i(TAG, "onShowSuccessed: ");
                if (mShowListener != null) mShowListener.onAdShown();
            }

            @Override
            public void onShowFailed(MBridgeIds mBridgeIds, String s) {
                Log.i(TAG, "onShowFailed: " + s);
                TPError tpError = new TPError(SHOW_FAILED);
                tpError.setErrorMessage(s);
                if (mShowListener != null) {
                    mShowListener.onAdVideoError(tpError);
                }
            }

            @Override
            public void onAdClicked(MBridgeIds mBridgeIds) {
                Log.i(TAG, "onAdClicked: ");
                if (mShowListener != null) {
                    mShowListener.onAdClicked();
                }
            }

            @Override
            public void onDismiss(MBridgeIds mBridgeIds, int i) {
                Log.i(TAG, "onDismiss: ");
                if (mShowListener != null) {
                    mShowListener.onAdClosed();
                }
            }

            @Override
            public void onAdTick(MBridgeIds mBridgeIds, long l) {
                Log.i(TAG, "onAdTick: ");
                if (mShowListener != null) {
                    mShowListener.onTick(l);
                }
            }

            @Override
            public void onZoomOutPlayStart(MBridgeIds mBridgeIds) {

            }

            @Override
            public void onZoomOutPlayFinish(MBridgeIds mBridgeIds) {

            }
        });

        if (TextUtils.isEmpty(payload)) {
            mbSplashHandler.preLoad();
        } else {
            mbSplashHandler.preLoadByToken(payload);
        }
        mbSplashHandler.onResume();
    }

    @Override
    public void showAd() {
        Log.i(TAG, "showAd: ");
        TPError tpError = new TPError(SHOW_FAILED);
        if (mAdContainerView == null || mbSplashHandler == null) {
            if (mShowListener != null) {
                tpError.setErrorMessage("AdContainerView == null or mbSplashHandler == null");
                mShowListener.onAdVideoError(tpError);
            }
            return;
        }

        if (TextUtils.isEmpty(payload)) {
            mbSplashHandler.show(mAdContainerView);
        }else {
            mbSplashHandler.show(mAdContainerView, payload);
        }
    }


    @Override
    public void clean() {
        if (mbSplashHandler != null) {
            mbSplashHandler.setSplashLoadListener(null);
            mbSplashHandler.setSplashShowListener(null);
            if (isCloseDestory == 0) {
                mbSplashHandler.onDestroy();
            }
            mbSplashHandler = null;
        }
    }

    @Override
    public boolean isReady() {
        if (mbSplashHandler != null) {
            Log.i(TAG, "isReady: " + (TextUtils.isEmpty(payload) ? mbSplashHandler.isReady() : mbSplashHandler.isReady(payload)));
        }
        return !isAdsTimeOut();
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
