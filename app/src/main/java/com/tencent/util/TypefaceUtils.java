package com.tencent.util;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by Administrator on 2016/10/13.
 */
public class TypefaceUtils {

    private static Typeface typeFace;
    private static Typeface typeFaceU;

    public static final Typeface setTypeface(Context context) {

        if (typeFace == null) {
            typeFace = Typeface.createFromAsset(context.getAssets(), "fonts/msyh.ttf");
        }
        return typeFace;
    }

    public static final Typeface setTypefaceB(Context context) {
        if (typeFaceU == null) {
            typeFaceU = Typeface.createFromAsset(context.getAssets(), "fonts/msyhbd.ttf");
        }
        return typeFaceU;

    }
}
