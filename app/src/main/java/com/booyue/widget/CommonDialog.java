package com.booyue.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.Window;

import com.booyue.monitor.R;
import com.tencent.util.LoggerUtils;

/**
 * Created by Administrator on 2018/4/26.11:31
 */

public class CommonDialog {
    public static final String TAG = "CommonDialog";

    /**
     * 显示升级提示对话框
     * @return
     */
    public static AlertDialog showAppUpgradeDialog(Context context, View view){
        LoggerUtils.d(TAG + "showAppUpgradeDialog()");
        AlertDialog loadingDialog = new AlertDialog.Builder(context).create();
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
        Window window = loadingDialog.getWindow();
        window.setBackgroundDrawableResource(R.drawable.upgrade_dialog_bg1);
//        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setLayout((int) context.getResources().getDimension(R.dimen.dimen_496),
                (int) context.getResources().getDimension(R.dimen.dimen_297));
        window.setContentView(view);
        return loadingDialog;
    }
}
