package com.booyue.serial;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import com.booyue.ConfKt;
import com.booyue.MonitorApplication;
import com.booyue.monitor.R;
import com.booyue.annotation.Unique;
import com.tencent.util.FileUtil;
import com.tencent.util.LoggerUtils;
import com.tencent.util.NetUtil;
import com.tencent.util.NetWorkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2018/4/12.18:23
 */

public class SerialNumberManager {
    public static final String TAG = "SerialNumberManager--->";
    private static final int SIZE = 4096;
    public static final String CHENXIN_ID = "I6S+";//
    public static final String CHENXIN_PRO_ID = "I6S Pro";//
    public static final String T6_ID = "HengKe_T6";// Build.Model = ZF6_T6_1.0
    public static final String L1_MODEL = "rk312x"; //L1的model
    public static final String I6SC_MODEL = "I6SC"; //I6SC的model
//    private static final String CHENXIN_SN_ADDRESS = "/sys/miscinfo/infos/userdef";
//    public static final String TEMP_ID = "I6S+";//KOT49H
//    public static final String TEMP_MAC = "20:18:0E:11:68:96";//K


    /**
     * 读取串号
     *
     * @param context
     */
    public static void readSerialNumber(final Context context, final SerialNumberListener serialNumberListener) {
        LoggerUtils.d(TAG + "readSerialNumber");


        if (I6SC_MODEL.equals(Build.MODEL)) {
            spilitSerailNumber("1700005382;FA6D878A8C124fc1;3045022100BEBCC9F4C949E159937AC590790A98A033577AC6CEA6B16B6191650B9F4FD2BE0220170BD84D14BFE3D98481741D3968DCFEE0A8A19427924CE68453AE4ECAF43BCE;04C51918B8B3E2ABB44CD61BFCE7A9E6723EBFE36EA2A6F1C76992F339B26975B7C436444EBF495541CED5E4C0687D108D");
            serialNumberListener.onSerailNumberListener(1);
            LoggerUtils.d(TAG + "current mode :" + I6SC_MODEL);
            return;
        }


        if ("SM-N9100".equals(Build.MODEL)) {
            spilitSerailNumber("1700004781;083311670AA04c77;304502206F4BD9650E43D9E80753441F53983BD6A56351ADDF776E184F4DC7B268FB4FE7022100EEDA27D4A6668A431114820281C68FAA596470BED8FA8D5457D6C311BBD62C85;04D0B6C03324295914363D34E0801043CFAC3159AE89B6150057AC3E59BB2DE3B3BE43FF4C424A734892C78CA8125CF3EC");
            serialNumberListener.onSerailNumberListener(1);
            return;
        }

        if (!NetWorkUtils.isWifiActive(context)) {
            Toast.makeText(context, R.string.check_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: 2018/2/26  QRCodeUtils.CHENXIN_ID 改成Build.ID
        //由于晨芯串号重复，之后直接从服务器获取
        if (!FileUtil.isSNCached() && matchDevice()) {
            if (CHENXIN_ID.equals(Build.ID) || CHENXIN_PRO_ID.equals(Build.ID)) {
                handleRequestI6SSerialNumber(serialNumberListener);
            } else if (TextUtils.equals(T6_ID, Build.ID)) {
                handleRequestSerialNumber(SN, serialNumberListener, "T6");
                //L1的model型号，这个是不会变的
            } else if (L1_MODEL.equals(Build.MODEL)) {
                handleRequestSerialNumber(MAC, serialNumberListener, "L1");
            } else if (I6SC_MODEL.equals(Build.MODEL)) {
                handleRequestSerialNumber(MAC, serialNumberListener, "I6SC");
            }
        } else {
            getTXSNFromDiskMemory();
        }
    }

    //区分通过哪种方式获取唯一值
    public static final String MAC = "mac";
    public static final String SN = "sn";

    /**
     * I6s请求串号
     *
     * @param serialNumberListener
     */
    public static void handleRequestI6SSerialNumber(final SerialNumberListener serialNumberListener) {
        LoggerUtils.d(TAG + ":handleRequestI6SSerialNumber");
        String mac = NetUtil.getAdresseMAC(MonitorApplication.Companion.getContext());
//               String mac = "20:18:0E:13:74:C3";
        UserRequestManager.getI6SSerialNumber(mac, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LoggerUtils.d(TAG + "异常 --" + e.getMessage());
                if (serialNumberListener != null) {
                    serialNumberListener.onSerailNumberListener(0);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean successful = response.isSuccessful();
                if (successful) {
                    String content = response.body().string();
                    LoggerUtils.d(TAG + "response = : " + content);
                    processI6SResponse(content, serialNumberListener);
                }
            }
        });

    }

    /**
     * 从我们自己的内部服务器获取串号
     *
     * @param uniqueWay            唯一值获取方式  Mac地址 sn
     * @param serialNumberListener 回调监听
     * @param productModel         产品型号（L1,I6s,T6）
     */
    public static void handleRequestSerialNumber(@Unique String uniqueWay,
                                                 final SerialNumberListener serialNumberListener,
                                                 String productModel) {
        //一恒科 获取sn 手动烧写所以不会变化
        //获取为空
        String unique = "";
        if (MAC.equals(uniqueWay)) {
            unique = NetUtil.getAdresseMAC(MonitorApplication.Companion.getContext());
        } else if (SN.equals(uniqueWay)) {
            unique = getSerialNumber();
        }
        LoggerUtils.format_debug("unique %s; model %s", unique, productModel);
        UserRequestManager.getSerialNumber(unique, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LoggerUtils.d(TAG + "异常 --" + e.getMessage());
                if (serialNumberListener != null) {
                    serialNumberListener.onSerailNumberListener(0);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean successful = response.isSuccessful();
                if (successful) {
                    String content = response.body().string();
                    LoggerUtils.d(TAG + "content = " + content);
                    processResponse(content, serialNumberListener);
                }
            }
        }, productModel);
    }


    /**
     * 从本地获取腾讯串号
     */
    public static void getTXSNFromDiskMemory() {
        File file = new File(MonitorApplication.Companion.getContext().getFilesDir(), "serial.txt");
        StringBuilder stringBuilder = new StringBuilder();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            int len;
            byte[] buf = new byte[SIZE];
            //从输入流中获取串号
            while ((len = fileInputStream.read(buf)) != -1) {
                String s1 = new String(buf, 0, len);
                stringBuilder.append(s1);
            }
            LoggerUtils.d("sn = " + stringBuilder.toString());
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            LoggerUtils.e(TAG + "串号文件FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            LoggerUtils.e(TAG + "读取串号文件IOException");
            e.printStackTrace();
        }
        //分割串号
        spilitSerailNumber(stringBuilder.toString());
    }

    /**
     * 分割串号，获取对应值
     *
     * @param s 串号
     */
    public static void spilitSerailNumber(String s) {
//        s = "1700005182;E5AE67E6D4C84750;30450220473829CA2675BF32748409BA398873D17068A2406DA860860649521A3FEC6BEC022100A1879939601B8C7DEE5DB7ACBEB35084C078B4A571392E47246B633AF62C0C75;0441F02B7DD57C571E4B9ECBDD9391A94623FE2FDF6E649079BE6BA97F48D87B5C3F6FE762EFE710D3221222132A3B7944";
        LoggerUtils.d(TAG + s);
        if (TextUtils.isEmpty(s)) {
            return;
        }
        if (s.contains(";")) {
            String[] splites = s.split(";");
            if (splites.length == 4) {
                ConfKt.setPRODUCT_ID(Long.parseLong(splites[0]));
                ConfKt.setSERIAL_NUMBER(splites[1]);
                ConfKt.setLICENSE(splites[2]);
                ConfKt.setSERVER_PUBLIC_KEY(splites[3]);
            }
        }
        LoggerUtils.d(TAG + "Conf.PRODUCT_ID = " + ConfKt.getPRODUCT_ID());
        LoggerUtils.d(TAG + "Conf.SERIAL_NUMBER = " + ConfKt.getSERIAL_NUMBER());
        LoggerUtils.d(TAG + "Conf.LICENSE = " + ConfKt.getLICENSE());
        LoggerUtils.d(TAG + "Conf.SERVER_PUBLIC_KEY = " + ConfKt.getSERVER_PUBLIC_KEY());
    }

    /**
     * 处理I6S响应数据
     *
     * @param response             返回json数据
     * @param serialNumberListener 接口回调
     */
    public static void processI6SResponse(String response, SerialNumberListener serialNumberListener) {
        LoggerUtils.d(TAG + response);
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String result = jsonObject.getString("result");
                if (TextUtils.equals("success", result)) {
                    //获取串号
                    String data = jsonObject.getString("data");
                    //将串号写入文件
                    FileUtil.saveSN(data);
                    //分割串号获取对应值
                    spilitSerailNumber(data);
                    LoggerUtils.d(TAG + data);
                    if (serialNumberListener != null) {
                        serialNumberListener.onSerailNumberListener(1);
                    }
                    return;
                }
            } catch (JSONException e) {
                if (serialNumberListener != null) {
                    serialNumberListener.onSerailNumberListener(0);
                }
                e.printStackTrace();
            }
        } else {
            if (serialNumberListener != null) {
                serialNumberListener.onSerailNumberListener(0);
            }
        }

    }

    /**
     * 处理T6响应数据
     *
     * @param response             返回json数据
     * @param serialNumberListener 接口回调
     */
    public static void processResponse(String response, SerialNumberListener serialNumberListener) {
        LoggerUtils.d(TAG + response);
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String result = jsonObject.getString("ret");
                if (TextUtils.equals("1", result)) {
                    //获取串号
                    String data = jsonObject.getString("content");
                    //将串号写入文件
                    FileUtil.saveSN(data);
                    //分割串号获取对应值
                    spilitSerailNumber(data);
                    LoggerUtils.d(TAG + data);
                    if (serialNumberListener != null) {
                        serialNumberListener.onSerailNumberListener(1);
                    }
                    return;
                } else {
                    if (serialNumberListener != null) {
                        serialNumberListener.onSerailNumberListener(0);
                    }
                }
            } catch (JSONException e) {
                if (serialNumberListener != null) {
                    serialNumberListener.onSerailNumberListener(0);
                }
                e.printStackTrace();
            }
        }
    }


    //视频聊天的串号读取没有使用指定的路径，需要修改为读取SN号码，SN号码
    private static String getSerialNumber() {
        String serial = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialnocustom");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }

    /**
     * 是否走网络请求串号
     *
     * @return true 是网络 false 本地
     */
    public static boolean matchDevice() {
        LoggerUtils.d("Build.ID = " + Build.ID + ",Build.MODEL = " + Build.MODEL);

        boolean matchI6S = (SerialNumberManager.CHENXIN_ID.equals(Build.ID));

        boolean matchI6SPro = (SerialNumberManager.CHENXIN_PRO_ID.equals(Build.ID));

        boolean matchT6 = (SerialNumberManager.T6_ID.equals(Build.ID));

        boolean matchL1 = (SerialNumberManager.L1_MODEL.equals(Build.MODEL));

        boolean matchI6SC = (SerialNumberManager.I6SC_MODEL.equals(Build.MODEL));

        return matchI6S || matchT6 || matchL1 || matchI6SC || matchI6SPro;
    }


    /**
     * 串号返回监听
     */
    public interface SerialNumberListener {
        /**
         * @param ret ret=1 成功  ret=0失败
         */
        void onSerailNumberListener(int ret);
    }

}
