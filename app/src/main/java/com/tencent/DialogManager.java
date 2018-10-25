package com.tencent;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.booyue.monitor.R;

/**
 * Created by Administrator on 2017/8/25.13:55
 */

public class DialogManager {

    public static void createAlertDialog(Context context, int x,int y,String text, final View.OnClickListener positiveListener) {
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.show();
        Window window = dialog.getWindow();
        window.setWindowAnimations(R.style.dialogWindowAnim);

        window.setLayout((int) context.getResources().getDimension(R.dimen.dimen_336),

                (int) context.getResources().getDimension(R.dimen.dimen_240));

        window.setBackgroundDrawableResource(R.drawable.tanchuangbeijing);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_prompt, null);

        TextView tvDesc = (TextView) view.findViewById(R.id.tv_desc);
        if (!TextUtils.isEmpty(text)) {
            tvDesc.setText(text);
        }
        Button btnPositive = (Button) view.findViewById(R.id.btn_positive);
        Button btnNegative = (Button) view.findViewById(R.id.btn_negative);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (positiveListener != null) {
                    positiveListener.onClick(v);
                    dialog.dismiss();
                }
            }
        });
        btnNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.x = x;
        attributes.y = y;
        window.setAttributes(attributes);
        window.setContentView(view);
    }
}
