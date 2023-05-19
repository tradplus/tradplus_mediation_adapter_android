package com.tradplus.ads.txadnet;

import android.util.Log;

import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.common.TPError;

import static com.tradplus.ads.base.common.TPError.CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.IMAGE_DOWNLOAD_FAILURE;
import static com.tradplus.ads.base.common.TPError.INVALID_PLACEMENTID;
import static com.tradplus.ads.base.common.TPError.LOAD_TOO_FREQUENTLY;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class TxAdnetErrorUtil {

    public static TPError getTradPlusErrorCode(AdError adError){
        TPError tradPlusErrorCode = new TPError();
        switch (adError.getErrorCode()) {
            case 2001:
                tradPlusErrorCode .setTpErrorCode(CONFIGURATION_ERROR);
                break;
            case 3001:
                tradPlusErrorCode .setTpErrorCode( CONNECTION_ERROR);
                break;
            case 4003:
                tradPlusErrorCode .setTpErrorCode( INVALID_PLACEMENTID);
                break;
            case 5002:
                tradPlusErrorCode .setTpErrorCode( IMAGE_DOWNLOAD_FAILURE);
                break;
            case 5004:
                tradPlusErrorCode .setTpErrorCode( NETWORK_NO_FILL);
                break;
            case 5013:
                tradPlusErrorCode .setTpErrorCode( LOAD_TOO_FREQUENTLY);
                break;
            case 102006:
                tradPlusErrorCode .setTpErrorCode( NETWORK_NO_FILL);
                break;
            default:
                tradPlusErrorCode .setTpErrorCode( UNSPECIFIED);
        }
        tradPlusErrorCode.setErrorCode(adError.getErrorCode()+"");
        tradPlusErrorCode.setErrorMessage(adError.getErrorMsg());
        Log.i("TradPlusLog", "ErrorCode: "+ adError.getErrorCode() + " , ErrorMsg :" + adError.getErrorMsg() );
        return tradPlusErrorCode;

    }

}
