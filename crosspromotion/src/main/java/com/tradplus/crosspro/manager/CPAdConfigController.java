package com.tradplus.crosspro.manager;

import static com.tradplus.ads.base.network.BaseHttpRequest.ERROR_PARSE_RESULT;
import static com.tradplus.ads.base.util.TradPlusDataConstants.DEVICE_TYPE_MOBILE;
import static com.tradplus.ads.base.util.TradPlusDataConstants.SCREEN_LAND_TYPE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.tradplus.ads.base.common.TPDataManager;
import com.tradplus.ads.base.common.TPTaskManager;
import com.tradplus.ads.base.network.BaseHttpRequest;
import com.tradplus.ads.base.network.TPRequestManager;
import com.tradplus.ads.common.serialization.JSON;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;

import java.util.HashMap;
import java.util.Map;


public class CPAdConfigController {
    private CPAdResponse cpAdResponse;
    private static Map<String, CPAdResponse> cpAdResponseMap = new HashMap<>();

    private OnConfigListener onConfigListener;

    public CPAdConfigController() {

    }

    public void loadConfig(final Context context, final String pid, final String adSourceId, String type, int orientation) {
        loadConfig(context, pid, adSourceId, type, orientation, 0);
    }

    public void loadConfig(final Context context, final String pid, final String adSourceId,final String type, final int orientation, final int direction) {
        TPTaskManager.getInstance().runNormalTask(new Runnable() {
            @Override
            public void run() {
                loadCPAdConfig(context, pid, adSourceId, type, orientation, direction);
            }
        });
    }

    private void loadCPAdConfig(final Context context, final String pid, final String adSourceId, String type, int orientation, final int direction) {
        TPRequestManager.getInstance().requestCrossConfig(context,pid,type,orientation,new BaseHttpRequest.OnHttpLoaderListener<CPAdResponse>() {
            @Override
            public void loadSuccess(CPAdResponse response) {
                if (response != null) {

                    cpAdResponseMap.put(pid, response);
                    if (response.getError_code() == 0) {
                        if (onConfigListener != null) {
                            String endcardUrl = getEndCardWithDeviceType(response, direction);
                            if (!TextUtils.isEmpty(endcardUrl) || direction == -1) {
                                String ip = response.getIp();
                                String iso = response.getIso();
                                EventSendMessageUtil.getInstance().sendOpenAPIStart(context, ip, adSourceId, iso);
                                Log.i("CrossPro", "onSuccess: adSourceId :" + adSourceId);
                                onConfigListener.onSuccess(pid);
                            } else {
                                if (onConfigListener != null) {
                                    onConfigListener.onError(ERROR_PARSE_RESULT,"data is null");
                                }
                            }
                        }
                    } else {
                        if (onConfigListener != null) {
                            onConfigListener.onError(ERROR_PARSE_RESULT,"error code is not 0");
                        }
                    }
                } else {
                    if (onConfigListener != null) {
                        onConfigListener.onError(ERROR_PARSE_RESULT,"response is null");
                    }
                }
            }

            @Override
            public void loadError(int code, String msg) {
//                error.printStackTrace();
                if (onConfigListener != null) {
                    onConfigListener.onError(code,msg);
                }
            }

            @Override
            public void loadCanceled() {

            }
        });
//        CPConfigRequest cpConfigRequest = new CPConfigRequest(url, new CPConfigRequest.Listener() {
//            @Override
//            public void onSuccess(CPAdResponse response) {
//                if (response != null) {
//                    cpAdResponseMap.put(pid, response);
//                    if (response.getError_code() == 0) {
//                        if (onConfigListener != null) {
//                            String endcardUrl = getEndCardWithDeviceType(response, direction);
//                            if (!TextUtils.isEmpty(endcardUrl) || direction == -1) {
//                                //5100  todo 确定
//                                String ip = response.getIp();
//                                String iso = response.getIso();
//                                EventSendMessageUtil.getInstance().sendOpenAPIStart(context, ip, adSourceId, iso);
//                                Log.i("CrossPro", "onSuccess: adSourceId :" + adSourceId);
//                                onConfigListener.onSuccess(pid);
//                            } else {
//                                if (onConfigListener != null) {
//                                    onConfigListener.onError(null);
//                                }
//                            }
//                        }
//                    } else {
//                        if (onConfigListener != null) {
//                            onConfigListener.onError(null);
//                        }
//                    }
//                } else {
//                    if (onConfigListener != null) {
//                        onConfigListener.onError(null);
//                    }
//                }
//            }
//
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                error.printStackTrace();
//                if (onConfigListener != null) {
//                    onConfigListener.onError(error);
//                }
//            }
//        });
//        RequestQueue requestQueue = Networking.getRequestQueue(context);
//        if (requestQueue != null)
//            requestQueue.add(cpConfigRequest);//http open接口
    }

    private String getEndCardWithDeviceType(CPAdResponse cpAdResponse, int direction) {
        LogUtil.ownShow("getEnd_cardcpAdResponse = " + JSON.toJSONString(cpAdResponse));
        TPDataManager tpDataManager = TPDataManager.getInstance();
        String device_type = tpDataManager.getDeviceType();
        if (direction == 0) {
            if (TextUtils.equals(device_type, DEVICE_TYPE_MOBILE)) {
                String landUrl;
                if (direction == SCREEN_LAND_TYPE) {
                    landUrl = getEndCardByIndex(cpAdResponse, 1);
                    if (TextUtils.isEmpty(landUrl)) {
                        landUrl = getEndCardByIndex(cpAdResponse, 0);
                    }
                } else {

                    landUrl = getEndCardByIndex(cpAdResponse, 0);
                    if (TextUtils.isEmpty(landUrl)) {
                        landUrl = getEndCardByIndex(cpAdResponse, 1);
                    }
                }
                return landUrl;
            } else {
                String landUrl;
                if (direction == SCREEN_LAND_TYPE) {
                    landUrl = getEndCardByIndex(cpAdResponse, 3);
                    if (TextUtils.isEmpty(landUrl)) {
                        landUrl = getEndCardByIndex(cpAdResponse, 2);
                    }
                } else {
                    landUrl = getEndCardByIndex(cpAdResponse, 2);
                    if (TextUtils.isEmpty(landUrl)) {
                        landUrl = getEndCardByIndex(cpAdResponse, 3);
                    }
                }
                return landUrl;
            }
        } else {
            if (TextUtils.equals(device_type, DEVICE_TYPE_MOBILE)) {
                if (direction == SCREEN_LAND_TYPE) {
                    return getEndCardByIndex(cpAdResponse, 1);
                } else {

                    return getEndCardByIndex(cpAdResponse, 0);
                }
            } else {
                if (direction == SCREEN_LAND_TYPE) {

                    return getEndCardByIndex(cpAdResponse, 3);
                } else {
                    return getEndCardByIndex(cpAdResponse, 2);
                }
            }
        }
    }

    private String getEndCardByIndex(CPAdResponse cpAdResponse, int index) {
        for (int i = 0; i < cpAdResponse.getEnd_card().size(); i++) {
            if (cpAdResponse.getEnd_card().get(i).getType().equals((index + 1) + "")) {
                return cpAdResponse.getEnd_card().get(i).getUrl();
            }
        }
        return "";
    }


    public void setOnConfigListener(OnConfigListener onConfigListener) {
        this.onConfigListener = onConfigListener;
    }

    public interface OnConfigListener {
        void onSuccess(String pid);

        void onError(int code,String msg);
    }


    public static CPAdResponse getCpAdResponse(String pid) {
        return cpAdResponseMap.get(pid);
    }

}
