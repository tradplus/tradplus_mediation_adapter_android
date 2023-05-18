package com.tradplus.ads.klevin;

import android.location.Location;

import com.tencent.klevin.KlevinCustomController;

public class UserDataObtainController extends KlevinCustomController {

    private boolean userAgree;

    public UserDataObtainController(boolean userAgree) {
        this.userAgree = userAgree;
    }

    @Override
    public boolean isCanUseLocation() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseLocation();
    }

    @Override
    public Location getLocation() {
        return super.getLocation();
    }
    @Override
    public boolean isCanUsePhoneState() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUsePhoneState();
    }


    @Override
    public String getDevImei() {
        return super.getDevImei();
    }


    @Override
    public boolean isCanUseWifiState() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseWifiState();
    }


    @Override
    public String getDevOaid() {
        return super.getDevOaid();
    }
}
