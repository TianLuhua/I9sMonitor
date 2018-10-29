package com.booyue.monitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import com.booyue.monitor.videomonitor.VideoMonitorHW;
import com.booyue.monitor.videomonitor.VideoMonitorSF;
import com.tencent.av.VideoController;
import com.tencent.av.core.VideoConstants;
import com.tencent.device.TXBinderInfo;
import com.tencent.device.TXDeviceService;

public class BooyueVideoMonitorService extends Service {
    private static final String TAG = "BooyueVideoMonitorService";

    IVideoMonitor mVideoMonitor = null;
    private String mPeerId;

    public BooyueVideoMonitorService() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent != null) {
            mPeerId = intent.getStringExtra("peerid");
            Log.d(TAG, "peerId = " + mPeerId);

            IntentFilter filter = new IntentFilter();
            filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT);
            filter.addAction(VideoController.ACTION_NETMONIOTR_INFO);
            filter.addAction(VideoController.ACTION_CHANNEL_READY);
            filter.addAction(VideoController.ACTION_VIDEO_QOS_NOTIFY);
            filter.addAction(TXDeviceService.BinderListChange);
            filter.addAction(TXDeviceService.OnEraseAllBinders);
            registerReceiver(mBroadcastHandler, filter);

            //VideoController.mEnableHWEncoder = false;
            if (VideoController.isHardwareEncoderEnabled()) {
                mVideoMonitor = new VideoMonitorHW();
            } else {
                mVideoMonitor = new VideoMonitorSF();
            }
            mVideoMonitor.start(this, mPeerId);

            VideoController.getInstance().acceptRequest(mPeerId);
        } else {
            this.stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.unregisterReceiver(mBroadcastHandler);
        mVideoMonitor.setVideoConnected(false);
        mVideoMonitor.stop();
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess();
        }
    }

    private BroadcastReceiver mBroadcastHandler = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(VideoConstants.ACTION_STOP_VIDEO_CHAT)) {
                Log.d(TAG, "recv broadcast : AVSessionClose");
                mVideoMonitor.setVideoConnected(false);
                BooyueVideoMonitorService.this.stopSelf();
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_CHANNEL_READY)) {
                mVideoMonitor.setVideoConnected(true);
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_NETMONIOTR_INFO)) {
                String msg = intent.getStringExtra("msg");
                Log.d(TAG, "recv broadcast : video info \r\n" + msg);
            } else if (intent.getAction().equalsIgnoreCase(VideoController.ACTION_VIDEO_QOS_NOTIFY)) {
                int width = intent.getIntExtra("width", 0);
                int height = intent.getIntExtra("height", 0);
                int bitrate = intent.getIntExtra("bitrate", 0) * 1000;
                int fps = intent.getIntExtra("fps", 0);
                if (width != 0 && height != 0 && bitrate != 0 && fps != 0) {
                    mVideoMonitor.resetEncoder(width, height, bitrate, fps);
                }
            } else if (intent.getAction() == TXDeviceService.BinderListChange) {
                boolean bFind = false;
                Parcelable[] listBinder = intent.getExtras().getParcelableArray("binderlist");
                for (int i = 0; i < listBinder.length; ++i) {
                    TXBinderInfo binder = (TXBinderInfo) (listBinder[i]);
                    if (binder.tinyid == Long.parseLong(mPeerId)) {
                        bFind = true;
                        break;
                    }
                }
                if (bFind == false) {
                    mVideoMonitor.setVideoConnected(false);
                    BooyueVideoMonitorService.this.stopSelf();
                }
            } else if (intent.getAction() == TXDeviceService.OnEraseAllBinders) {
                mVideoMonitor.setVideoConnected(false);
                BooyueVideoMonitorService.this.stopSelf();
            }
        }
    };

    public static interface IVideoMonitor {
        public void start(Service service, String peerId);

        public void stop();

        public void resetEncoder(int width, int height, int bitrate, int fps);

        public void setVideoConnected(boolean videoConnected);
    }

}
