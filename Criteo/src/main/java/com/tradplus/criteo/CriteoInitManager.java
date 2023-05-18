package com.tradplus.criteo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.criteo.publisher.Criteo;
import com.criteo.publisher.CriteoInitException;
import com.criteo.publisher.model.AdUnit;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.List;
import java.util.Map;

public class CriteoInitManager extends TPInitMediation {

    private static CriteoInitManager sInstance;
    private static final String TAG = "Criteo";
    private String mCriteoPublisherId;

    public synchronized static CriteoInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new CriteoInitManager();
        }
        return sInstance;
    }

    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final List<AdUnit> adUnits, final InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            mCriteoPublisherId = tpParams.get(AppKeyManager.APP_ID);
        }

        if (isInited(mCriteoPublisherId + adUnits)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mCriteoPublisherId + adUnits, initCallback)) {
            return;
        }

        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + mCriteoPublisherId + ", adUnits :" + adUnits);
        try {
            new Criteo.Builder(((Application) context.getApplicationContext()), mCriteoPublisherId)
                    .debugLogsEnabled(TestDeviceUtil.getInstance().isNeedTestDevice())
                    .adUnits(adUnits)
                    .init();

        } catch (CriteoInitException e) {
            e.printStackTrace();
            sendResult(mCriteoPublisherId + adUnits, false, "", e.getLocalizedMessage());
            return;
        }
        //  You must initialize the SDK before calling Criteo.getInstance()
        suportGDPR(context, userParams);

        postDelayResult(mCriteoPublisherId + adUnits, 2000);
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {

    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
            boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
            Log.i("privacylaws", "CCPA: " + ccpa);
            Criteo.getInstance().setUsPrivacyOptOut(!ccpa);
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return Criteo.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "Criteo";
    }
}
