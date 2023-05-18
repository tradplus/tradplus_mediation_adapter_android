package com.tradplus.ads.helium;


import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class HeliumInterstitialCallbackRouter {

    private static HeliumInterstitialCallbackRouter instance;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();
    private final Map<String, HeliumPidReward> pidlisteners = new HashMap<>();

    public static HeliumInterstitialCallbackRouter getInstance() {
        if (instance == null) {
            instance = new HeliumInterstitialCallbackRouter();
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

    public Map<String, HeliumPidReward> getPidlisteners() {
        return pidlisteners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showListeners;
    }

    public void addListener(String id, TPLoadAdapterListener listener) {
        getListeners().put(id, listener);

    }

    public void addShowListener(String id, TPShowAdapterListener listener) {
        getShowListeners().put(id, listener);
        ;
    }

    public void addPidListener(String id, HeliumPidReward mIronSourcePidReward) {
        getPidlisteners().put(id, mIronSourcePidReward);
    }

    public HeliumPidReward getMIntergralPidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPLoadAdapterListener getListener(String id) {
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id) {
        return getShowListeners().get(id);
    }

}
