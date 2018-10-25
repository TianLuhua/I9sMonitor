package com.tencent.devicedemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.util.LoggerUtils;
import com.tencent.util.WindowsUtils;
import com.tencent.av.VideoController;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.tencent.device.TXFriendInfo;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.booyue.monitor.R;


public class MainActivity extends Activity {

    private String TAG = this.getClass().getSimpleName() + "------------";

    private GridView mGridView;
    private BinderListAdapter mBinderAdapter;
    private NotifyReceiver mNotifyReceiver;
    private TextView mFriendTextView;
    private GridView mFriendGridView;
    private FriendListAdapter mFriendAdapter;
    private int mFriendListItemIndex = 0;
    private Set<Long> mNewFriendReqList = new HashSet<Long>();
    private Handler mHandler = null;
    private Bitmap mNewFriendHeadPic = null;
    private Button mContextMenu;
    private Button mUploadLog;
    private Button mEraseAllBinders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated constructor stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent startIntent = new Intent(this, TXDeviceService.class);
        startService(startIntent);

        mGridView = (GridView) findViewById(R.id.gridView_binderlist);
        mBinderAdapter = new BinderListAdapter(this);
        mGridView.setAdapter(mBinderAdapter);

        mFriendTextView = (TextView) findViewById(R.id.textView_friendtext);

        mFriendGridView = (GridView) findViewById(R.id.gridView_friendlist);
        mFriendAdapter = new FriendListAdapter(this);
        mFriendGridView.setAdapter(mFriendAdapter);
        this.registerForContextMenu(mFriendGridView);
        mFriendGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
                mFriendListItemIndex = arg2;
                return false;
            }

        });


        mUploadLog = (Button) findViewById(R.id.btn_upload_log);
        mEraseAllBinders = (Button) findViewById(R.id.btn_eraseallbinders);

        mUploadLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadDeviceLog(v);
            }
        });

        mEraseAllBinders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eraseAllBinders(v);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(TXDeviceService.BinderListChange);
        filter.addAction(TXDeviceService.OnEraseAllBinders);

        filter.addAction(TXDeviceService.OnGetSociallyNumber);
        filter.addAction(TXDeviceService.OnFriendListChange);
        filter.addAction(TXDeviceService.OnReceiveAddFriendReq);
        filter.addAction(TXDeviceService.OnDelFriend);
        filter.addAction(TXDeviceService.OnModifyFriendRemark);
        mNotifyReceiver = new NotifyReceiver();
        registerReceiver(mNotifyReceiver, filter);

        boolean bNetworkSetted = this.getSharedPreferences("TXDeviceSDK", 0).getBoolean("NetworkSetted", false);
        if (TXDeviceService.NetworkSettingMode == true && bNetworkSetted == false) {
            Intent intent = new Intent(MainActivity.this, WifiDecodeActivity.class);
            startActivity(intent);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub 
        ListItemInfo info = mFriendAdapter.getListItemInfo(mFriendListItemIndex);
        if (info.type != ListItemInfo.LISTITEM_TYPE_ADD_FRIEND) {
            menu.add(0, 0, 0, "语音通话");
            menu.add(0, 1, 1, "视频通话");
            menu.add(0, 2, 2, "修改备注名");
            menu.add(0, 3, 3, "删除好友");
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub        
        if (3 == item.getItemId()) {//删除好友
            AlertDialog dialog = null;
            Builder builder = new AlertDialog.Builder(this).setTitle("删除好友").setMessage("您确定要删除此好友吗？").setPositiveButton("取消", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    dialog.dismiss();

                }
            }).setNegativeButton("删除", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    dialog.dismiss();
                    ListItemInfo info = mFriendAdapter.getListItemInfo(mFriendListItemIndex);
                    TXDeviceService.delFriend(info.id);
                }
            });
            dialog = builder.create();
            dialog.show();
        } else if (2 == item.getItemId()) {//修改备注名
            final ListItemInfo info = mFriendAdapter.getListItemInfo(mFriendListItemIndex);
            final EditText et = new EditText(this);
            et.setText(info.nick_name, TextView.BufferType.EDITABLE);
            et.setSelection(info.nick_name.length());

            AlertDialog remarkDialog = null;
            remarkDialog = new AlertDialog.Builder(this).setTitle("请输入新的备注名").setIcon(android.R.drawable.ic_dialog_info).setView(et).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    dialog.dismiss();

                    String newRemark = et.getText().toString();
                    if (newRemark.length() == 0) {
                        Log.d("MainActivity", "the input new remark name is empty");
                        String init_name = TXDeviceService.getFriendInitialName(info.id);
                        Log.d("MainActivity", "initial_name:" + init_name);
                        if (init_name != null) {
                            TXDeviceService.modifyFriendRemark(info.id, init_name);
                        }
                    } else {
                        Log.d("MainActivity", "new remark name is: " + newRemark);
                        TXDeviceService.modifyFriendRemark(info.id, newRemark);
                    }
                }
            }).setNegativeButton("取消", null).show();
        } else if (1 == item.getItemId()) {//视频通话
            ListItemInfo info = mFriendAdapter.getListItemInfo(mFriendListItemIndex);
            if (MainActivity.isNetworkAvailable(this)) {
                if (false == VideoController.getInstance().hasPendingChannel()) {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "启动中...", Toast.LENGTH_SHORT).show();
                    TXDeviceService.getInstance().startVideoChatActivity(info.id, VideoController.UINTYPE_DIN);
                } else {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "视频中", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this.getApplicationContext(), "当前网络不可用，请连接网络", Toast.LENGTH_SHORT).show();
            }
        } else if (0 == item.getItemId()) {//语音通话
            ListItemInfo info = mFriendAdapter.getListItemInfo(mFriendListItemIndex);
            if (MainActivity.isNetworkAvailable(this)) {
                if (false == VideoController.getInstance().hasPendingChannel()) {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "启动中...", Toast.LENGTH_SHORT).show();
                    TXDeviceService.getInstance().startAudioChatActivity(info.id, VideoController.UINTYPE_DIN);
                } else {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "视频中", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this.getApplicationContext(), "当前网络不可用，请连接网络", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onContextItemSelected(item);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (cm.getActiveNetworkInfo() != null) {
                return cm.getActiveNetworkInfo().isAvailable();
            }
        }
        return false;
    }

    /**
     * 解除所有的绑定
     *
     * @param v
     */
    public void eraseAllBinders(View v) {
        AlertDialog dialog = null;
        Builder builder = new AlertDialog.Builder(this).setTitle("解除绑定").setMessage("您确定要解绑所有用户吗？").setPositiveButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        }).setNegativeButton("解除绑定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
                TXDeviceService.eraseAllBinders();
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    /**
     * 上传设备日志
     *
     * @param v
     */
    public void uploadDeviceLog(View v) {
        TXDeviceService.getInstance().uploadSDKLog();
    }

    public void remoteBind(View v) {
        startActivity(new Intent(this, RemoteBindActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        /**
         *  获取社交号
         */
        long sociallyNum = TXDeviceService.getSociallyNumber();
        if (0 != sociallyNum) {
            setSocaillyNumber(sociallyNum);
        }

        /**
         * 获取设备好友列表
         */
        TXFriendInfo[] arrayFriend = TXDeviceService.getFriendList();
        if (arrayFriend != null) {
            List<TXFriendInfo> friendList = new ArrayList<>();
            for (int i = 0; i < arrayFriend.length; ++i) {
                friendList.add(arrayFriend[i]);
            }
            if (mFriendAdapter != null) {
                mFriendAdapter.freshFriendList(friendList);
            }
        }


        float density = WindowsUtils.getDisplayDensity(this);
        int displayDensityDpi = WindowsUtils.getDisplayDensityDpi(this);
        int height = WindowsUtils.getDisplayHeight(this);
        int width = WindowsUtils.getDisplayWidth(this);
        LoggerUtils.d(TAG + "width = " + width + ",Height = " + height + ",density = " + density

                + ",displayDensity = " + displayDensityDpi);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNotifyReceiver);
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
                    List<TXBinderInfo> binderList = new ArrayList<TXBinderInfo>();
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
                    showAlert("解除绑定失败", "解除绑定失败，错误码:" + resultCode);
                } else {
                    showAlert("解除绑定成功", "解除绑定成功!!!");
                }
                /**
                 *设备号
                 */
            } else if (intent.getAction() == TXDeviceService.OnGetSociallyNumber) {
                int result = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 == result) {
                    long sociallyNum = intent.getExtras().getLong("SociallyNumber");
                    if (0 != sociallyNum) {
                        setSocaillyNumber(sociallyNum);
                    }
                }
                /**
                 * 朋友列表改变
                 */
            } else if (intent.getAction() == TXDeviceService.OnFriendListChange) {
                Parcelable[] listTemp = intent.getExtras().getParcelableArray("FriendList");
                int length = listTemp != null ? listTemp.length : 0;
                List<TXFriendInfo> friendList = new ArrayList<>();
                for (int i = 0; i < length; ++i) {
                    TXFriendInfo friend = (TXFriendInfo) (listTemp[i]);
                    friendList.add(friend);
                }
                if (mFriendAdapter != null) {
                    mFriendAdapter.freshFriendList(friendList);
                }
                /**
                 * 接收所有朋友的请求
                 */
            } else if (intent.getAction() == TXDeviceService.OnReceiveAddFriendReq) {
                TXFriendInfo friendInfo = intent.getParcelableExtra("FriendInfo");
                String strValidationMsg = intent.getStringExtra("ValidationMsg");
                long socialNumber = intent.getLongExtra("SocialNumber", 0);
                synchronized (mNewFriendReqList) {
                    if (mNewFriendReqList.contains(friendInfo.friend_din)) {
                        //同一个好友的连续多次请求只处理一次
                    } else {
                        mNewFriendReqList.add(friendInfo.friend_din);
                        showNewFriendReqFloatWin(friendInfo, strValidationMsg, socialNumber);
                    }
                }
                /**
                 * 删除好友
                 */
            } else if (intent.getAction() == TXDeviceService.OnDelFriend) {
                int resultCode = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != resultCode) {
                    showAlert("删除好友失败", "删除好友失败，错误码:" + resultCode);
                }
                /**
                 * 修改好友
                 */
            } else if (intent.getAction() == TXDeviceService.OnModifyFriendRemark) {
                int resultCode = intent.getExtras().getInt(TXDeviceService.OperationResult);
                if (0 != resultCode) {
                    showAlert("修改备注结果", "错误码:" + resultCode);
                }
            }
        }
    }

    /**
     * 获取新朋友的头像
     *
     * @param strUrl
     */
    public void fetchNewFriendHeadPic(final String strUrl) {
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    mNewFriendHeadPic = BitmapFactory.decodeStream(stream);
                    mHandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * 显示新朋友请求弹窗
     *
     * @param friendInfo
     * @param validationMsg
     * @param socialNumber
     */
    private void showNewFriendReqFloatWin(final TXFriendInfo friendInfo, final String validationMsg, final long socialNumber) {
        final View contentView = LayoutInflater.from(this).inflate(R.layout.view_newfriendreq, null);
        TextView textView = (TextView) contentView.findViewById(R.id.newfriendreq_text);
        String showText = friendInfo.getDeviceName() + "(" + Long.toString(socialNumber) + ")";//+"请求添加好友"; //friendInfo.getAdminRemark() + "的" + friendInfo.str_device_type + "请求添加好友。";
        textView.setText(showText);

        final ImageView imgView = (ImageView) contentView.findViewById(R.id.friendinfo_headpic);

        mHandler = new Handler(this.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (mNewFriendHeadPic != null) {
                    imgView.setImageBitmap(mNewFriendHeadPic);
                }
            }
        };

        fetchNewFriendHeadPic(friendInfo.head_url);

        final WindowManager wm = (WindowManager) this.getApplication().getSystemService(Context.WINDOW_SERVICE);
        //设置LayoutParams(全局变量）相关参数
        final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        wmParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wmParams.format = PixelFormat.TRANSLUCENT;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.gravity = Gravity.CENTER;
        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        ((WindowManager) this.getWindowManager()).addView(contentView, wmParams);
        //wm.addView(contentView, wmParams);

        contentView.setOnTouchListener(new View.OnTouchListener() {
            private float mTouchStartX = 0;
            private float mTouchStartY = 0;
            private float mX = 0;
            private float mY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                mX = event.getRawX();
                mY = event.getRawY() - 25;   //25是系统状态栏的高度
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //获取相对View的坐标，即以此View左上角为原点
                        mTouchStartX = event.getX();
                        mTouchStartY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        updateViewPosition();
                        break;

                    case MotionEvent.ACTION_UP:
                        updateViewPosition();
                        mTouchStartX = mTouchStartY = 0;
                        break;
                }
                return false;  //此处必须返回false，否则OnClickListener获取不到监听
            }

            private void updateViewPosition() {
                //更新浮动窗口位置参数
                wmParams.x = (int) (mX - mTouchStartX);
                wmParams.y = (int) (mY - mTouchStartY);
                wmParams.gravity = Gravity.TOP;
                ((WindowManager) MainActivity.this.getWindowManager()).updateViewLayout(contentView, wmParams);
            }

        });

        View.OnClickListener clickListerner = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Intent friendIntent = new Intent(MainActivity.this, FriendInfoActivity.class);
                friendIntent.putExtra("FriendInfo", friendInfo);
                friendIntent.putExtra("type", FriendInfoActivity.ACTIVITY_TYPE_NEW_FRIEND_REQ);
                friendIntent.putExtra("ValidationMsg", validationMsg);
                friendIntent.putExtra("SocialNumber", socialNumber);
                startActivity(friendIntent);
                MainActivity.this.getWindowManager().removeView(contentView);
                //wm.removeView(contentView);

                synchronized (mNewFriendReqList) {
                    mNewFriendReqList.remove(friendInfo.friend_din);
                }
            }
        };

        Button btn = (Button) contentView.findViewById(R.id.newfriendreq_btn);
        btn.setOnClickListener(clickListerner);
        //contentView.setOnClickListener(clickListerner);

    }

    /**
     * 显示对话框
     *
     * @param strTitle
     * @param strMsg
     */
    private void showAlert(String strTitle, String strMsg) {
        // TODO Auto-generated method stub
        AlertDialog dialogError;
        Builder builder = new AlertDialog.Builder(this).setTitle(strTitle).setMessage(strMsg).setPositiveButton("取消", null).setNegativeButton("确定", null);
        dialogError = builder.create();
        dialogError.show();
    }

    /**
     * 设置设备号
     *
     * @param number
     */
    private void setSocaillyNumber(long number) {
        String text = "好友列表（设备号：" + number + "）";
        mFriendTextView.setText(text);
    }
}
