package com.booyue.binding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.booyue.monitor.R;
import com.booyue.base.BaseActivity;
import com.booyue.Conf;
import com.tencent.util.LoggerUtils;
import com.tencent.util.NetWorkUtils;
import com.tencent.util.QRCodeUtils;

import java.io.File;
/**
 * 使用qq进行与火火兔绑定的引导页
 */

/**
 * Created by Administrator on 2017/5/23.
 */
public class BooyueGuideActivity extends BaseActivity {

    public static final String TAG = "BooyueGuideActivity";

    //    public static final String QRCODE_URL = "http://iot.qq.com/add?pid=1700004781&sn=0C5C20EC3E7C430b";
    //    private static String sn = "0C5C20EC3E7C430b";
    //SDK测试
    //正式版本
    //    private static String sn = "F10ECCE0F7544954";
    public static final String QRCODE_URL = "http://iot.qq.com/add?pid=" + Conf.PRODUCT_ID + "&sn=" + Conf.SERIAL_NUMBER;


    private TextView tvBack;
    private ImageView ivQRCode;
    private String filePath;

    @Override
    public void setView() {
        setContentView(R.layout.activity_guide);
    }

    @Override
    public void initView() {
        tvBack = (TextView) findViewById(R.id.tv_back);
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ivQRCode = (ImageView) findViewById(R.id.iv_zxing);

    }

    @Override
    public void initData() {
//        checkPermission();
        generateQRCode();
    }

    public void generateQRCode() {
        filePath = getFileRoot(this) + File.separator + "qr_" + System.currentTimeMillis() + ".jpg";
        LoggerUtils.d(QRCODE_URL);
        //二维码图片较大时，生成图片、保存文件的时间可能较长，因此放在新线程中
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = QRCodeUtils.createQRImage(QRCODE_URL, 160, 160,
                        BitmapFactory.decodeResource(getResources(), R.drawable.logo), filePath);
                LoggerUtils.d(TAG + "success = " + success);
                showQRCode(success);
            }
        }).start();
    }

    /**
     * 显示二维码
     *
     * @param success
     */
//    private boolean isQRcodeSuccess = false;
    public void showQRCode(final boolean success) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (success) {
//                        isQRcodeSuccess = true;
                    showTips(R.string.generate_qrcode_success);
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    ivQRCode.setImageBitmap(bitmap);
                } else {
                    if (!NetWorkUtils.isNetWorkAvailable(BooyueGuideActivity.this)) {
                        showTips(R.string.network_close);
                    } else {
                        showTips(R.string.generate_qrcode_fail);
                    }
                }
            }
        });

    }


    /**
     * 二维码缓存目录
     */
    private String getFileRoot(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File external = context.getExternalFilesDir(null);
            if (external != null) {
                return external.getAbsolutePath();
            }
        }
        return context.getFilesDir().getAbsolutePath();
    }

    private void showTips(int tips) {
        Toast.makeText(this, tips, Toast.LENGTH_SHORT).show();
    }

//    private List<String> permissionList = new ArrayList<>();//用于存放需要授权的权限
//    private static final int REQUEST_CODE_PERMISSION = 100;

//    public void checkPermission() {
//        permissionList.clear();
//        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MOUNT_FORMAT_FILESYSTEMS};
//        for (String permission : permissions) {
//            int checkSelfPermission = ContextCompat.checkSelfPermission(this, permission);
//
//
//            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
//                permissionList.add(permission);
//            }
//        }
//        if (permissionList.isEmpty()) {
//            generateQRCode();
////            mPermissionListener.permissionSuccess();
//        } else {
//            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), REQUEST_CODE_PERMISSION);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (requestCode) {
//            case REQUEST_CODE_PERMISSION:
//                for (int grantResult : grantResults) {
//                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
////                        mPermissionListener.permissionFail();
//                        Toast.makeText(BooyueGuideActivity.this, R.string.add_friend, Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                }
//                generateQRCode();
////                mPermissionListener.permissionSuccess();
//                break;
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }

}
