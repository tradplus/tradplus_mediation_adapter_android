package com.tradplus.crosspro.manager;

import com.tradplus.crosspro.network.base.CPError;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CPAdMessager {

    public static final String TAG = CPAdMessager.class.getSimpleName();

    private CPAdMessager() {
        mEventMap = new HashMap<>(2);
    }

    public static CPAdMessager getInstance() {
        return Holder.sInstance;
    }

    private static class Holder {
        private static final CPAdMessager sInstance = new CPAdMessager();
    }

    private Map<String, OnEventListener> mEventMap;

    public void setListener(String key, OnEventListener listener) {
        mEventMap.put(key,listener);
    }

    public OnEventListener getListener(String key) {
        return mEventMap.get(key);
    }

    public void unRegister(String key) {
        mEventMap.remove(key);
    }


    public interface OnEventListener extends Serializable {
        void onShow();
        void onVideoShowFailed(CPError error);
        void onVideoPlayStart();
        void onVideoPlayEnd();
        void onReward();
        void onClose();
        void onClick();
    }
}
