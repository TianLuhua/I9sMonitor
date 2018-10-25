package com.tencent;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.booyue.monitor.R;
import com.tencent.devicedemo.WifiDecodeActivity;
import com.tencent.util.AppUtil;
import com.tencent.util.FileUtil;
import com.tencent.util.LoggerUtils;
import com.tencent.util.UpgradeUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
/**
 * 使用qq进行与火火兔绑定的引导页
 */

/**
 * Created by Administrator on 2017/5/23.
 * app主页（绑定列表页面）
 *
 */
public class BooyueFriendListActivity extends BaseActivity {
    private static final String TAG = "BooyueFriendListActivity-------";
    //返回
    private TextView tvBack;
    //上一页
    private TextView tvPre;
    //下一页
    private TextView tvNext;
    //绑定列表展示的容器
    private RecyclerView recyclerView;
    //通知广播
    private NotifyReceiver mNotifyReceiver;
    //列表适配器
    private FriendListAdapter mBinderAdapter;
    //RecyclerView的显示布局
    private LinearLayoutManager linearLayoutManager;
    //解除所有绑定
    private ImageButton ibEraseAllBinders;
    private AlertDialog dialog;
    private boolean checkUpgrade = false;

    @Override
    public void setView() {
        setContentView(R.layout.activity_friendlist);
    }

    @Override
    public void initView() {
        /**
         * 获取串号（分两种情况：同步和异步）
         * 同步：本地获取
         * 异步：网络获取之后通过回调调用
         */
        SerialNumberManager.readSerialNumber(this, new SerialNumberListener() {
            //异步获取之后回调，同步不回调此方法，同步方法执行在line 70
            @Override
            public void onSerailNumberListener(final int ret) {
                LoggerUtils.d(TAG + "onSerailNumberListener");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                         startService();
                    }
                });
            }
        });
        LoggerUtils.d(TAG + "-------------------------");
        // TODO: 2018/2/26
        // 晨芯方案商&串号没有写入文件，
        // 不需要做任何操作，此时启动线程网络获取串号并写入文件，操作执行在接口回调中（line 60）
        /**modify by : 2018/3/5 16:17*/ //T6改用服务端获取数据
        if(!FileUtil.isSNCached() && SerialNumberManager.matchDevice()){//启动线程通过回调获取

        }else {
           startService();
        }


//
//        if(!NetWorkUtils.isWifiActive(this)){
//            Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
//        }
//      String sn = "1700004781;05914E91D7874e35;3044022065B3B9F00ADEAF680FB0CABB2170F19A02F2857D77D4D23CC41ABF7D2835315702205705BFBC33CDB7EF06D38BEB33E6CCB10736073354E9A96F3CEFBC5ABDF085C2;04D0B6C03324295914363D34E0801043CFAC3159AE89B6150057AC3E59BB2DE3B3BE43FF4C424A734892C78CA8125CF3EC";
////        String sn = "1700005382;FB8BFCD862274d43;3045022100BB556608E834992CBCA928D740D689B3C657A5457162CCCC664C524DE65497B402205BAE197702729A6A4EA1CDC12C7C6678D66A75F85BC122CEA594ADCDA1547BB6;04C51918B8B3E2ABB44CD61BFCE7A9E6723EBFE36EA2A6F1C76992F339B26975B7C436444EBF495541CED5E4C0687D108D";
//        SerialNumberManager.spilitSerailNumber(sn);
//        startService();



        tvBack = (TextView) findViewById(R.id.tv_back);
        ibEraseAllBinders = (ImageButton) findViewById(R.id.ib_erase_all_binders);
        ibEraseAllBinders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogManager.createAlertDialog(BooyueFriendListActivity.this, 0, 0, null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TXDeviceService.eraseAllBinders();
                        /**modify by : 2018/3/1 18:18 如果是晨芯 需要删除串号文件*/
                        FileUtil.cleanSNFile();
                    }
                });
            }
        });
        tvPre = (TextView) findViewById(R.id.tv_pre);
        tvNext = (TextView) findViewById(R.id.tv_next);

        recyclerView = (RecyclerView) findViewById(R.id.recylerview_list);

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pre();
            }
        });

        tvNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        initRecyclerView();

        if(!checkUpgrade){
            checkUpgrade();
        }
    }

    /**
     * 启动服务
     */
    public void startService(){
        if (Conf.PRODUCT_ID == 0 || TextUtils.isEmpty(Conf.LICENSE) || TextUtils.isEmpty(Conf.SERIAL_NUMBER)
                || TextUtils.isEmpty(Conf.SERVER_PUBLIC_KEY)) {
            showToast(R.string.unique_identifier);
        } else {
            Intent startIntent = new Intent(BooyueFriendListActivity.this, TXDeviceService.class);
            startService(startIntent);
        }
    }

    /**
     * 上一页
     */
    public void pre() {
        if (mBinderAdapter != null && mBinderAdapter.getItemCount() > 4) {
            //获取第一个view对应的位置
            int firstPosition = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(0));
            //获取最后一个view对应的位置
            int lastPosition = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(recyclerView.getChildCount() - 1));
            if (firstPosition > 0) {
                recyclerView.smoothScrollToPosition(firstPosition - 1);

            }
        }
    }

    /**
     * 下一页
     */
    public void next() {

        if (mBinderAdapter != null && mBinderAdapter.getItemCount() > 4) {
            int firstPosition = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(0));
            int lastPosition = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(recyclerView.getChildCount() - 1));
            if (lastPosition < mBinderAdapter.getItemCount() - 1) {
//                recyclerView.smoothScrollToPosition(lastPosition - 3 + 1);
                int left = recyclerView.getChildAt(1).getLeft();
                recyclerView.smoothScrollBy(left, 0);
            }
        }

    }

    /**
     * 初始化recyclerview
     */
    private void initRecyclerView() {
        //创建布局管理
        linearLayoutManager = new LinearLayoutManager(this, LinearLayout.HORIZONTAL, false);
        //设置布局
        recyclerView.setLayoutManager(linearLayoutManager);
        //创建适配器
        mBinderAdapter = new FriendListAdapter(this);
        //设置适配器
        recyclerView.setAdapter(mBinderAdapter);

        List<TXBinderInfo> mBinderList = new ArrayList<>();
        //刷新列表
        mBinderAdapter.freshBinderList(mBinderList);

    }

    @Override
    public void initData() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TXDeviceService.BinderListChange);//绑定列表改变
        filter.addAction(TXDeviceService.OnEraseAllBinders);//解除所有的绑定者

        filter.addAction(TXDeviceService.OnGetSociallyNumber);//获取设备号
        filter.addAction(TXDeviceService.OnFriendListChange);//朋友列表变化
        filter.addAction(TXDeviceService.OnReceiveAddFriendReq);//接收添加朋友请求
        filter.addAction(TXDeviceService.OnDelFriend);//删除朋友
        filter.addAction(TXDeviceService.OnModifyFriendRemark);//修改朋友标志
        mNotifyReceiver = new NotifyReceiver();
        registerReceiver(mNotifyReceiver, filter);

        boolean bNetworkSetted = this.getSharedPreferences("TXDeviceSDK", 0).getBoolean("NetworkSetted", false);
        if (TXDeviceService.NetworkSettingMode == true && bNetworkSetted == false) {
            LoggerUtils.d(TAG + "start WifiDecodeActivity.class");
            Intent intent = new Intent(BooyueFriendListActivity.this, WifiDecodeActivity.class);
            startActivity(intent);
        }

    }

    public class NotifyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
             * 绑定列表改变
             */
            if (intent.getAction() == TXDeviceService.BinderListChange) {
                try {
                    Parcelable[] listTemp = intent.getExtras().getParcelableArray("binderlist");
                    List<TXBinderInfo> binderList = new ArrayList<>();
                    for (int i = 0; i < listTemp.length; ++i) {
                        TXBinderInfo binder = (TXBinderInfo) (listTemp[i]);
                        binderList.add(binder);
                    }
                    if (mBinderAdapter != null) {
                        mBinderAdapter.freshBinderList(binderList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /**
                 * 解除绑定列表
                 *
                 * resultcode 结果码  检测解除成功与否
                 */
            } else if (intent.getAction() == TXDeviceService.OnEraseAllBinders) {
                int resultCode = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != resultCode) {
                    showToast("解除绑定失败，错误码:" + resultCode);
                } else {
                    showToast("解除绑定成功!!!");
                }
                /**
                 *设备号
                 */
            } else if (intent.getAction() == TXDeviceService.OnGetSociallyNumber) {
                int result = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 == result) {
                    long sociallyNum = intent.getExtras().getLong("SociallyNumber");
                    if (0 != sociallyNum) {
                        showToast("设备号：" + sociallyNum);
                    }
                }
                /**
                 * 朋友列表改变
                 */
            } else if (intent.getAction() == TXDeviceService.OnFriendListChange) {
//                Parcelable[] listTemp = intent.getExtras().getParcelableArray("FriendList");
//                int length = listTemp != null ? listTemp.length : 0;
//                List<TXFriendInfo> friendList = new ArrayList<>();
//                for (int i = 0; i < length; ++i) {
//                    TXFriendInfo friend = (TXFriendInfo) (listTemp[i]);
//                    friendList.add(friend);
//                }
//                if (mFriendAdapter != null) {
//                    mFriendAdapter.freshFriendList(friendList);
//                }
                /**
                 * 接收所有朋友的请求
                 */
            } else if (intent.getAction() == TXDeviceService.OnReceiveAddFriendReq) {
//                TXFriendInfo friendInfo = intent.getParcelableExtra("FriendInfo");
//                String strValidationMsg = intent.getStringExtra("ValidationMsg");
//                long socialNumber = intent.getLongExtra("SocialNumber", 0);
//                synchronized (mNewFriendReqList) {
//                    if (mNewFriendReqList.contains(friendInfo.friend_din)) {
//                        //同一个好友的连续多次请求只处理一次
//                    } else {
//                        mNewFriendReqList.add(friendInfo.friend_din);
//                        showNewFriendReqFloatWin(friendInfo, strValidationMsg, socialNumber);
//                    }
//                }
                /**
                 * 删除好友
                 */
            } else if (intent.getAction() == TXDeviceService.OnDelFriend) {
                int resultCode = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != resultCode) {
                    showToast("删除好友失败：错误码" + resultCode);
                } else {
                    showToast("删除好友成功");
                }
                /**
                 * 修改好友备注
                 */
            } else if (intent.getAction() == TXDeviceService.OnModifyFriendRemark) {
                int resultCode = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != resultCode) {
                    showToast("修改好友备注失败：错误码" + resultCode);
                } else {
                    showToast("修改好友备注成功");
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBinerList();


    }

    /**
     * 检测升级
     */
    private void checkUpgrade(){
        UpgradeUtil.checkUpgrade(this, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response != null && response.isSuccessful()){
                    String result = response.body().string();
                    LoggerUtils.d(TAG +"checkUpgrade response = " + result);
                    processResult(result);
                }
            }
        });

    }


    private  void processResult(String result) {
        if(result == null || result == "")return;
        try {
            JSONObject jsonObject = new JSONObject(result);
            if("1".equals(jsonObject.getString("ret"))){
                JSONObject contentOject = jsonObject.getJSONObject("content");
                final String apk = contentOject.getString("apk");
                String newVersion = contentOject.getString("newVersion");
                final String tips = contentOject.getString("video_content");
                if(!TextUtils.isEmpty(apk)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            View view = BooyueFriendListActivity.this.getLayoutInflater().inflate(R.layout.dialog_upgrade, null);
                            dialog = CommonDialog.showAppUpgradeDialog(BooyueFriendListActivity.this, view);
                            initUpgradeView(view, tips, apk);

                        }
                    });
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化升级对话框
     * @param view 升级对话框
     * @param tips 提示语
     * @param apkUrl 新版本apk地址
     */
    private void initUpgradeView(View view, String tips, final String apkUrl) {
        if (view == null) throw new NullPointerException();
        LoggerUtils.d(TAG + "initUpgradeView()");
        TextView tvMessage = (TextView) view.findViewById(R.id.tv_msg);
        Button btnCancel = (Button) view.findViewById(R.id.btn_cancel);
        Button btnUpgrder = (Button) view.findViewById(R.id.btn_upgrade);

        tvMessage.setText(tips);
        //销毁提示框
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                dialog = null;
                checkUpgrade = false;
            }
        });
        //确定 升级下载
        btnUpgrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                dialog = null;
                checkUpgrade = true;
                new UpgradeUtil().downLoadApk(BooyueFriendListActivity.this, apkUrl);
            }
        });
    }


    private void updateBinerList() {
        /**
         * 更新绑定列表
         */
        TXBinderInfo[] arrayBinder = TXDeviceService.getBinderList();
        if (arrayBinder != null) {
            List<TXBinderInfo> binderList = new ArrayList<>();
            for (int i = 0; i < arrayBinder.length; ++i) {
                binderList.add(arrayBinder[i]);
            }
            if (mBinderAdapter != null) {
                mBinderAdapter.freshBinderList(binderList);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mNotifyReceiver);
        super.onDestroy();
    }

    /**
     * 只有app在前台才弹吐司
     *
     * @param text 弹出吐司的内容
     */
    public void showToast(String text) {
        LoggerUtils.d(TAG + text);
        if (AppUtil.isAppOnForeground()) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    public void showToast(int text) {
        if (AppUtil.isAppOnForeground()) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }


}
