package com.tencent.devicedemo;

import android.content.Context;
import android.content.Intent;

import com.tencent.device.TXBinderInfo;

import java.util.ArrayList;
import java.util.List;

public class BinderListAdapter extends ListAdapter {
	private static String TAG = "BinderListAdapter";
	private List<TXBinderInfo> mListBinder = new ArrayList<>();
	private Context mContext;
	
	public BinderListAdapter(Context applicationContext) {
		// TODO Auto-generated constructor stub
		super(applicationContext);
		mContext = applicationContext;
	}
	
	public void freshBinderList(List<TXBinderInfo> binderList){
		mListBinder.clear();
		for (int i = 0; i < binderList.size(); ++i){
			TXBinderInfo  binder = binderList.get(i);
			mListBinder.add(binder);
		}
		notifyDataSetChanged();
	}
	
	@Override
	public ListItemInfo getListItemInfo(int index) {
		ListItemInfo item = new ListItemInfo();
		TXBinderInfo binder = mListBinder.get(index);
		item.id = binder.tinyid;
		item.head_url = binder.head_url;
		item.type = ListItemInfo.LISTITEM_TYPE_BINDER;
		item.nick_name = binder.getNickName();
		return item;
	}
	
	@Override
	public void onItemClicked(int index) {
		ListItemInfo item = getListItemInfo(index);
		Intent binder = new Intent(mContext, BinderActivity.class);
		binder.putExtra("tinyid", item.id);
		binder.putExtra("nickname", item.nick_name);
		binder.putExtra("type", item.type);
        mContext.startActivity(binder);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mListBinder.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mListBinder.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}
	
	
}
