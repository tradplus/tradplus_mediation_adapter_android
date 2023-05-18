package com.tradplus.crosspro.network.base;

import android.content.Context;
import android.text.TextUtils;

import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.crosspro.manager.CPAdManager;

public abstract class CPBaseAd implements ICPAd {
    protected Context mContext;
    protected String campaignId;
    protected String adSourceId;

    public CPBaseAd(Context context, String campaignId, String adSourceId) {
        this.mContext = context.getApplicationContext();
        this.campaignId = campaignId;
        this.adSourceId = adSourceId;

    }

    public Context getContext() {
        return mContext;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }


    protected boolean checkIsReadyParams() {
        CPAdResponse _cpAdResponse = CPAdManager.getInstance(getContext()).getCpAdConfig(campaignId);
        if (mContext == null) {
            LogUtil.ownShow("isReady() context = null!");
            return false;
        }else if (_cpAdResponse == null) {
            LogUtil.ownShow("isReady() cp no exist!");
            return false;
        } else if (TextUtils.isEmpty(_cpAdResponse.getCampaign_id())) {
            LogUtil.ownShow("isReady() mPlacementId = null!");
            return false;
        } else if (TextUtils.isEmpty(_cpAdResponse.getAd_id())) {
            LogUtil.ownShow("isReady() mOfferId = null!");
            return false;
        }
//


        return true;
    }

}
