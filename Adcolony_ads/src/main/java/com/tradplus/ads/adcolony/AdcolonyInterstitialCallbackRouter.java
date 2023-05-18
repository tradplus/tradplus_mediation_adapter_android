package com.tradplus.ads.adcolony;


import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class AdcolonyInterstitialCallbackRouter {

    private static AdcolonyInterstitialCallbackRouter instance;
    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showListeners = new HashMap<>();

    public static AdcolonyInterstitialCallbackRouter getInstance() {
        if (instance == null) {
            instance = new AdcolonyInterstitialCallbackRouter();
        }
        return instance;
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showListeners;
    }

    public void addListener(String id, TPLoadAdapterListener listener) {
        getListeners().put(id, listener);

    }

    public void addShowListener(String id, TPShowAdapterListener listener) {
        getShowListeners().put(id, listener);
    }

    public TPShowAdapterListener getShowListener(String id) {
        return getShowListeners().get(id);
    }

    public TPLoadAdapterListener getListener(String id) {
        return getListeners().get(id);
    }

}
