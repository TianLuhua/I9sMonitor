package com.booyue.ui.friend.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.booyue.MonitorApplication
import com.booyue.ui.binding.BooyueGuideActivity
import com.booyue.monitor.R
import com.booyue.widget.CircleImageView
import com.tencent.av.VideoController
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDataPoint
import com.tencent.device.TXDeviceService
import com.tencent.devicedemo.ListItemInfo
import com.tencent.devicedemo.ListItemInfo.*
import com.tencent.devicedemo.MainActivity
import com.tencent.util.NetWorkUtils
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Created by Tianluhua on 2018\11\1 0001.
 */
class BooyueFriendListAdapter(private var mContext: Context) : RecyclerView.Adapter<BooyueFriendListAdapter.MyViewHolder>() {

    companion object {
        val TAG = "BooyueFriendListAdapter"
    }

    private var mHeadPicPath: String
    private var mHandler: Handler
    private var layoutInflater: LayoutInflater
    private val mSetFetching = HashSet<Long>()
    private val mListBinder = ArrayList<TXBinderInfo>()

    init {
        this.layoutInflater = LayoutInflater.from(mContext)
        this.mHeadPicPath = mContext.cacheDir.absolutePath + "/head"
        val file = File(mHeadPicPath)
        if (!file.exists()) {
            file.mkdirs()
        }
        mHandler = object : Handler(Looper.myLooper()) {
            override fun handleMessage(msg: Message?) {
                this@BooyueFriendListAdapter.notifyDataSetChanged()
            }
        }
    }

    fun freshBinderList(binderList: List<TXBinderInfo>) {
        mListBinder.clear()
        binderList.forEach {
            mListBinder.add(it)
        }
        //增加一个加好友项
        val addItem = TXBinderInfo()
        addItem.nick_name = ByteArray(0)
        addItem.head_url = ""
        addItem.tinyid = 0
        addItem.binder_type = LISTITEM_TYPE_ADD_FRIEND
        mListBinder.add(addItem)
        notifyDataSetChanged()
    }


    private fun getListItemInfo(index: Int): ListItemInfo {
        val item = ListItemInfo()
        val binder = mListBinder[index]
        item.id = binder.tinyid
        item.head_url = binder.head_url
        //        item.type = ListItemInfo.LISTITEM_TYPE_BINDER;
        item.nick_name = binder.nickName

        if (index == mListBinder.size - 1) {
            item.type = LISTITEM_TYPE_ADD_FRIEND  //列表最后一个是添加好友项
        } else {
            item.type = LISTITEM_TYPE_BINDER //其余的是设备好友
        }
        return item
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = layoutInflater.inflate(R.layout.friendlist_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (mListBinder.size > 0) mListBinder.size else 0
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getListItemInfo(position)
        if (item.type == LISTITEM_TYPE_ADD_FRIEND) {
            holder.ivAvatar.setImageResource(R.drawable.add_more)
            holder.tvName.setText(R.string.add_friend)
            holder.llFunction.visibility = View.GONE
        } else {

            holder.tvName.text = item.nick_name
            holder.llFunction.visibility = View.VISIBLE
            var bitmap = getBinderHeadPic(item.id, item.type)
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(mContext.resources, R.drawable.head_portrait_default)
                holder.ivAvatar.setImageBitmap(bitmap)
                if (item.head_url != null && item.head_url.isNotEmpty()) {
                    fetchBinderHeadPic(item.id, item.head_url)
                }
            } else {
                holder.ivAvatar.setImageBitmap(bitmap)
            }

            holder.ibPhone.setOnClickListener {
                if (NetWorkUtils.isNetWorkAvailable(mContext)) {
                    if (!VideoController.getInstance().hasPendingChannel()) {
                        Toast.makeText(MonitorApplication.getContext(), R.string.launching, Toast.LENGTH_SHORT).show()
                        TXDeviceService.getInstance().startAudioChatActivity(item.id, VideoController.UINTYPE_QQ)
                        (mContext as Activity).overridePendingTransition(R.anim.dialog_enter_anim_scale, R.anim.dialog_exit_anim_scale)
                    } else {
                        Toast.makeText(MonitorApplication.getContext(), R.string.being_video, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(MonitorApplication.getContext(), R.string.network_close, Toast.LENGTH_SHORT).show()
                }
            }
            holder.ibVideo.setOnClickListener {
                if (MainActivity.isNetworkAvailable(mContext)) {
                    if (false == VideoController.getInstance().hasPendingChannel()) {
                        Toast.makeText(MonitorApplication.getContext(), R.string.launching, Toast.LENGTH_SHORT).show()
                        TXDeviceService.getInstance().startVideoChatActivity(item.id, VideoController.UINTYPE_QQ)
                        (mContext as Activity).overridePendingTransition(R.anim.dialog_enter_anim_scale, R.anim.dialog_exit_anim_scale)
                    } else {
                        Toast.makeText(MonitorApplication.getContext(), R.string.being_video, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(MonitorApplication.getContext(), R.string.network_close, Toast.LENGTH_SHORT).show()
                }
            }
        }
        holder.ivAvatar.setOnClickListener {
            val item = getListItemInfo(position)
            if (item.type == LISTITEM_TYPE_BINDER) {

                val dataPoint = TXDataPoint()
                dataPoint.property_id = 100001L
                dataPoint.property_val = "当前电量值：100"
                TXDeviceService.reportDataPoint(arrayOf(dataPoint))

                //external fun sendNotifyMsg(digest: String, msgId: Int, targetIds: LongArray).
                TXDeviceService.sendNotifyMsg("NotifyMsg From Device", 100001, longArrayOf(item.id))
                Toast.makeText(mContext, "sendNotifyMsg", Toast.LENGTH_SHORT).show()

                //public static native long sendTextMsg(String text, int msgId, long[] targetIds);
                TXDeviceService.sendTextMsg("TextMsg From Device", 100001, longArrayOf(item.id))
                Toast.makeText(mContext, "sendTextMsg", Toast.LENGTH_SHORT).show()

                // 发送模版消息
                // external fun sendTemplateMsg(json: String): Long TemplateMsg From Device
                TXDeviceService.sendTemplateMsg("{\" msg \":\" TextMsg From Device \"}")
                Toast.makeText(mContext, "sendTemplateMsg", Toast.LENGTH_SHORT).show()

                //                    Intent binder = new Intent(mContext, BinderActivity.class);
                //                    binder.putExtra("tinyid", item.id);
                //                    binder.putExtra("nickname", item.nick_name);
                //                    binder.putExtra("type", item.type);
                //                    mContext.startActivity(binder);
                //                    ((Activity)mContext).overridePendingTransition(R.anim.slide_right_in,R.anim.slide_left_out);
            } else {
                val binder = Intent(mContext, BooyueGuideActivity::class.java)
                mContext.startActivity(binder)
            }

            if (mItemClickListener != null) {
                mItemClickListener!!.onItemClickListener(position)
            }
        }
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var ivAvatar: CircleImageView
        var tvName: TextView
        var ibPhone: ImageButton
        var ibVideo: ImageButton
        var llFunction: LinearLayout

        init {
            ivAvatar = itemView.findViewById(R.id.iv_avatar) as CircleImageView
            tvName = itemView.findViewById(R.id.tv_name) as TextView
            ibPhone = itemView.findViewById(R.id.ib_phone) as ImageButton
            ibVideo = itemView.findViewById(R.id.ib_video) as ImageButton
            llFunction = itemView.findViewById(R.id.ll_function) as LinearLayout
        }
    }


    private var mItemClickListener: ItemClickListener? = null

    /**
     * 点击事件回调
     */
    interface ItemClickListener {
        fun onItemClickListener(index: Int)
    }

    /**
     * 点击事件注入
     */
    fun setOnItemClickListener(mItemClickListener: ItemClickListener) {
        this.mItemClickListener = mItemClickListener
    }

    /**
     * 获取头像
     *
     * @param uin
     * @param type
     * @return
     */
    fun getBinderHeadPic(uin: Long, type: Int): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            if (LISTITEM_TYPE_ADD_FRIEND == type) {
                bitmap = BitmapFactory.decodeResource(mContext.resources, R.drawable.add_more)
            } else {
                val strHeadPic = "$mHeadPicPath/$uin.png"
                bitmap = BitmapFactory.decodeFile(strHeadPic)
            }
        } catch (e: Exception) {
            Log.i(TAG, e.toString())
        }

        return bitmap
    }

    /**
     * 保存头像
     *
     * @param uin
     * @param bitmap
     */
    fun saveBinderHeadPic(uin: Long, bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        val strHeadPic = "$mHeadPicPath/$uin.png"
        val file = File(strHeadPic)
        if (file.exists()) {
            file.delete()
        }
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
        } catch (e: Exception) {
            Log.i(TAG, e.toString())
        }

    }


    /**
     * 从网络获取头像
     *
     * @param uin    标识码
     * @param strUrl 头像url
     */
    private fun fetchBinderHeadPic(uin: Long, strUrl: String) {
        synchronized(mSetFetching) {
            if (mSetFetching.contains(uin)) {
                return
            } else {
                mSetFetching.add(uin)
            }
        }
        object : Thread() {
            override fun run() {
                try {
                    val url = URL(strUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true
                    conn.connect()
                    val stream = conn.inputStream
                    val bitmap = BitmapFactory.decodeStream(stream)
                    saveBinderHeadPic(uin, bitmap)
                    synchronized(mSetFetching) {
                        mSetFetching.remove(uin)
                    }
                    mHandler.sendEmptyMessage(0)
                } catch (e: Exception) {
                    Log.i(TAG, e.toString())
                }

            }
        }.start()
    }

}