package com.tradplus.crosspro.ui.util;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.tradplus.crosspro.manager.CPResourceManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class CPResourceUtil {

    /**
     * Get Bitmap by width and height
     */
    public static Bitmap getBitmap(String url, int width, int height) {
        if (TextUtils.isEmpty(url) || width <= 0 || height <= 0) {
            return null;
        }
        Bitmap result = null;
        FileInputStream fis = CPResourceManager.getInstance().getInputStream(url);
        if (fis != null) {
            try {
                FileDescriptor fd = fis.getFD();
                if (fd != null) {
                    result = CPImageUtil.getBitmap(fd, width, height);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
