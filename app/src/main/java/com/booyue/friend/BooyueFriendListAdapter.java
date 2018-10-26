package com.booyue.friend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.MyApp;
import com.booyue.binding.BooyueGuideActivity;
import com.booyue.monitor.R;
import com.booyue.widget.CircleImageView;
import com.tencent.av.VideoController;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;
import com.tencent.devicedemo.ListItemInfo;
import com.tencent.devicedemo.MainActivity;
import com.tencent.util.NetWorkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2017/5/24.
 * <p/>
 * 朋友列表适配器
 */
class BooyueFriendListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static String TAG = "BooyueFriendListAdapter";
    private Context mContext;
    private String mHeadPicPath;
    private Handler mHandler = null;
    private Set<Long> mSetFetching = new HashSet<>();
    private LayoutInflater layoutInflater;
    private List<TXBinderInfo> mListBinder = new ArrayList<>();
//    private final ImageUtils imageUtils;


    public BooyueFriendListAdapter(Context context) {
        this.mContext = context;
        layoutInflater = LayoutInflater.from(this.mContext);
        mHeadPicPath = mContext.getCacheDir().getAbsolutePath() + "/head";
        File file = new File(mHeadPicPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        mHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                BooyueFriendListAdapter.this.notifyDataSetChanged();
            }
        };
//        imageUtils = new ImageUtils(mHandler, mSetFetching);
    }

    public void freshBinderList(List<TXBinderInfo> binderList) {
        mListBinder.clear();
        for (int i = 0; i < binderList.size(); ++i) {
            TXBinderInfo binder = binderList.get(i);
            mListBinder.add(binder);
        }

        //增加一个加好友项
        TXBinderInfo addItem = new TXBinderInfo();
        addItem.nick_name = new byte[0];
        addItem.head_url = "";
        addItem.tinyid = 0;
        addItem.binder_type = ListItemInfo.LISTITEM_TYPE_ADD_FRIEND;
        mListBinder.add(addItem);
        notifyDataSetChanged();
    }

    public ListItemInfo getListItemInfo(int index) {
        ListItemInfo item = new ListItemInfo();
        TXBinderInfo binder = mListBinder.get(index);
        item.id = binder.tinyid;
        item.head_url = binder.head_url;
//        item.type = ListItemInfo.LISTITEM_TYPE_BINDER;
        item.nick_name = binder.getNickName();

        if (index == mListBinder.size() - 1) {
            item.type = ListItemInfo.LISTITEM_TYPE_ADD_FRIEND;  //列表最后一个是添加好友项
        } else {
            item.type = ListItemInfo.LISTITEM_TYPE_BINDER; //其余的是设备好友
        }
        return item;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.friendlist_item, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final ListItemInfo item = getListItemInfo(position);
        if (item == null) {
            return;
        }
        final MyViewHolder holder1 = (MyViewHolder) holder;

        if (item.type == ListItemInfo.LISTITEM_TYPE_ADD_FRIEND) {
            holder1.ivAvatar.setImageResource(R.drawable.add_more);
            holder1.tvName.setText(R.string.add_friend);
            holder1.llFunction.setVisibility(View.GONE);
        } else {

            holder1.tvName.setText(item.nick_name);
            holder1.llFunction.setVisibility(View.VISIBLE);
            Bitmap bitmap = getBinderHeadPic(item.id, item.type);
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.head_portrait_default);
                holder1.ivAvatar.setImageBitmap(bitmap);
                if (item.head_url != null && item.head_url.length() > 0) {
                    fetchBinderHeadPic(item.id, item.head_url);
                }
            } else {
                holder1.ivAvatar.setImageBitmap(bitmap);
            }

            holder1.ibPhone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (NetWorkUtils.isNetWorkAvailable(mContext)) {
                        if (false == VideoController.getInstance().hasPendingChannel()) {
                            Toast.makeText(MyApp.getContext(), R.string.launching, Toast.LENGTH_SHORT).show();
                            TXDeviceService.getInstance().startAudioChatActivity(item.id, VideoController.UINTYPE_QQ);
                            ((Activity) mContext).overridePendingTransition(R.anim.dialog_enter_anim_scale, R.anim.dialog_exit_anim_scale);
                        } else {
                            Toast.makeText(MyApp.getContext(), R.string.being_video, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyApp.getContext(), R.string.network_close, Toast.LENGTH_SHORT).show();
                    }

                }
            });
            holder1.ibVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (MainActivity.isNetworkAvailable(mContext)) {
                        if (false == VideoController.getInstance().hasPendingChannel()) {
                            Toast.makeText(MyApp.getContext(), R.string.launching, Toast.LENGTH_SHORT).show();
                            TXDeviceService.getInstance().startVideoChatActivity(item.id, VideoController.UINTYPE_QQ);
                            ((Activity) mContext).overridePendingTransition(R.anim.dialog_enter_anim_scale, R.anim.dialog_exit_anim_scale);
                        } else {
                            Toast.makeText(MyApp.getContext(), R.string.being_video, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyApp.getContext(), R.string.network_close, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        holder1.ivAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListItemInfo item = getListItemInfo(position);
                if (item.type == ListItemInfo.LISTITEM_TYPE_BINDER) {
//                    Intent binder = new Intent(mContext, BinderActivity.class);
//                    binder.putExtra("tinyid", item.id);
//                    binder.putExtra("nickname", item.nick_name);
//                    binder.putExtra("type", item.type);
//                    mContext.startActivity(binder);
//                    ((Activity)mContext).overridePendingTransition(R.anim.slide_right_in,R.anim.slide_left_out);
                } else {
                    Intent binder = new Intent(mContext, BooyueGuideActivity.class);
                    mContext.startActivity(binder);
                }

                if (mItemClickListener != null) {
                    mItemClickListener.onItemClickListener(position);
                }
            }
        });
//        holder1.ivAvatar.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                ListItemInfo item = getListItemInfo(position);
//                int[] position = new int[2];
//                holder1.itemView.getLocationInWindow(position);
//                int width = holder1.itemView.getWidth();
//                Point ivAvatarCenter = new Point();
//                ivAvatarCenter.x = position[0] + width / 2;
//                int displayWidth = WindowsUtils.getDisplayWidth((Activity) mContext);
//                int delta = ivAvatarCenter.x - displayWidth / 2;
//                String desc_header = mContext.getResources().getString(R.string.dialog_desc_header);
//                String desc_footer = mContext.getResources().getString(R.string.dialog_desc_footer);
//                String desc = desc_header + item.nick_name + desc_footer;
//                if (item.type == ListItemInfo.LISTITEM_TYPE_BINDER) {
//                    DialogManager.createAlertDialog(mContext, delta, desc, new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            //执行具体的解除绑定操作
//                            TXDeviceService.eraseAllBinders();
//                        }
//                    });
//
//                }
//                return false;
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return mListBinder.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView ivAvatar;
        private TextView tvName;
        private ImageButton ibPhone;
        private ImageButton ibVideo;
        private LinearLayout llFunction;

        public MyViewHolder(View itemView) {
            super(itemView);
            ivAvatar = (CircleImageView) itemView.findViewById(R.id.iv_avatar);
//            //针对T6机器头像有点偏下，所以需要稍微调整一下
//            if (TextUtils.equals(SerialNumberManager.T6_ID, Build.ID)) {
//                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) ivAvatar.getLayoutParams();
//                layoutParams.topMargin = (int) mContext.getResources().getDimension(R.dimen.dimen_34);
//                ivAvatar.setLayoutParams(layoutParams);
//            }
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
            ibPhone = (ImageButton) itemView.findViewById(R.id.ib_phone);
            ibVideo = (ImageButton) itemView.findViewById(R.id.ib_video);
            llFunction = (LinearLayout) itemView.findViewById(R.id.ll_function);
        }
    }

    public interface ItemClickListener {
        void onItemClickListener(int index);
    }

    private ItemClickListener mItemClickListener;

    public void setOnItemClickListener(ItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;

    }

    /**
     * 获取头像
     *
     * @param uin
     * @param type
     * @return
     */
    public Bitmap getBinderHeadPic(long uin, int type) {
        Bitmap bitmap = null;
        try {
            if (ListItemInfo.LISTITEM_TYPE_ADD_FRIEND == type) {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.add_more);
            } else {
                String strHeadPic = mHeadPicPath + "/" + uin + ".png";
                bitmap = BitmapFactory.decodeFile(strHeadPic);
            }
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
        return bitmap;
    }

    /**
     * 保存头像
     *
     * @param uin
     * @param bitmap
     */
    public void saveBinderHeadPic(long uin, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        String strHeadPic = mHeadPicPath + "/" + uin + ".png";
        File file = new File(strHeadPic);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    /**
     * 从网络获取头像
     *
     * @param uin    标识码
     * @param strUrl 头像url
     */
    public void fetchBinderHeadPic(final long uin, final String strUrl) {
        synchronized (mSetFetching) {
            if (mSetFetching.contains(uin)) {
                return;
            } else {
                mSetFetching.add(uin);
            }
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    saveBinderHeadPic(uin, bitmap);
                    synchronized (mSetFetching) {
                        mSetFetching.remove(uin);
                    }
                    mHandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            }
        }.start();
    }
}
