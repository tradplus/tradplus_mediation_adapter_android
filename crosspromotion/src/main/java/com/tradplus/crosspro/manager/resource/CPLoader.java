package com.tradplus.crosspro.manager.resource;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.tradplus.ads.base.event.TPPushCenter;
import com.tradplus.ads.common.util.DeviceUtils;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;
import com.tradplus.ads.pushcenter.event.request.EventLoadEndRequest;
import com.tradplus.ads.pushcenter.event.utils.EventPushMessageUtils;
import com.tradplus.ads.pushcenter.utils.RequestUtils;
import com.tradplus.crosspro.network.base.CPError;
import com.tradplus.crosspro.network.base.CPErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tradplus.ads.base.common.TPError.EC_DOWNLOADING;
import static com.tradplus.ads.base.common.TPError.EC_ISCACHE;
import static com.tradplus.ads.base.common.TPError.EC_NETWORK_TIMEOUT;
import static com.tradplus.ads.base.common.TPError.EC_NO_CONNECTION;
import static com.tradplus.ads.base.common.TPError.EC_START_LOAD_ISCACHE;
import static com.tradplus.ads.base.common.TPError.EC_SUCCESS;
import static com.tradplus.ads.base.common.TPError.EC_UNSPECIFIED;
import static com.tradplus.crosspro.network.base.CPErrorCode.timeOutError;

public class CPLoader implements CPUrlLoadManager.CPResourceLoadResult {
    private String mPlacementId;
    private int mCPTimeout;
    private String adid;
    private List<String> mUrlList;
    private CPLoaderListener mListener;
    private EventLoadEndRequest eventLoadEndRequest;
    private Context mContext;
    private String adSourceId;
    private List<EventLoadEndRequest> loadEndRequestList;

    private Handler mMainHandler;
    private AtomicBoolean mHasCallback = new AtomicBoolean(false);//Load callback flag

    public CPLoader(String placementId, int cpTimeout, String adSourceId) {
        this.mPlacementId = placementId;
        this.mCPTimeout = cpTimeout;
        this.adSourceId = adSourceId;
    }

    public interface CPLoaderListener {
        /**
         * MyOffer load success
         */
        void onSuccess();

        /**
         * MyOffer load failed
         */
        void onFailed(CPError msg);
    }

    /**
     * load MyOffer
     */
    public void load(Context context, CPAdResponse cpAdResponse, CPLoaderListener listener) {
        this.adid = cpAdResponse.getAd_id();
        mListener = listener;
        this.mContext = context;

        loadEndRequestList = new ArrayList<>();
        loadEndRequestList.clear();

        //5800
        eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_LOAD_AD_END.getValue());
        List<String> urlList = cpAdResponse.getUrlList();
        int size = 0;
//        if (urlList == null) {
//            notifyFailed(CPErrorCode.get(CPErrorCode.incompleteResourceError, CPErrorCode.fail_incomplete_resource));
//        }else{
        size = urlList.size();
//        }
        String url;
        if (size == 0) {
            notifySuccess();
            return;
        }

        mUrlList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            url = urlList.get(i);

            if (!CPResourceStatus.isExist(url)) {
                mUrlList.add(url);
            } else {
                String _url = url;
                EventSendMessageUtil.getInstance().sendDownloadAdStart(mContext, mPlacementId, adid, adSourceId, _url, !cpAdResponse.isEndCardUrl(_url) && !cpAdResponse.isIconUrl(_url));
                EventLoadEndRequest _eventLoadEndRequest = null;
                if (cpAdResponse.isEndCardUrl(_url)) {
                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_ENDCARD_END.getValue());
                } else if (cpAdResponse.isVideoUrl(_url)) {
                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_VIDEO_END.getValue());
                }else if (cpAdResponse.isIconUrl(url)) {
                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_ENDCARD_END.getValue());
                }

                _eventLoadEndRequest.setCampaign_id(mPlacementId);
                _eventLoadEndRequest.setAd_id(adid);
                _eventLoadEndRequest.setAsu_id(adSourceId);
                _eventLoadEndRequest.setUrl(_url);
                _eventLoadEndRequest.setError_code(EC_ISCACHE);
                long loadTime = RequestUtils.getInstance().countRuntime(_eventLoadEndRequest.getCreateTime());
                _eventLoadEndRequest.setLoad_time(loadTime + "");
                TPPushCenter.getInstance().saveCrossEvent(_eventLoadEndRequest);
            }
        }

        int url_size = mUrlList.size();
        if (url_size == 0) {
            LogUtil.ownShow("cp(" + adid + "), all files have already exist");
//            for (int i = 0; i < urlList.size(); i++) {
//                String _url = urlList.get(i);
//                EventSendMessageUtil.getInstance().sendDownloadAdStart(mContext, mPlacementId, adid, adSourceId, _url, !cpAdResponse.isEndCardUrl(_url));
//                EventLoadEndRequest _eventLoadEndRequest = null;
//                if (cpAdResponse.isEndCardUrl(_url)) {
//                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_ENDCARD_END.getValue());
//                } else if (cpAdResponse.isVideoUrl(_url)) {
//                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_VIDEO_END.getValue());
//                }
//
//                _eventLoadEndRequest.setCampaign_id(mPlacementId);
//                _eventLoadEndRequest.setAd_id(adid);
//                _eventLoadEndRequest.setAsu_id(adSourceId);
//                _eventLoadEndRequest.setUrl(_url);
//                _eventLoadEndRequest.setError_code(EC_ISCACHE);
//                long loadTime = RequestUtils.getInstance().countRuntime(_eventLoadEndRequest.getCreateTime());
//                _eventLoadEndRequest.setLoad_time(loadTime + "");
//                TPPushCenter.getInstance().saveCrossEvent(GlobalTradPlus.getInstance().getContext(), _eventLoadEndRequest);
//            }
            notifySuccess();
            return;
        }


        CPUrlLoadManager.getInstance().register(this);
        startLoadTimer();

        synchronized (CPLoader.this) {
            for (int i = 0; i < url_size; i++) {
                url = mUrlList.get(i);
                EventSendMessageUtil.getInstance().sendDownloadAdStart(mContext, mPlacementId, adid, adSourceId, url, !cpAdResponse.isEndCardUrl(url) && !cpAdResponse.isIconUrl(url));
                EventLoadEndRequest _eventLoadEndRequest = null;
                if (cpAdResponse.isEndCardUrl(url)) {
                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_ENDCARD_END.getValue());
                } else if (cpAdResponse.isVideoUrl(url)) {
                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_VIDEO_END.getValue());
                }else if (cpAdResponse.isIconUrl(url)) {
                    _eventLoadEndRequest = new EventLoadEndRequest(context, EventPushMessageUtils.EventPushStats.EV_DOWNLOAD_ENDCARD_END.getValue());
                }

                _eventLoadEndRequest.setCampaign_id(mPlacementId);
                _eventLoadEndRequest.setAd_id(adid);
                _eventLoadEndRequest.setAsu_id(adSourceId);
                _eventLoadEndRequest.setUrl(url);
                if (!DeviceUtils.isNetworkAvailable(context)) {
                    processLoadStartUrlStatus(_eventLoadEndRequest, EC_NO_CONNECTION);
                    continue;
                } else if (TextUtils.isEmpty(url)) {
                    processLoadStartUrlStatus(_eventLoadEndRequest, EC_UNSPECIFIED);
                    continue;
                } else if (CPResourceStatus.isLoading(url)) {
                    LogUtil.ownShow("file is loading -> " + url);
                    processLoadStartUrlStatus(_eventLoadEndRequest, EC_DOWNLOADING);
                    continue;
                } else if (CPResourceStatus.isExist(url)) {
                    LogUtil.ownShow("file exist -> " + url);
                    CPResourceStatus.setState(url, CPResourceStatus.NORMAL);
                    CPUrlLoadManager.getInstance().notifyDownloadSuccess(url);
                    processLoadStartUrlStatus(_eventLoadEndRequest, EC_START_LOAD_ISCACHE);
                    continue;
                }
                CPResourceStatus.setState(url, CPResourceStatus.LOADING);
                LogUtil.ownShow("file not exist -> " + url);
                CPUrlLoader CPUrlLoader = new CPUrlLoader(mPlacementId, url);
//                EventSendMessageUtil.getInstance().sendDownloadNetworkStart(mContext, mPlacementId, adid, adSourceId, url);
                if (_eventLoadEndRequest != null) {
                    loadEndRequestList.add(_eventLoadEndRequest);
                }
                CPUrlLoader.start();
            }
        }
    }

    private void processLoadStartUrlStatus(EventLoadEndRequest _eventLoadEndRequest, String errorCode) {
        _eventLoadEndRequest.setError_code(errorCode);
        long loadTime = RequestUtils.getInstance().countRuntime(_eventLoadEndRequest.getCreateTime());
        _eventLoadEndRequest.setLoad_time(loadTime + "");
        TPPushCenter.getInstance().saveCrossEvent( _eventLoadEndRequest);
    }

    @Override
    public void onResourceLoadSuccess(String url) {
        synchronized (CPLoader.this) {
            CPResourceStatus.setState(url, CPResourceStatus.NORMAL);
            processEndEvent(url, EC_SUCCESS);
            if (mUrlList != null) {
                mUrlList.remove(url);
                LogUtil.ownShow("mUrlList.size() = " + mUrlList.size());
                if (mUrlList.size() == 0) {
                    if (!mHasCallback.get()) {//Load success before timeout
                        notifySuccess();
                    }
                }
            }
        }
    }

    @Override
    public void onResourceLoadFailed(String url, CPError error) {
        CPResourceStatus.setState(url, CPResourceStatus.NORMAL);
        processEndEvent(url, EC_UNSPECIFIED);
        notifyFailed(error);
    }

    private void processEndEvent(String url, String errorCode) {
        int index = -1;
        for (int i = 0; i < loadEndRequestList.size(); i++) {
            if (TextUtils.equals(url, loadEndRequestList.get(i).getUrl())) {
                loadEndRequestList.get(i).setError_code(errorCode);
                long loadTime = RequestUtils.getInstance().countRuntime(loadEndRequestList.get(i).getCreateTime());
                loadEndRequestList.get(i).setLoad_time(loadTime + "");
                TPPushCenter.getInstance().saveCrossEvent( loadEndRequestList.get(i));
                index = i;
                break;
            }

        }
        if (index != -1) {
            loadEndRequestList.remove(index);
        }
    }


    private void notifySuccess() {
        mHasCallback.set(true);
        if (mListener != null) {
            LogUtil.ownShow("cp load success, adid -> " + adid);
            eventLoadEndRequest.setCampaign_id(mPlacementId);
            eventLoadEndRequest.setAd_id(adid);
            eventLoadEndRequest.setError_code(EC_SUCCESS);
            eventLoadEndRequest.setAsu_id(adSourceId);
            long loadTime = RequestUtils.getInstance().countRuntime(eventLoadEndRequest.getCreateTime());
            eventLoadEndRequest.setLoad_time(loadTime + "");
            TPPushCenter.getInstance().saveCrossEvent(eventLoadEndRequest);
            mListener.onSuccess();
        }
        this.release();
    }

    private void notifyFailed(CPError error) {
        mHasCallback.set(true);
        if (mListener != null) {
            LogUtil.ownShow("cp load failed, adid -> " + adid);
            //5800
            eventLoadEndRequest.setCampaign_id(mPlacementId);
            eventLoadEndRequest.setAd_id(adid);
            eventLoadEndRequest.setAsu_id(adSourceId);
            if (error.getCode().equals(timeOutError)) {
                eventLoadEndRequest.setError_code(EC_NETWORK_TIMEOUT);
            } else {
                eventLoadEndRequest.setError_code(EC_UNSPECIFIED);
            }
            long loadTime = RequestUtils.getInstance().countRuntime(eventLoadEndRequest.getCreateTime());
            eventLoadEndRequest.setLoad_time(loadTime + "");
            TPPushCenter.getInstance().saveCrossEvent( eventLoadEndRequest);
            mListener.onFailed(error);
        }
        this.release();
    }

    private void release() {
        CPUrlLoadManager.getInstance().unRegister(this);
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
    }

    private void startLoadTimer() {
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mHasCallback.get()) {
                        for (int i = 0; i < loadEndRequestList.size(); i++) {
                            loadEndRequestList.get(i).setError_code(EC_NETWORK_TIMEOUT);
                            long loadTime = RequestUtils.getInstance().countRuntime(loadEndRequestList.get(i).getCreateTime());
                            loadEndRequestList.get(i).setLoad_time(loadTime + "");
                            TPPushCenter.getInstance().saveCrossEvent( loadEndRequestList.get(i));
                        }
                        notifyFailed(CPErrorCode.get(CPErrorCode.timeOutError, CPErrorCode.fail_load_timeout));
                    }
                }
            }, mCPTimeout);
        }
    }
}

