package com.tradplus.ads.sigmob;

import android.location.Location;

import com.sigmob.windad.WindCustomController;

public class SigmobCustomController extends WindCustomController {

    private boolean userAgree;

    public SigmobCustomController(boolean userAgree) {
        this.userAgree = userAgree;
    }

    public boolean isCanUseLocation() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseLocation();
    }

    public Location getLocation() {
        return null;
    }

    public boolean isCanUsePhoneState() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUsePhoneState();
    }

    public String getDevImei() {
        return null;
    }

    public String getDevOaid() {
        return null;
    }

    public boolean isCanUseAndroidId() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseAndroidId();
    }

    public String getAndroidId() {
        return null;
    }



}
