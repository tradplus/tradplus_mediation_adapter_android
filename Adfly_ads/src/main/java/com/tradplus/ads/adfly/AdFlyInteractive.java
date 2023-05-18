package com.tradplus.ads.adfly;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;

import android.content.Context;
import android.view.View;

import com.adfly.sdk.core.AdFlySdk;
import com.adfly.sdk.interactive.InteractiveAdView;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.interactive.TPInterActiveAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.util.AppKeyManager;

import java.util.Map;

public class AdFlyInteractive extends TPInterActiveAdapter {

    private int mAdWidth;
    private int mAdHeight;
    private String widgetId;
    private InteractiveAdView interactiveAdView;
    private boolean needClose = true;
    public static final String AD_NEED_CLOSE = "need_close";

    @Override
    public void loadCustomAd(Context context, Map<String, Object> userParams, Map<String, String> tpParams) {

        if (tpParams != null && tpParams.size() > 0) {
            widgetId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(DataKeys.AD_WIDTH_SIZE)) {
                mAdWidth = (int) userParams.get(DataKeys.AD_WIDTH_SIZE);
            }
            if (userParams.containsKey(DataKeys.AD_HEIGHT_SIZE)) {
                mAdHeight = (int) userParams.get(DataKeys.AD_HEIGHT_SIZE);
            }
            if (userParams.containsKey(AD_NEED_CLOSE)) {
                needClose = (boolean) userParams.get(AD_NEED_CLOSE);
            }

        }

        AdFlyInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                requestAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null)
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(INIT_FAILED));
            }
        });

    }

    public void requestAd(Context context){
        interactiveAdView = new InteractiveAdView(context);


        if(mLoadAdapterListener != null) {
            mLoadAdapterListener.loadAdapterLoaded(null);
        }
    }

    @Override
    public View getInterActiveView() {
        return interactiveAdView;
    }

    @Override
    public void showAd() {
        if(interactiveAdView != null) {
            interactiveAdView.showAd(mAdWidth, mAdHeight, needClose, widgetId);
        }
    }

    @Override
    public void clean() {

    }


    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public String getNetworkName() {
        return "AdFlySdk";
    }

    @Override
    public String getNetworkVersion() {
        return AdFlySdk.getVersion();
    }
}
