package com.tradplus.crosspro.network.rewardvideo;


import com.tradplus.crosspro.network.base.CPCustomEventInterstitial;
import com.tradplus.crosspro.network.base.CPError;

public interface CPRewardVideoAdListener extends CPCustomEventInterstitial.CustomEventInterstitialListener {

    void onVideoAdPlayStart();

    void onVideoAdPlayEnd();

    void onVideoShowFailed(CPError error);

    void onRewarded();

}

