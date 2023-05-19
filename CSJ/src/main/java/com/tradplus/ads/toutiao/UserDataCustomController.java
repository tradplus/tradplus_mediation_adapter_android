package com.tradplus.ads.toutiao;

import com.bytedance.sdk.openadsdk.LocationProvider;
import com.bytedance.sdk.openadsdk.TTCustomController;

public class UserDataCustomController extends TTCustomController {

    private boolean userAgree;

    public UserDataCustomController(boolean userAgree) {
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
    public LocationProvider getTTLocation() {
        return super.getTTLocation();
    }

    @Override
    public boolean alist() {
        return super.alist();
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
    public String getMacAddress() {
        return super.getMacAddress();
    }

    @Override
    public boolean isCanUseWriteExternal() {
        if (!userAgree) {
            return false;
        }
        return super.isCanUseWriteExternal();
    }

    @Override
    public String getDevOaid() {
        return super.getDevOaid();
    }
}
