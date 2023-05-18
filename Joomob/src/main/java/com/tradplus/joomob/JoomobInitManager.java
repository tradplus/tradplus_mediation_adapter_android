package com.tradplus.joomob;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.strictmode.CredentialProtectedWhileLockedViolation;
import android.util.Log;

import com.joomob.sdk.common.AdManager;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;

public class JoomobInitManager extends TPInitMediation  {

    private static final String TAG = "Joomob";
    private String mAppId;
    private static JoomobInitManager sInstance;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public synchronized static JoomobInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new JoomobInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        if (tpParams != null && tpParams.size() > 0) {
            mAppId = tpParams.get(AppKeyManager.APP_ID);
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mAppId);
        AdManager.getInstance().init(context, mAppId, false);

        if(initCallback != null)
            initCallback.onSuccess();

        AppKeyManager.getInstance().addAppKey(mAppId, AppKeyManager.AdType.SHARE);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {

    }
}
