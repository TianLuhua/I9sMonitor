package com.tencent.devicedemo;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.tencent.device.TXDeviceService;
import com.tencent.device.TXFriendInfo;

import java.util.ArrayList;
import java.util.List;

public class FriendListAdapter extends ListAdapter {
	private static String TAG = "BooyueFriendListAdapter";
	private List<TXFriendInfo> mListFriend = new ArrayList<>();
	private Context mContext;
	
	public FriendListAdapter(Context applicationContext) {
		// TODO Auto-generated constructor stub
		super(applicationContext);
		mContext = applicationContext;
	}
	
	public void freshFriendList(List<TXFriendInfo> friendList){
		mListFriend.clear();
		for (int i = 0; i < friendList.size(); ++i){
			TXFriendInfo  friendinfo = friendList.get(i);
			mListFriend.add(friendinfo);
		}
		//增加一个加好友项
		TXFriendInfo addItem = new TXFriendInfo();
		addItem.device_name = new byte[0];
		addItem.admin_remark = new byte[0];
		addItem.friend_din = 0;
		mListFriend.add(addItem);
		notifyDataSetChanged();

	}
	
	@Override
	public ListItemInfo getListItemInfo(int index) {
		ListItemInfo item = new ListItemInfo();
		TXFriendInfo friendinfo = mListFriend.get(index);
		item.id = friendinfo.friend_din;
		item.head_url = friendinfo.head_url;
		if (index == mListFriend.size() - 1) {
			item.type = ListItemInfo.LISTITEM_TYPE_ADD_FRIEND;  //列表最后一个是添加好友项
		} else {
			item.type = ListItemInfo.LISTITEM_TYPE_FRIEND; //其余的是设备好友
		}
		item.nick_name = friendinfo.getDeviceName();
		return item;
	}
	
	@Override
	public void onItemClicked(int index) {
		ListItemInfo item = getListItemInfo(index);
		if (ListItemInfo.LISTITEM_TYPE_FRIEND == item.type) {    //点击设备好友图标
			Intent binder = new Intent(mContext, BinderActivity.class);
			binder.putExtra("tinyid", item.id);
			binder.putExtra("nickname", item.nick_name);
			binder.putExtra("type", item.type);
	        mContext.startActivity(binder);
		} else {           //点击添加好友图标
			// 如果没被绑定就提示用户需要先绑定
			if (TXDeviceService.mBinderList.size() == 0) {
				Toast.makeText(mContext, "需要先绑定设备才能加好友", Toast.LENGTH_SHORT).show();
			} else {
				Intent intent = new Intent(mContext, AddFriendActivity.class);
				mContext.startActivity(intent);
			}
		}
		
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mListFriend.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mListFriend.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}


}
