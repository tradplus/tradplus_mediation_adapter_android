package com.tradplus.appnext;


import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class AppNextInterstitialCallbackRouter {

    private static AppNextInterstitialCallbackRouter instance;
    private AppNextPidReward mPidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();
    private final Map<String, AppNextPidReward> pidlisteners = new HashMap<>();

    private AppNextInterstitialCallbackRouter() {
    }

    public static AppNextInterstitialCallbackRouter getInstance() {
        if (instance == null) {
            instance = new AppNextInterstitialCallbackRouter();
        }
        return instance;
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showListeners;
    }

    public Map<String, AppNextPidReward> getPidlisteners() {
        return pidlisteners;
    }

    public void addListener(String id, TPLoadAdapterListener listener) {
        getListeners().put(id, listener);

    }

    public void addPidListener(String id, AppNextPidReward pidReward) {
        getPidlisteners().put(id, pidReward);
    }

    public void addShowListener(String id, TPShowAdapterListener listener) {
        getShowListeners().put(id, listener);
    }

    public AppNextPidReward getAppNextPidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPLoadAdapterListener getListener(String id) {
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id) {
        return getShowListeners().get(id);
    }
}
