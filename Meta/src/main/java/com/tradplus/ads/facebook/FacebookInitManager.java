package com.tradplus.ads.facebook;

import static com.facebook.ads.BuildConfig.DEBUG;

import android.content.Context;

import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BuildConfig;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class FacebookInitManager extends TPInitMediation {

    private static FacebookInitManager sInstance;

    public synchronized static FacebookInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new FacebookInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        final String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_FACEBOOK);

        AdSettings.setTestMode(TestDeviceUtil.getInstance().isNeedTestDevice());

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }

        if (!AudienceNetworkAds.isInitialized(context)) {
            if (DEBUG) {
                AdSettings.turnOnSDKDebugger(context);
            }

            AudienceNetworkAds
                    .buildInitSettings(context)
                    .withInitListener(new AudienceNetworkAds.InitListener() {
                        @Override
                        public void onInitialized(AudienceNetworkAds.InitResult initResult) {
                            sendResult(customAs, true);
                        }
                    })
                    .initialize();
        } else {
            sendResult(customAs, true);
        }
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public String getNetworkVersionName() {
        return "Meta";
    }

}
