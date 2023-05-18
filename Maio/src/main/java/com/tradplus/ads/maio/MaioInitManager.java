package com.tradplus.ads.maio;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;

import java.util.Map;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsListenerInterface;

public class MaioInitManager extends TPInitMediation {

    private static final String TAG = "Maio";
    private static MaioInitManager sInstance;
    private String appId;
    private MaioInterstitialCallbackRouter mCallbackRouter;
    private boolean alwaysReward;
    private boolean hasGrantedReward = false;

    public synchronized static MaioInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new MaioInitManager();
        }
        return sInstance;
    }

    public MaioInitManager() {
        mCallbackRouter = MaioInterstitialCallbackRouter.getInstance();
    }

    public void setAlwaysReward(boolean alwaysReward) {
        this.alwaysReward = alwaysReward;
        hasGrantedReward = false;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

        if (!(context instanceof Activity)) {
            if (initCallback != null) {
                initCallback.onFailed("", "Context is not Activity");
            }
            return;
        }

        if (extrasAreValid(tpParams)) {
            appId = tpParams.get(AppKeyManager.APP_ID);
        }

        if (isInited(appId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId, initCallback)) {
            return;
        }

        //TestMode
        MaioAds.setAdTestMode(TestDeviceUtil.getInstance().isNeedTestDevice());
        //Init SDK
        MaioAds.init((Activity) context, appId, new MaioAdsListenerInterface() {
            @Override
            public void onInitialized() {
                Log.i(TAG, "onInitialized: SDK Version " + MaioAds.getSdkVersion());
            }

            @Override
            public void onChangedCanShow(String zoneEid, boolean newValue) {
                Log.i("TradPluslog", "zoneEid = " + zoneEid + " newValue = " + newValue);
                if (newValue) {
                    if (mCallbackRouter != null && mCallbackRouter.getListener(zoneEid) != null)
                        mCallbackRouter.getListener(zoneEid).loadAdapterLoaded(null);
                }
            }

            @Override
            public void onOpenAd(String zoneEid) {
                Log.i(TAG, "onOpenAd: ");
                if (mCallbackRouter != null && mCallbackRouter.getShowListener(zoneEid) != null) {
                    mCallbackRouter.getShowListener(zoneEid).onAdShown();
                    mCallbackRouter.getShowListener(zoneEid).onAdVideoStart();
                }
            }

            @Override
            public void onClosedAd(String zoneEid) {
                if (mCallbackRouter != null && mCallbackRouter.getShowListener(zoneEid) == null) {
                    return;
                }
                Log.i(TAG, "onClosedAd: ");
                if (mCallbackRouter.getMaioPidReward(zoneEid) != null && (hasGrantedReward || alwaysReward)) {
                    mCallbackRouter.getShowListener(zoneEid).onReward();
                }
                mCallbackRouter.getShowListener(zoneEid).onAdClosed();
            }

            @Override
            public void onStartedAd(String zoneEid) {
                Log.i(TAG, "onStartedAd: ");
            }

            @Override
            public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneEid) {
                Log.i(TAG, "onFinishedAd: " + "skipped:" + skipped);
                if (mCallbackRouter != null && mCallbackRouter.getShowListener(zoneEid) != null) {
                    if (skipped) {
                        alwaysReward = false;
                    } else {
                        hasGrantedReward = true;
                    }
                    mCallbackRouter.getShowListener(zoneEid).onAdVideoEnd();
                }
            }

            @Override
            public void onClickedAd(String zoneEid) {
                Log.i(TAG, "onClickedAd: ");
                if (mCallbackRouter != null && mCallbackRouter.getShowListener(zoneEid) != null)
                    mCallbackRouter.getShowListener(zoneEid).onAdVideoClicked();
            }

            @Override
            public void onFailed(FailNotificationReason failNotificationReason, String zoneEid) {
                Log.i(TAG, "onFailed: reason " + failNotificationReason.name());
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(failNotificationReason.name());
                if (mCallbackRouter != null && mCallbackRouter.getListener(zoneEid) != null)
                    mCallbackRouter.getListener(zoneEid).loadAdapterLoadFailed(tpError);
            }
        });
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return MaioAds.getSdkVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Maio";
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }
}
