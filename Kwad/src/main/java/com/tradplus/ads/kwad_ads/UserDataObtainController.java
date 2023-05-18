package com.tradplus.ads.kwad_ads;

import android.location.Location;

import com.kwad.sdk.api.KsCustomController;

import java.util.List;

public class UserDataObtainController extends KsCustomController {

    private boolean userAgree;

    public UserDataObtainController(boolean userAgree) {
        this.userAgree = userAgree;
    }


    @Override
    public boolean canReadLocation() {
        if (!userAgree) {
            return false;
        }
        return super.canReadLocation();
    }

    @Override
    public boolean canUsePhoneState() {
        if (!userAgree) {
            return false;
        }
        return super.canUsePhoneState();
    }

    @Override
    public boolean canUseOaid() {
        if (!userAgree) {
            return false;
        }
        return super.canUseOaid();
    }

    @Override
    public boolean canUseMacAddress() {
        if (!userAgree) {
            return false;
        }
        return super.canUseMacAddress();
    }

    @Override
    public boolean canReadInstalledPackages() {
        if (!userAgree) {
            return false;
        }
        return super.canReadInstalledPackages();
    }

    @Override
    public boolean canUseStoragePermission() {
        if (!userAgree) {
            return false;
        }
        return super.canUseStoragePermission();
    }

    @Override
    public boolean canUseNetworkState() {
        if (!userAgree) {
            return false;
        }
        return super.canUseNetworkState();
    }

    @Override
    public Location getLocation() {
        return super.getLocation();
    }

    @Override
    public String getImei() {
        return super.getImei();
    }

    @Override
    public String[] getImeis() {
        return super.getImeis();
    }

    @Override
    public String getAndroidId() {
        return super.getAndroidId();
    }

    @Override
    public String getOaid() {
        return super.getOaid();
    }

    @Override
    public String getMacAddress() {
        return super.getMacAddress();
    }

    @Override
    public List<String> getInstalledPackages() {
        return super.getInstalledPackages();
    }
}
