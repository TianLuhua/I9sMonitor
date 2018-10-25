package com.tencent.devicedemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.booyue.monitor.R;


public class ListAdapter extends BaseAdapter {

    private static String TAG = "ListAdapter";
    private Context mContext;
    private String mHeadPicPath;
    private Handler mHandler = null;
    private Set<Long> mSetFetching = new HashSet<Long>();

    public ListAdapter(Context applicationContext) {
        // TODO Auto-generated constructor stub
        mContext = applicationContext;
        mHeadPicPath = mContext.getCacheDir().getAbsolutePath() + "/head";
        File file = new File(mHeadPicPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        mHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ListAdapter.this.notifyDataSetChanged();
            }
        };
    }


    public ListItemInfo getListItemInfo(int index) {
        return null;
    }

    public void onItemClicked(int index) {

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.binderlayout, null);
        }

        final ListItemInfo item = getListItemInfo(position);
        if (item == null) {
            return null;
        }

        TextView nickName = (TextView) convertView.findViewById(R.id.nick_name);
        nickName.setText(item.nick_name);

        ImageView head = (ImageView) convertView.findViewById(R.id.headpic);
        Bitmap bitmap = getBinderHeadPic(item.id, item.type);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.binder_default_head);
            head.setImageBitmap(bitmap);
            if (item.head_url != null && item.head_url.length() > 0) {
                fetchBinderHeadPic(item.id, item.head_url);
            }
        } else {
            head.setImageBitmap(bitmap);
        }
        convertView.setLongClickable(true);
        convertView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                onItemClicked(position);
            }
        });

        return convertView;
    }

    public Bitmap getBinderHeadPic(long uin, int type) {
        Bitmap bitmap = null;
        try {
            if (ListItemInfo.LISTITEM_TYPE_ADD_FRIEND == type) {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_add_friend);
            } else {
                String strHeadPic = mHeadPicPath + "/" + uin + ".png";
                bitmap = BitmapFactory.decodeFile(strHeadPic);
            }
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
        return bitmap;
    }

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
