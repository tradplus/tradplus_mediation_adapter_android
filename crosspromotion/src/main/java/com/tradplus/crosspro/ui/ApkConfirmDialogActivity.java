package com.tradplus.crosspro.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.tradplus.ads.common.util.ResourceUtils;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.ads.pushcenter.event.EventSendMessageUtil;
import com.tradplus.crosspro.manager.CPAdManager;

import static com.tradplus.ads.base.common.TPError.ACTION_NO;
import static com.tradplus.ads.base.common.TPError.ACTION_YES;

public class ApkConfirmDialogActivity extends Activity {

    public static CPAdResponse cpAd;
    public static String requestId;
    public static String adSourceId;
    public static String url;
    private AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View dialogView = View.inflate(this, ResourceUtils.getLayoutIdByName(this,"cp_alert_dialog_view"), null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        alertDialog = builder.create();
        alertDialog.setView(dialogView);
        final Window window = alertDialog.getWindow();
        Display display = getWindowManager().getDefaultDisplay();
        window.setGravity(Gravity.CENTER);
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        alertDialog.show();
        if (getResources().getConfiguration().orientation== Configuration.ORIENTATION_PORTRAIT) {
            window.setLayout((int)(display.getWidth()*0.8), LinearLayout.LayoutParams.WRAP_CONTENT);
        }else {
            window.setLayout((int)(display.getWidth()*0.5), LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        final Button btnCancel = (Button) dialogView.findViewById(ResourceUtils.getViewIdByName(this,"btn_cancel"));
        final Button btnConfirm = (Button) dialogView.findViewById(ResourceUtils.getViewIdByName(this,"btn_login"));
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventSendMessageUtil.getInstance().sendDownloadApkConfirm(ApkConfirmDialogActivity.this,cpAd.getCampaign_id(),cpAd.getAd_id(),ACTION_NO,adSourceId);
                alertDialog.dismiss();
                finish();
            }
        });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventSendMessageUtil.getInstance().sendDownloadApkConfirm(ApkConfirmDialogActivity.this,cpAd.getCampaign_id(),cpAd.getAd_id(),ACTION_YES,adSourceId);
                CPAdManager.getInstance(getApplicationContext()).realStartDownloadApp(adSourceId, cpAd, url);
                finish();
            }
        });
    }

    public static void start(Context context, String requestId, CPAdResponse cpAd, String url,String adSourceId) {
        ApkConfirmDialogActivity.requestId = requestId;
        ApkConfirmDialogActivity.cpAd = cpAd;
        ApkConfirmDialogActivity.url = url;
        ApkConfirmDialogActivity.adSourceId = adSourceId;

        Intent intent = new Intent(context, ApkConfirmDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("confirm dialog", "onKeyDown: ");
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        ApkConfirmDialogActivity.requestId = null;
        ApkConfirmDialogActivity.cpAd = null;
        ApkConfirmDialogActivity.url = null;
        ApkConfirmDialogActivity.adSourceId = null;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        super.onDestroy();
    }
}
