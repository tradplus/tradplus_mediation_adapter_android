package com.tradplus.ads.toutiao;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.VideoView;

import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.tradplus.ads.base.adapter.nativead.TPNativeStream;

public class ToutiaoStreamPlayer extends TPNativeStream {
    public static final String TAG = "ToutiaoStreamPlayer";
    private TTFeedAd ttFeedAd;

    public ToutiaoStreamPlayer(TTFeedAd ttFeedAd) {
        this.ttFeedAd = ttFeedAd;
    }


    @Override
    public Object getNetworkAdObject() {
        return ttFeedAd;
    }

    @Override
    public String getVideoUrl() {
        if(ttFeedAd == null) return "";
        return ttFeedAd.getCustomVideo().getVideoUrl();
    }

    @Override
    public void play() {
        if(ttFeedAd == null) return;
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoStart();
        }
    }

    @Override
    public void resume(long currentPosition) {
        if(ttFeedAd == null) return;
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoContinue(currentPosition);
        }
    }

    @Override
    public void stop(long currentPosition) {
        if(ttFeedAd == null) return;
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoBreak(currentPosition);
        }
    }

    @Override
    public void pause(long currentPosition) {
        if(ttFeedAd == null) return;
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoPause(currentPosition);
        }
    }

    @Override
    public void finish() {
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoFinish();
        }
    }

    @Override
    public void videoBreak(long currentPosition) {
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoBreak(currentPosition);
        }
    }

    @Override
    public void autoStart() {
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoAutoStart();
        }
    }

    @Override
    public void startError(int d1, int d2) {
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoStartError(d1,d2);
        }
    }

    @Override
    public void videoError(long d1, int d2, int d3) {
        if (ttFeedAd.getCustomVideo() != null) {
            ttFeedAd.getCustomVideo().reportVideoError(d1,d2,d3);
        }
    }

}
