package com.tradplus.ads.maio;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import jp.maio.sdk.android.MaioAds;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

public class MaioInterstitialVideo extends TPRewardAdapter {


    private String mPlacementId;
    private MaioInterstitialCallbackRouter maioInterstitialCallbackRouter;
    private static final String TAG = "MaioInterstitialVideo";
    private boolean alwaysReward;

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras,
                             Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        if (extrasAreValid(serverExtras)) {
            mPlacementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
            alwaysReward =  (Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD)) == AppKeyManager.ENFORCE_REWARD);
        }
//        appId = "m365efed61ea596e098cf332f2229d18d";
//        mPlacementId = "zd5f64c708da06628cee107147766be3f";
//        mCurrencyName = "测试";
//        mAmount="100";

        maioInterstitialCallbackRouter = MaioInterstitialCallbackRouter.getInstance();
        maioInterstitialCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);
        maioInterstitialCallbackRouter.addPidListener(mPlacementId, new MaioPidReward("", ""));

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }
        MaioInitManager.getInstance().initSDK(activity, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    @Override
    public void showAd() {
        if (maioInterstitialCallbackRouter != null && mShowListener != null) {
            maioInterstitialCallbackRouter.addShowListener(mPlacementId, mShowListener);
        }
        if (MaioAds.canShow(mPlacementId)) {
            MaioInitManager.getInstance().setAlwaysReward(alwaysReward);
            MaioAds.show(mPlacementId);
        } else {
            if (maioInterstitialCallbackRouter.getShowListener(mPlacementId) != null) {
                maioInterstitialCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
        return MaioAds.canShow(mPlacementId);
    }

    @Override
    public void clean() {
        if (mPlacementId != null) {
            maioInterstitialCallbackRouter.removeListeners(mPlacementId);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MAIO);
    }

    @Override
    public String getNetworkVersion() {
        return MaioAds.getSdkVersion();
    }
}
