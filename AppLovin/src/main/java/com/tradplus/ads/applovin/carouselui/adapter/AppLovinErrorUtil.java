package com.tradplus.ads.applovin.carouselui.adapter;

import com.applovin.sdk.AppLovinErrorCodes;
import com.tradplus.ads.base.common.TPError;

public class AppLovinErrorUtil {
    public static TPError getTradPlusErrorCode(int errorCode){
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode) {
            case AppLovinErrorCodes.INVALID_ZONE:
                tradPlusErrorCode.setTpErrorCode("The zone provided is invalid");
                break;
            case AppLovinErrorCodes.NO_NETWORK:
                tradPlusErrorCode.setTpErrorCode("Indicates that the device had no network connectivity at the time of an ad request, either due to airplane mode or no service.");
                break;
            case AppLovinErrorCodes.NO_FILL:
                tradPlusErrorCode.setTpErrorCode("No Fill.Indicates that no ads are currently eligible for your device.");
                break;
            default:
                tradPlusErrorCode.setTpErrorCode(null);
        }

        tradPlusErrorCode.setErrorCode(errorCode+"");

        return tradPlusErrorCode;
    }
}
