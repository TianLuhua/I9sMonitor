package com.tencent.devicedemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.booyue.monitor.R;

public class AddFriendFinishActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_addfriend_finish);
	}
	
	public void back(View v) {
		finish();
	}

}
