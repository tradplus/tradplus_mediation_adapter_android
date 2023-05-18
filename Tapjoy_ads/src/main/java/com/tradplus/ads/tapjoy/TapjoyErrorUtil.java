package com.tradplus.ads.tapjoy;

import com.tapjoy.TJError;
import com.tradplus.ads.base.common.TPError;

public class TapjoyErrorUtil {

    public static TPError getTradPlusErrorCode(String error, TJError tjError ){
        TPError tradPlusErrorCode = new TPError();
        tradPlusErrorCode .setTpErrorCode(error);
        tradPlusErrorCode.setErrorCode(tjError.code+"");
        tradPlusErrorCode.setErrorMessage(tjError.message);

        return tradPlusErrorCode;
    }

}
