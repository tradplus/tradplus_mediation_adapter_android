package com.tradplus.crosspro.network.interstitial;


import com.tradplus.crosspro.network.base.CPCustomEventInterstitial;
import com.tradplus.crosspro.network.base.CPError;

public interface CPInterstitialAdListener extends CPCustomEventInterstitial.CustomEventInterstitialListener {

    void onVideoAdPlayStart();

    void onVideoAdPlayEnd();

    void onVideoShowFailed(CPError error);

    void onRewarded();

}

