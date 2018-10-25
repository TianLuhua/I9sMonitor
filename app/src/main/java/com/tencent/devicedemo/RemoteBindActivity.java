package com.tencent.devicedemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import com.booyue.monitor.R;

import static com.tencent.devicedemo.CameraListAdapter.getMd5;

/**
 * Created by lingyuhuang on 2016/9/14.
 */

public class RemoteBindActivity extends Activity {

    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        mEditText = (EditText) findViewById(R.id.et);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = mEditText.getText().toString();
                if (number.length() != 11) {
                    Toast.makeText(RemoteBindActivity.this, "输入正确手机号", Toast.LENGTH_SHORT).show();
                    return;
                }
                String json = " { \"pid\":\"1700004123\", \"din\":\"\", \"sn\":\"2CF28B0271484593\", \"lisence\": \"304502205A8C8BAFE7A95273A702C996B18E6412C34AE110798CD14F9A5140CA5CB82254022100F73F1B9D3320A33E4D3C1F62BD1C220A150F5DC78E544BCA665E26F942CD882B\",\"md5sum\":\"" + getMd5("304502205A8C8BAFE7A95273A702C996B18E6412C34AE110798CD14F9A5140CA5CB82254022100F73F1B9D3320A33E4D3C1F62BD1C220A150F5DC78E544BCA665E26F942CD882B") + "\",\"dstPhoneNum\":\"" + number + "\",\"isVerify\": \"0\",\"Sig\": \"\",\"verify\":\"\"}";
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "https://yun.tim.qq.com/v4/SmartDeviceRemoteBind/test?apn=0", json);
            }
        });
    }

    AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-type", "application/x-java-serialized-object");
                conn.setRequestMethod("POST");
                OutputStream out = conn.getOutputStream();
                OutputStreamWriter osr = new OutputStreamWriter(out, "ISO-8859-1");
                BufferedWriter writer = new BufferedWriter(osr);
                writer.write(params[1]);
                writer.flush();
                writer.close();

                if (conn.getResponseCode() != 200) {
                    return null;
                }

                InputStream in = conn.getInputStream();
                String result = streamToString(in);
                return result;
            } catch (SocketTimeoutException e) {
                return null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast.makeText(RemoteBindActivity.this, "短信已发送", Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    public static String streamToString(InputStream input) throws IOException {

        InputStreamReader isr = new InputStreamReader(input, "ISO-8859-1");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        isr.close();
        return sb.toString();
    }
}
