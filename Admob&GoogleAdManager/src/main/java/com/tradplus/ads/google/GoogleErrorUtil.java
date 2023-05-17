package com.tradplus.ads.google;

import com.google.android.gms.ads.LoadAdError;
import com.tradplus.ads.base.common.TPError;

public class GoogleErrorUtil {

    public static TPError getTradPlusErrorCode(TPError tpError, LoadAdError errorCode){
        tpError.setErrorMessage(errorCode.getMessage());
        tpError.setErrorCode(errorCode.getCode() + "");
        return tpError;
    }
}
