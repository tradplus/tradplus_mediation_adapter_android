package com.tradplus.ads.ironsource;


import com.tradplus.ads.base.adapter.TPLoadAdapterListener;
import com.tradplus.ads.base.adapter.TPShowAdapterListener;

import java.util.HashMap;
import java.util.Map;

public class IronSourceInterstitialCallbackRouter {

    private static IronSourceInterstitialCallbackRouter instance;
    private IronSourcePidReward mIronSourcePidReward;

    private final Map<String, TPLoadAdapterListener> listeners = new HashMap<>();
    private final Map<String, TPShowAdapterListener> showlisteners = new HashMap<>();
    private final Map<String, IronSourcePidReward> pidlisteners = new HashMap<>();

    public static IronSourceInterstitialCallbackRouter getInstance() {
        if(instance == null){
            instance = new IronSourceInterstitialCallbackRouter();
        }
        return instance;
    }

    public Map<String, TPLoadAdapterListener> getListeners() {
        return listeners;
    }

    public Map<String, TPShowAdapterListener> getShowListeners() {
        return showlisteners;
    }

    public Map<String, IronSourcePidReward> getPidlisteners() {
        return pidlisteners;
    }

    public void removeListeners(String placementId) {
        listeners.remove(placementId);
        showlisteners.remove(placementId);
    }

    public void addListener(String id, TPLoadAdapterListener listener){
        getListeners().put(id,listener);

    }

    public void addShowListener(String id, TPShowAdapterListener listener){
        getShowListeners().put(id,listener);

    }

    public void addPidListener(String id, IronSourcePidReward mIronSourcePidReward) {
        getPidlisteners().put(id, mIronSourcePidReward);
    }

    public IronSourcePidReward getIronSourcePidReward(String id) {
        return getPidlisteners().get(id);
    }

    public TPLoadAdapterListener getListener(String id){
        return getListeners().get(id);
    }

    public TPShowAdapterListener getShowListener(String id){
        return getShowListeners().get(id);
    }


}
