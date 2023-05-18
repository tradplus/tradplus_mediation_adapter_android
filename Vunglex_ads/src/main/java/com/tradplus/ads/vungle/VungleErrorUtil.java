package com.tradplus.ads.vungle;

import com.tradplus.ads.base.common.TPError;
import com.vungle.warren.error.VungleException;

public class VungleErrorUtil {

    public static TPError getTradPlusErrorCode(String error, Throwable throwable ){
        TPError tradPlusErrorCode = new TPError(error);
        tradPlusErrorCode.setErrorMessage(throwable.getLocalizedMessage());

        return tradPlusErrorCode;
    }
}
