package com.tencent.devicedemo;

import com.tencent.device.TXDeviceService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.booyue.monitor.R;

public class WifiDecodeActivity extends Activity {
    private static final int samplerate = 44100;
    private static final int channel = AudioFormat.CHANNEL_IN_MONO;
    private static final int format = AudioFormat.ENCODING_PCM_16BIT;


    private TextView mwifiinfo;
    private NotifyReceiver mNotifyReceiver;

    private int bufferSizeInBytes = 0;

    private AudioRecord audioRecord;

    private int isRecording = 0;

    Handler mHandler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated constructor stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifidecode);

        mwifiinfo = (TextView) findViewById(R.id.wifiinfo);
        IntentFilter filter = new IntentFilter();
        filter.addAction(TXDeviceService.OnReceiveWifiInfo);
        mNotifyReceiver = new NotifyReceiver();
        registerReceiver(mNotifyReceiver, filter);

        TXDeviceService.startWifiDecoder("TXTEST-axewang-7", samplerate, 3);
        // 创建audiorecoder

        createAudioRecord();
        startRecord();
    }

    private void createAudioRecord() {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(samplerate, channel, format);
        if (bufferSizeInBytes % (441 * 2 * 2) == 0) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplerate, channel, format, bufferSizeInBytes);

        } else {
            bufferSizeInBytes = (bufferSizeInBytes / (441 * 2 * 2) + 2) * (441 * 2 * 2);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplerate, channel, format, bufferSizeInBytes);
        }
    }

    private void startRecord() {
        if (audioRecord != null) {
            audioRecord.startRecording();
            isRecording = 1;
            new Thread(new AudioRecordThread()).start();
        }
    }

    private void stopRecord() {
        if (audioRecord != null) {
            isRecording = 0;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    class AudioRecordThread implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            int readsize = 0;
            byte[] audiodata = new byte[bufferSizeInBytes];
            byte[] block = new byte[(441 * 2 * 2)];
            byte[] tail_block = null;
            while (isRecording == 1) {
                readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);

                if (readsize == bufferSizeInBytes) {
                    int i = 0;
                    while (readsize > (441 * 2 * 2)) //20ms
                    {
                        System.arraycopy(audiodata, i * (441 * 2 * 2), block, 0, (441 * 2 * 2));
                        TXDeviceService.fillVoiceWavData(block);
                        i = i + 1;
                        readsize = readsize - (441 * 2 * 2);
                    }

                    if (readsize > 0) {
                        tail_block = new byte[readsize];
                        System.arraycopy(audiodata, i * (441 * 2 * 2), tail_block, 0, readsize);
                        TXDeviceService.fillVoiceWavData(tail_block);
                        tail_block = null;
                    }
                } else {
                    Log.i("TAG_WifiDecode", "size error");
                }
            }
        }

    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        stopRecord();
        unregisterReceiver(mNotifyReceiver);
        super.onDestroy();
    }

    public class NotifyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TXDeviceService.OnReceiveWifiInfo)) {
                // show info
                Bundle bundle = intent.getExtras();
                String ssid = bundle.getString(TXDeviceService.WifiInfo_SSID);
                String pass = bundle.getString(TXDeviceService.WifiInfo_PASS);
                int ip = bundle.getInt(TXDeviceService.WifiInfo_IP);
                int port = bundle.getInt(TXDeviceService.WifiInfo_PORT);
                String info = "ssid:" + ssid + "\npassword:" + pass + "\nip:" + ip + "\nport:" + port;

                mwifiinfo.setText(info);

                stopRecord();

                TXDeviceService.stopWifiDecoder();

                WifiDecodeActivity.this.getSharedPreferences("TXDeviceSDK", 0).edit().putBoolean("NetworkSetted", true);

//				mHandler.postDelayed(new Runnable(){
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//						finish();
//					}
//				}, 5000);

            } else {

            }
        }
    }

}