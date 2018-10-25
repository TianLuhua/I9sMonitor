package com.tencent.devicedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tencent.device.TXDeviceService;

import com.booyue.monitor.R;

/**
 * Created by xiaofenglou on 2016/8/18.
 */

public class FileMsgActivity extends Activity implements View.OnClickListener {

    private EditText commonFileEdit;
    private EditText channelTypeEdit;
    private EditText fileTypeEdit;
    private EditText bussinessEdit;

    private long peerTinyId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_send_layout);


        Intent intent = getIntent();
        peerTinyId = intent.getLongExtra("tinyid", 0);


        TXDeviceService.regFileTransferFilter(/*TXDeviceService.strFileMsgService*/"FileMsg");
        TXDeviceService.regFileTransferFilter(/*TXDeviceService.strImgMsgService*/"ImgMsg");
        TXDeviceService.regFileTransferFilter(/*TXDeviceService.strVideoMsgService*/"VideoMsg");
        TXDeviceService.regFileTransferFilter(/*TXDeviceService.strAudioMsgService*/"AudioMsg");

        commonFileEdit = (EditText) super.findViewById(R.id.common_file_path);
        channelTypeEdit = (EditText) super.findViewById(R.id.channel_type);
        fileTypeEdit = (EditText) super.findViewById(R.id.file_type);
        bussinessEdit = (EditText) super.findViewById(R.id.business_name);


        Button uploadFileButton = (Button) super.findViewById(R.id.uploadCommonFile);
        uploadFileButton.setOnClickListener(this);

        Button commonFileButton = (Button) super.findViewById(R.id.sendCommonFile);
        commonFileButton.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        int vId = v.getId();
        switch (vId) {
            case R.id.uploadCommonFile:
                try {
                    String commonFilePath = commonFileEdit.getText().toString();
                    if (TextUtils.isEmpty(commonFilePath)) return;
                    String channelType = channelTypeEdit.getText().toString();
                    if (TextUtils.isEmpty(channelType)) return;
                    String filetype = channelTypeEdit.getText().toString();
                    if (TextUtils.isEmpty(filetype)) return;
                    TXDeviceService.uploadFile(commonFilePath, Integer.parseInt(channelType), Integer.parseInt(filetype));
                } catch (Exception e) {
                    Toast.makeText(this, "参数错误", 100).show();
                }

                break;
            case R.id.sendCommonFile:
                try {
                    String commonFilePath = commonFileEdit.getText().toString();
                    if (TextUtils.isEmpty(commonFilePath)) return;
                    String bussinessname = bussinessEdit.getText().toString();
                    if (TextUtils.isEmpty(bussinessname)) return;
                    TXDeviceService.sendFileTo(peerTinyId, commonFilePath, null, bussinessname);
                } catch (Exception e) {
                    Toast.makeText(this, "参数错误", 100).show();
                }
                break;
        }
    }
}
