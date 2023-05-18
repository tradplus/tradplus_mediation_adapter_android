package com.tradplus.ads.fpangolin;

import android.util.Log;

import com.tradplus.ads.base.common.TPError;

import static com.tradplus.ads.base.common.TPError.CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.FREQUENCY_LIMITED;
import static com.tradplus.ads.base.common.TPError.INVALID_PLACEMENTID;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;


public class PangleErrorUtil {
    public static TPError getTradPlusErrorCode(int errorCode, String msg) {
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode) {
            case 40016:
                tradPlusErrorCode.setTpErrorCode(CONFIGURATION_ERROR);
                break;
            case 40020:
                tradPlusErrorCode.setTpErrorCode(FREQUENCY_LIMITED);
                break;
            case 40006:
                tradPlusErrorCode.setTpErrorCode(INVALID_PLACEMENTID);
                break;
            default:
                tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode + "");
        tradPlusErrorCode.setErrorMessage(msg);
        Log.d("TradPlus", "Pangle Error , errorMsg :" + msg + " , errorCode : " + errorCode);

        return tradPlusErrorCode;
    }
}
