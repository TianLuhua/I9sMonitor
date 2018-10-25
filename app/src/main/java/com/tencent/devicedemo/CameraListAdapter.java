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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.booyue.monitor.R;

public class CameraListAdapter extends BaseAdapter {
    private static String TAG = "CameraListAdapter";

    class TXCameraInfo {
        public long din = 0;
        public String remark;
        public String headUrl;

        public TXCameraInfo(long din, String remark, String headUrl) {
            this.din = din;
            this.remark = remark;
            this.headUrl = headUrl;
        }
    }

    public interface INotifyCameraConnect {
        public abstract void nofityCameraConnect(long din);
    }

    private Context mContext;
    private INotifyCameraConnect mNotify;
    private List<TXCameraInfo> mListCamera;
    private String mCameraPicPath;
    private Handler mHandler = null;
    private Set<String> mSetFetching = new HashSet<String>();

    public CameraListAdapter(Context applicationContext, INotifyCameraConnect notify) {
        // TODO Auto-generated constructor stub
        mContext = applicationContext;
        mNotify = notify;
        mListCamera = new ArrayList<TXCameraInfo>();
        mCameraPicPath = mContext.getCacheDir().getAbsolutePath() + "/icon";
        File file = new File(mCameraPicPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        mHandler = new Handler(mContext.getMainLooper()) {
            public void handleMessage(Message msg) {
                CameraListAdapter.this.notifyDataSetChanged();
            }
        };
    }

    public void addCameraInfo(long din, String remark, String headUrl) {
        for (int i = 0; i < mListCamera.size(); ++i) {
            if (mListCamera.get(i).din == din) {
                return;
            }
        }
        mListCamera.add(new TXCameraInfo(din, remark, headUrl));
        notifyDataSetChanged();
    }

    public void clear() {
        mListCamera.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mListCamera.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return mListCamera.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup arg2) {
        // TODO Auto-generated method stub
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.cameralayout, null);
        }

        TextView remark = (TextView) convertView.findViewById(R.id.camera_remark);
        remark.setText(mListCamera.get(position).remark);

        ImageView icon = (ImageView) convertView.findViewById(R.id.camera_icon);
        Bitmap bitmap = getCameraPic(mListCamera.get(position).headUrl);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.binder_default_head);
            icon.setImageBitmap(bitmap);
            fetchCameraPic(mListCamera.get(position).headUrl);
        } else {
            icon.setImageBitmap(bitmap);
        }

        convertView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                long din = mListCamera.get(position).din;
                if (mNotify != null) {
                    mNotify.nofityCameraConnect(din);
                }
            }
        });

        return convertView;
    }

    public Bitmap getCameraPic(String strHeadUrl) {
        Bitmap bitmap = null;
        try {
            String strHeadPic = mCameraPicPath + "/" + getMd5(strHeadUrl) + ".png";
            bitmap = BitmapFactory.decodeFile(strHeadPic);

        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
        return bitmap;
    }

    public void saveCameraPic(String strHeadUrl, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        String strHeadPic = mCameraPicPath + "/" + getMd5(strHeadUrl) + ".png";
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

    public void fetchCameraPic(final String strUrl) {
        synchronized (mSetFetching) {
            if (mSetFetching.contains(strUrl)) {
                return;
            } else {
                mSetFetching.add(strUrl);
            }
        }

        new Thread() {
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    saveCameraPic(strUrl, bitmap);
                    synchronized (mSetFetching) {
                        mSetFetching.remove(strUrl);
                    }
                    mHandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }
            }
        }.start();
    }

    ////////////////////////// 计算String的MD5 /////////////////////////////////

    private static MessageDigest md5 = null;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMd5(String str) {
        byte[] bs = md5.digest(str.getBytes());
        StringBuilder sb = new StringBuilder(40);
        for (byte x : bs) {
            if ((x & 0xff) >> 4 == 0) {
                sb.append("0").append(Integer.toHexString(x & 0xff));
            } else {
                sb.append(Integer.toHexString(x & 0xff));
            }
        }
        return sb.toString();
    }
}
