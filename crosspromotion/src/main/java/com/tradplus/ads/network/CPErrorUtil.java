package com.tradplus.ads.network;

import com.tradplus.ads.base.common.TPError;
import com.tradplus.crosspro.network.base.CPError;

import static com.tradplus.ads.base.common.TPError.EC_NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.EC_NO_CONFIG;
import static com.tradplus.ads.base.common.TPError.EC_NO_CONNECTION;
import static com.tradplus.ads.base.common.TPError.EC_UNSPECIFIED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.NO_CONFIG;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;
import static com.tradplus.crosspro.network.base.CPErrorCode.noADError;
import static com.tradplus.crosspro.network.base.CPErrorCode.timeOutError;

public class CPErrorUtil {

    public static TPError getTradPlusErrorCode(CPError errorCode){
        TPError tradPlusErrorCode = new TPError();
        switch (errorCode.getCode()) {
            case noADError:
                tradPlusErrorCode .setTpErrorCode( NETWORK_NO_FILL);
                break;
            case timeOutError:
                tradPlusErrorCode .setTpErrorCode(  NETWORK_TIMEOUT);
                break;
            default:
                tradPlusErrorCode .setTpErrorCode(  UNSPECIFIED);
        }

        tradPlusErrorCode.setErrorCode(errorCode.getCode()+"");
        tradPlusErrorCode.setErrorMessage(errorCode.getDesc());

        return tradPlusErrorCode;
    }

    public static TPError getErrorCode(int code,String msg){
        TPError tradPlusErrorCode =  new TPError( NO_CONFIG);
            tradPlusErrorCode.setErrorCode(TPError.parseErrorCode(code));
            tradPlusErrorCode.setErrorMessage(msg);

        return tradPlusErrorCode;
    }
}
