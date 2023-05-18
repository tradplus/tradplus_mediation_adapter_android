package com.tradplus.crosspro.manager.resource;

import com.tradplus.ads.common.task.TPTaskManager;
import com.tradplus.ads.common.task.TPWorker;
import com.tradplus.ads.base.network.util.ResourceDownloadBaseUrlLoader;
import com.tradplus.crosspro.manager.CPResourceManager;
import com.tradplus.crosspro.network.base.CPErrorCode;

import java.io.InputStream;
import java.util.Map;

public class CPUrlLoader extends ResourceDownloadBaseUrlLoader {

    private String mPlacementId;


    public CPUrlLoader(String placementId, String url) {
        super(url);
        this.mPlacementId = placementId;
    }

    @Override
    protected Map<String, String> onPrepareHeaders() {
        return null;
    }

    @Override
    protected void onErrorAgent(String mURL, String msg) {

    }

    @Override
    protected boolean saveHttpResource(InputStream inputStream) {
        return CPResourceManager.getInstance().writeToDiskLruCache(mURL, inputStream);
    }

    @Override
    protected void startWorker(TPWorker worker) {
        TPTaskManager.getInstance().run(worker, TPTaskManager.TYPE_IMAGE_TYPE);
    }


    @Override
    protected void onLoadFinishCallback() {
//        if (mIsVideo) {
//            AgentEventManager.myOfferVideoUrlDownloadEvent(mPlacementId, mOfferId, mURL, "1"
//                    , downloadSize, null, downloadStartTime, downloadEndTime);
//        }
        CPUrlLoadManager.getInstance().notifyDownloadSuccess(mURL);
    }

    @Override
    protected void onLoadFailedCallback(String errorCode, String erroMsg) {
//        if (mIsVideo) {
//            AgentEventManager.myOfferVideoUrlDownloadEvent(mPlacementId, mOfferId, mURL, "0"
//                    , downloadSize, erroMsg, downloadStartTime, 0);
//        }
        CPUrlLoadManager.getInstance().notifyDownloadFailed(mURL, CPErrorCode.get(errorCode, erroMsg));
    }

}
