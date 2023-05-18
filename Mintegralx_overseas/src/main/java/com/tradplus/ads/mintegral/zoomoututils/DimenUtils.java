package com.tradplus.ads.mintegral.zoomoututils;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashMap;


public class DimenUtils {
	
	
	/**
	 * According to the resolution of the mobile phone dp turn px
	 */
	public static int dip2px(Context context,float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * According to the resolution of the mobile phone px Turn dp
	 */
	public static int px2dip(Context context,float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	public static int getDisplayHeight(Context context) {

		if (context == null) {
			return 0;
		}
		try {
			DisplayMetrics dm = context.getResources().getDisplayMetrics();
			HashMap sizeMap = getSystemDisplay(context);
			return sizeMap.get("height") == null ? dm.heightPixels : (int) sizeMap.get("height");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static int getDisplayWidth(Context context) {
		if (context == null) {
			return 0;
		}
		try {
			DisplayMetrics dm = context.getResources().getDisplayMetrics();
			HashMap sizeMap = getSystemDisplay(context);
			return sizeMap.get("width") == null ? dm.widthPixels : (int) sizeMap.get("width");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static HashMap getSystemDisplay(Context context) {
		HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
		if (context == null) {
			return hashMap;
		}
		try {
			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = windowManager.getDefaultDisplay();
			DisplayMetrics displayMetrics = new DisplayMetrics();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				display.getRealMetrics(displayMetrics);
			} else {
				display.getMetrics(displayMetrics);
			}

			if (displayMetrics != null) {
				hashMap.put("height", displayMetrics.heightPixels);
				hashMap.put("width", displayMetrics.widthPixels);
			}
		} catch (Exception e) {
			Log.e("", e.getMessage(), e);
		}
		return hashMap;
	}
}
