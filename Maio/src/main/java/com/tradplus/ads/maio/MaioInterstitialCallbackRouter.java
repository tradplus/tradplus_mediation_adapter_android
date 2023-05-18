package com.tradplus.ads.maio;

import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class MaioInterstitialCallbackRouter {

    private static MaioInterstitialCallbackRouter instance;
    private MaioPidReward mMaioPidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();
    private final Map<String, MaioPidReward> pidlisteners = new HashMap<>();//currency、amount

    public static MaioInterstitialCallbackRouter getInstance() {
        if (instance == null) {
            instance = new MaioInterstitialCallbackRouter();
        }
        return instance;
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showListeners;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
    }

    public void addListener(String id, TPLoadAdapterListener listener) {
        getListeners().put(id, listener);

    }

    public void addShowListener(String id, TPShowAdapterListener listener) {
        getShowListeners().put(id, listener);
    }

    public Map<String, MaioPidReward> getPidlisteners() {
        return pidlisteners;
    }

    public void addPidListener(String id, MaioPidReward maioPidReward) {
        getPidlisteners().put(id, maioPidReward);
    }

    public TPLoadAdapterListener getListener(String id) {
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id) {
        return getShowListeners().get(id);
    }

    public MaioPidReward getMaioPidReward(String id) {
        return getPidlisteners().get(id);
    }

}
