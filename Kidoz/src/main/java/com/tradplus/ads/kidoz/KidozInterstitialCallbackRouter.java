package com.tradplus.ads.kidoz;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class KidozInterstitialCallbackRouter {

    private static KidozInterstitialCallbackRouter instance;
    private KidozPidReward mKidozPidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showlisteners = new HashMap<>();
    private final Map<String, KidozPidReward> pidlisteners = new HashMap<>();

    public static KidozInterstitialCallbackRouter getInstance() {
        if(instance == null){
            instance = new KidozInterstitialCallbackRouter();
        }
        return instance;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
        showlisteners.remove(placementId);
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showlisteners;
    }

    public Map<String, KidozPidReward> getPidlisteners() {
        return pidlisteners;
    }


    public void addListener(String id, TPLoadAdapterListener listener){
        getListeners().put(id,listener);
    }


    public void addShowListener(String id, TPShowAdapterListener listener){
        getShowListeners().put(id,listener);
    }


    public void addPidListener(String id, KidozPidReward mKidozPidReward) {
        getPidlisteners().put(id, mKidozPidReward);
    }

    public KidozPidReward getOguryPidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPLoadAdapterListener getListener(String id){
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id){
        return getShowListeners().get(id);
    }
}
