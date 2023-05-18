package com.tradplus.ads.mintegral;

import android.content.Context;
import android.util.AttributeSet;

import com.mbridge.msdk.nativex.view.MBMediaView;

public class TPMTGMediaView extends MBMediaView {
    public TPMTGMediaView(Context context) {
        super(context);
    }

    public TPMTGMediaView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
//        super.onWindowVisibilityChanged(i);
    }
}
