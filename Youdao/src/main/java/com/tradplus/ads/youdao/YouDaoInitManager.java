package com.tradplus.ads.youdao;

import android.content.Context;
import android.util.Log;

import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.youdao.sdk.common.YoudaoSDK;

import java.util.Map;

public class YouDaoInitManager extends TPInitMediation {

    private static final String TAG = "YouDao";
    private static YouDaoInitManager sInstance;

    public synchronized static YouDaoInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new YouDaoInitManager();
        }
        return sInstance;
    }


    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        String key = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_YOUDAO);
        if(isInited(key)){
            initCallback.onSuccess();
            return;
        }

        if(hasInit(key,initCallback)){
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK:");

        try {
            YoudaoSDK.init(context);
            sendResult(key,true);
        } catch (Exception var3) {
            Log.i(TAG, "YoudaoSDK initFailed: " + var3.getMessage());
            sendResult(key,false,"",var3.getMessage());
        }


    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }

    @Override
    public String getNetworkVersionCode() {
        return "4.2.16";
    }

    @Override
    public String getNetworkVersionName() {
        return "YouDao";
    }
}
