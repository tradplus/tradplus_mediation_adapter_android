package com.tradplus.ads.ogury;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class OguryInterstitialCallbackRouter {

    private static OguryInterstitialCallbackRouter instance;
    private OguryPidReward mOguryPidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();
    private final Map<String, OguryPidReward> pidlisteners = new HashMap<>();

    public static OguryInterstitialCallbackRouter getInstance() {
        if (instance == null) {
            instance = new OguryInterstitialCallbackRouter();
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

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showListeners;
    }

    public Map<String, OguryPidReward> getPidlisteners() {
        return pidlisteners;
    }

    public void addListener(String id, TPLoadAdapterListener listener) {
        getListeners().put(id, listener);

    }

    public void addShowListener(String id, TPShowAdapterListener showAdapterListener) {
        getShowListeners().put(id, showAdapterListener);
    }

    public void addPidListener(String id, OguryPidReward mOguryPidReward) {
        getPidlisteners().put(id, mOguryPidReward);
    }

    public OguryPidReward getOguryPidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id) {
        return getShowListeners().get(id);
    }

    public TPLoadAdapterListener getListener(String id) {
        return getListeners().get(id);
    }

}