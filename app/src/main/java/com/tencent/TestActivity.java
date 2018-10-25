package com.tencent;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.booyue.monitor.R;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        final EditText etSerial = (EditText) findViewById(R.id.et_serial);
        Button btnStart = (Button) findViewById(R.id.start_activity);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serial = etSerial.getText().toString().trim();
                if(TextUtils.isEmpty(serial)){
                    Toast.makeText(TestActivity.this, TestActivity.this.getString(R.string.content_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                SerialNumberManager.spilitSerailNumber(serial);
                startActivity(new Intent(TestActivity.this,BooyueFriendListActivity.class));
            }
        });
    }
}
