package com.tradplus.ads.toutiao;


import android.util.Log;

import com.tradplus.ads.base.common.TPError;

import static com.tradplus.ads.base.common.TPError.CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class ToutiaoErrorUtil {

    public static TPError getTradPlusErrorCode(int errorCode,String msg){
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode) {
            case 20001:
                tradPlusErrorCode .setTpErrorCode( NETWORK_NO_FILL);
                break;
            case 40016:
                tradPlusErrorCode.setTpErrorCode(CONFIGURATION_ERROR);
                break;
            case 40020:
                tradPlusErrorCode.setTpErrorCode(TPError.FREQUENCY_LIMITED);
                break;
            case 40006:
                tradPlusErrorCode.setTpErrorCode(TPError.INVALID_PLACEMENTID);
                break;
            default:
                tradPlusErrorCode.setTpErrorCode(TPError.UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode + "");
        tradPlusErrorCode.setErrorMessage(msg);
        Log.i("TouTiao","Toutiao Error , errorMsg :" + msg + " , errorCode : " +errorCode);
        return tradPlusErrorCode;
    }
}
