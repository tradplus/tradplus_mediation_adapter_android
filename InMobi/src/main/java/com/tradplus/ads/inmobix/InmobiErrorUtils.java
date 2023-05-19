package com.tradplus.ads.inmobix;

import com.inmobi.ads.InMobiAdRequestStatus;
import com.tradplus.ads.base.common.TPError;

import static com.inmobi.ads.InMobiAdRequestStatus.StatusCode.NO_FILL;
import static com.tradplus.ads.base.common.TPError.CONNECTION_ERROR;
import static com.tradplus.ads.base.common.TPError.LOAD_TOO_FREQUENTLY;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

import android.util.Log;


public class InmobiErrorUtils {

    private static final String TAG = "InMobi";

    public static TPError getTPError(InMobiAdRequestStatus errorCode){
        TPError tradPlusErrorCode = new TPError();
        if (errorCode != null) {
            InMobiAdRequestStatus.StatusCode statusCode = errorCode.getStatusCode();
            switch (statusCode) {
                case NO_FILL:
                    tradPlusErrorCode.setTpErrorCode(NETWORK_NO_FILL);
                    //"Ad request successful but no ad served."
                    break;
                case NETWORK_UNREACHABLE:
                    tradPlusErrorCode.setTpErrorCode(CONNECTION_ERROR);
                    //"The Internet is unreachable. Please check your Internet connection."
                    break;
                case EARLY_REFRESH_REQUEST:
                    tradPlusErrorCode.setTpErrorCode(LOAD_TOO_FREQUENTLY);
                    //"The Ad Request cannot be done so frequently. Please wait for some time before loading another ad."
                    break;
                case REQUEST_TIMED_OUT:
                    tradPlusErrorCode.setTpErrorCode(NETWORK_TIMEOUT);
                    //"The Ad Request timed out waiting for a response from the network. This can be caused due to a bad network connection. Please try again after a few minutes."
                    break;
                default:
                    tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
            }

            tradPlusErrorCode.setErrorCode(statusCode + "");
            tradPlusErrorCode.setErrorMessage(errorCode.getMessage());
            Log.i(TAG, "errorCode: " + statusCode + ", msg:" + errorCode.getMessage());
        }else {
            tradPlusErrorCode.setTpErrorCode(UNSPECIFIED);
        }

        return tradPlusErrorCode;
    }
}
