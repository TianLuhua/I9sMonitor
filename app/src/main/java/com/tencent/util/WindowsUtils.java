package com.tencent.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public class WindowsUtils {

    /**
     * 标题栏隐藏
     * 在Activity.setCurrentView()之前调用此方法
     */
    public static void setNoTitle(Context cxt) {
        if (cxt instanceof Activity) {
            Activity activity = (Activity) cxt;
            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
    }

    /**
     * 隐藏状态栏（全屏）
     * 在Activity.setCurrentView()之前调用此方法
     */
    private void setFullScreen(Context cxt) {
        // 隐藏标题
        if (cxt instanceof Activity) {
            Activity activity = (Activity) cxt;
            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
            // 定义全屏参数
            int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            // 获得窗口对象
            Window myWindow = activity.getWindow();
            // 设置 Flag 标识
            myWindow.setFlags(flag, flag);
        }

    }

    /**
     * 销毁dialog
     */
    public static void delayFinishActivity(final Activity a, int delayTime) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {

                a.finish();
            }
        }, delayTime);

    }

    /**
     * 获取屏幕尺寸
     *
     * @param ctx
     * @return
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static Point screenSize(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            display.getSize(size);
        } else {
            size.x = display.getWidth();
            size.y = display.getHeight();
        }
        return size;
    }

    /**
     * 获取频幕的宽度
     */
    public static int getDisplayWidth(Activity mActivity) {
        DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;  // 屏幕宽度（像素）=720
        return width;
    }

    /**
     * 获取频幕的高度
     */
    public static int getDisplayHeight(Activity mActivity) {
        DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int height = metric.heightPixels;  // 屏幕高度（像素）=1280
        return height;
    }

    /**
     * 获取屏幕的密度
     */
    public static float getDisplayDensity(Activity mActivity) {
        DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        float density = metric.density;  // 屏幕密度（0.75 / 1.0 / 1.5）=2.0
        return density;
    }

    /**
     * 获取屏幕的密度Dpi
     */
    public static int getDisplayDensityDpi(Activity mActivity) {
        DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int densityDpi = metric.densityDpi; // 屏幕密度DPI（120 / 160 / 240）=320
        return densityDpi;
    }

    /**
     * 编辑框点击之后才弹出软键盘
     */
    public static void setSoftInputMode(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

    }

    /**
     * 修复软键盘遮住对话框的bug
     */
    public static void softInputAdjustPan(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    /**
     * 获取状态栏的go高度
     */
    public static int getStatusHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 获取导航栏的高度
     *
     * @param activity
     * @return
     */
    public static int getNavigationBarHeight(Activity activity) {
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        //获取NavigationBar的高度
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }

    /**
     * 隐藏输入法
     * @param activity
     * @return
     */
    public static boolean hideInputMethod(Activity activity){

        InputMethodManager mInputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        return mInputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

}
