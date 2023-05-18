package com.tradplus.ads.vungle;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class VungleInterstitialCallbackRouter {

    private static VungleInterstitialCallbackRouter instance;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();
//    private final Map<String, VungleNativeAd> bannerListeners = new HashMap<>();

    public static VungleInterstitialCallbackRouter getInstance() {
        if(instance == null){
            instance = new VungleInterstitialCallbackRouter();
        }
        return instance;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
        showListeners.remove(placementId);
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public void addListener(String id, TPLoadAdapterListener listener){
        getListeners().put(id,listener);

    }

    public TPLoadAdapterListener getListener(String id){
        return getListeners().get(id);
    }
    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showListeners;
    }

    public void addShowListener(String id, TPShowAdapterListener listener){
        getShowListeners().put(id,listener);

    }

    public TPShowAdapterListener getShowListener(String id){
        return getShowListeners().get(id);
    }

//    public VungleNativeAd getBannerListener(String id){
//        return getBannerListeners().get(id);
//    }
//
//    public void addBannerListener(String id, VungleNativeAd listener){
//        getBannerListeners().put(id,listener);
//
//    }

//    public Map<String, VungleNativeAd> getBannerListeners() {
//        return bannerListeners;
//    }

}
