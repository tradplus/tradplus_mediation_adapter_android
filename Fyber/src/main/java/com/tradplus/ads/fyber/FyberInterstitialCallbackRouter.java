package com.tradplus.ads.fyber;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class FyberInterstitialCallbackRouter {

    private static FyberInterstitialCallbackRouter instance;
    private FyberPidReward mFyberPidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();
    private final Map<String, FyberPidReward> pidlisteners = new HashMap<>();

    public static FyberInterstitialCallbackRouter getInstance() {
        if (instance == null) {
            instance = new FyberInterstitialCallbackRouter();
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

    public Map<String, FyberPidReward> getPidlisteners() {
        return pidlisteners;
    }

    public void addListener(String id, TPLoadAdapterListener listener) {
        getListeners().put(id, listener);

    }

    public void addShowListener(String id, TPShowAdapterListener listener) {
        getShowListeners().put(id, listener);
    }

    public void addPidListener(String id, FyberPidReward mFyberPidReward) {
        getPidlisteners().put(id, mFyberPidReward);
    }

    public FyberPidReward getFyberPidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPLoadAdapterListener getListener(String id) {
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id) {
        return getShowListeners().get(id);
    }
}
