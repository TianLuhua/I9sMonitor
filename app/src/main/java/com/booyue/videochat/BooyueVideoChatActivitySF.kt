package com.booyue.videochat

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import com.booyue.base.BaseActivity
import com.booyue.monitor.R
import com.tencent.av.VideoController
import com.tencent.av.camera.VcCamera
import com.tencent.av.core.VideoConstants
import com.tencent.av.opengl.GLVideoView
import com.tencent.av.opengl.GraphicRenderMgr
import com.tencent.av.opengl.ui.GLView
import com.tencent.av.opengl.ui.GLViewGroup
import com.tencent.device.QLog
import com.tencent.device.TXBinderInfo
import com.tencent.device.TXDeviceService
import com.tencent.util.LoggerUtils
import com.tencent.util.TimeUtils
import kotlinx.android.synthetic.main.activity_videochat_softcodec_booyue.*


/**
 * Created by Tianluhua on 2018\10\30 0030.
 */
class BooyueVideoChatActivitySF : BaseActivity() {


    companion object {
        val TAG = "BooyueVideoChatActivity"
        val SYSTEM_DIALOG_REASON_KEY = "reason"
        val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"
    }

    private lateinit var mPeerId: String
    private var mDinType = 0
    private lateinit var mSelfDin: String
    private var mIsReceiver = false
    private var mVideoConnected = false
    private var mSwitchVideoIndex = 0.toLong()
    private var mSwitchCameraIndex = 0.toLong()

    private lateinit var mGlpanelView: GLViewGroup
    private lateinit var mGlSmallVideoView: GLVideoView
    private lateinit var mGlBigVideoView: GLVideoView

    private lateinit var mCamera: VcCamera

    private var currentDefinition = 1
    private var startTime: Long = 0
    private val mHandler = Handler()
    private lateinit var mBroadcastHandler: BroadcastHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mIsReceiver) {
            beforeReceive()
        } else {
            afterReceive()
            tv_time.setVisibility(View.GONE)
            if (java.lang.Long.parseLong(mPeerId) != 0L) {
                VideoController.getInstance().request(mPeerId, mDinType)
            }
        }

        VideoController.getInstance().enableQosNotify(true)

        //startRing必须和stopRing成对调用（使用MediaPlayer）； startRing2必须和stopRing2成对调用（使用SoundPool）；
        if (mIsReceiver) {
            VideoController.getInstance().startRing(R.raw.qav_video_incoming, -1, null)
        } else {
            VideoController.getInstance().startRing(R.raw.qav_video_request, -1, null)
        }
    }

    override fun setView() {
        setContentView(R.layout.activity_videochat_softcodec_booyue)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val intent = getIntent()
        if (intent == null) {
            LoggerUtils.e(TAG + " Intent is null")
        }
        mPeerId = intent.getStringExtra("peerid")
        mDinType = intent.getIntExtra("dinType", VideoController.UINTYPE_QQ)
        mSelfDin = VideoController.getInstance().GetSelfDin()
        mIsReceiver = intent.getBooleanExtra("receive", false)
        if (mPeerId.toLong() == 0.toLong() || mSelfDin.toLong() == 0.toLong()) {
            LoggerUtils.e(TAG + " invalid peerId: " + mPeerId + " invalid selfDin: " + mSelfDin)
            finish()
        }
        mCamera = VideoController.getInstance().camera

        initQQGlView()
        initCameraPreview()
    }

    private fun initCameraPreview() {
        val holder = av_video_surfaceView.getHolder()
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                if (holder!!.getSurface() == null) {
                    return
                }
                holder.setFixedSize(width, height);
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                locateCameraPreview()
                layoutGlVideoView()
                VideoController.getInstance().execute(openCamera, null)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
            }
        })
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        av_video_surfaceView.setZOrderMediaOverlay(true)

    }

    // 这里对大小两个画面进行排布
    fun layoutGlVideoView() {
        val width = av_video_gl_root_view.getWidth()
        val height = av_video_gl_root_view.getHeight()

        //layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
        //        mGlBigVideoView.layout(0, 0, width / 2, width / 2);
        mGlBigVideoView.layout(0, 0, width, height)
        mGlBigVideoView.invalidate()

        //layout的四个参数定义： public void layout(int left, int top, int right, int bottom)
        //        mGlSmallVideoView.layout(width / 2, 0, width, width / 2);
        mGlSmallVideoView.layout(16, 15, 238, 156)
        mGlSmallVideoView.invalidate()
    }

    private fun locateCameraPreview() {
        val params = av_video_surfaceView.layoutParams as ViewGroup.MarginLayoutParams
        params.leftMargin = resources.getDimension(R.dimen.dimen__125).toInt()
        av_video_surfaceView.layoutParams = params
    }


    private fun initQQGlView() {
        mGlpanelView = GLViewGroup(this)
        av_video_gl_root_view.setContentPane(mGlpanelView)

        //在这里设置背景
        mGlpanelView.backgroundColor = Color.DKGRAY

        mGlBigVideoView = GLVideoView(this)
        mGlpanelView.addView(mGlBigVideoView)
        mGlBigVideoView.setIsPC(false)
        mGlBigVideoView.enableLoading(false)
        mGlBigVideoView.isMirror = true
        mGlBigVideoView.setNeedRenderVideo(true)
        mGlBigVideoView.visibility = GLView.VISIBLE
        mGlBigVideoView.scaleType = ImageView.ScaleType.FIT_CENTER// 可以避免裁剪导致的花屏问题。建议显示手Q传过来的视频的控件用这个
        mGlBigVideoView.setOnTouchListener(object : GLView.OnTouchListener {
            override fun onTouch(p0: GLView?, p1: MotionEvent?): Boolean {
                return true
            }
        })
        mGlBigVideoView.setBackground(R.drawable.bg)
        mGlSmallVideoView = GLVideoView(this)
        mGlpanelView.addView(mGlSmallVideoView)
        mGlSmallVideoView.setIsPC(false)
        mGlSmallVideoView.enableLoading(false)
        mGlSmallVideoView.isMirror = true
        mGlSmallVideoView.setNeedRenderVideo(true)
        mGlSmallVideoView.visibility = GLView.VISIBLE
        mGlSmallVideoView.scaleType = ImageView.ScaleType.CENTER_CROP
        mGlSmallVideoView.setBackground(R.drawable.bg)
        mGlSmallVideoView.setOnTouchListener(object : GLView.OnTouchListener {
            override fun onTouch(p0: GLView?, p1: MotionEvent?): Boolean {
                when (p1!!.action) {
                    MotionEvent.ACTION_UP -> switchVideo()
                }
                return true
            }

        })
        //设置边框颜色和边框宽度，不需要可以注释下面这两行代码
//        mGlSmallVideoView.setPaddingColor(Color.WHITE);
//        mGlSmallVideoView.setPaddings(2, 2, 2, 2);
        GraphicRenderMgr.getInstance().setGlRender(mSelfDin, mGlSmallVideoView.getYuvTexture())
        GraphicRenderMgr.getInstance().setGlRender(mPeerId, mGlBigVideoView.getYuvTexture())
    }

    private fun switchVideo() {
        mSwitchVideoIndex++
        val key1 = mPeerId
        val key2 = mSelfDin

        if (mSwitchVideoIndex % 2 == 0L) {
            //mGlBigVideoView 显示对方 mGlSmallVideoView显示自己
            GraphicRenderMgr.getInstance().setGlRender(key1, mGlBigVideoView.yuvTexture)
            GraphicRenderMgr.getInstance().setGlRender(key2, mGlSmallVideoView.yuvTexture)
            //显示mpeerId的videoview显示出来
            mGlBigVideoView.visibility = GLView.VISIBLE

        } else {
            //mGlBigVideoView 显示自己 mGlSmallVideoView显示对方
            GraphicRenderMgr.getInstance().setGlRender(key2, mGlBigVideoView.yuvTexture)
            GraphicRenderMgr.getInstance().setGlRender(key1, mGlSmallVideoView.yuvTexture)
            //显示mpeerId的videoview显示出来
            mGlSmallVideoView.visibility = GLView.VISIBLE
        }
    }

    override fun initView() {
        val friendInfo = VideoController.getInstance().getFriendInfo(mPeerId)
        tv_name.setText(friendInfo.name)


        btn_receive.setOnClickListener({
            VideoController.getInstance().stopRing()
            if (java.lang.Long.parseLong(mPeerId) != 0L) {
                VideoController.getInstance().acceptRequest(mPeerId)
                afterReceive()
            }
        })

        btn_refuse.setOnClickListener({
            finish()
        })

        ib_speaker_switcher.setOnClickListener({
            if (currentDefinition == 1) {
                currentDefinition = VideoController.DEFINITION_TYPE_CLEAR
                ib_speaker_switcher.setImageResource(R.drawable.button_smooth)
            } else {
                currentDefinition = VideoController.DEFINITION_TYPE_FLUENT
                ib_speaker_switcher.setImageResource(R.drawable.button_hd)
            }
            Toast.makeText(this@BooyueVideoChatActivitySF, R.string.switching, Toast.LENGTH_SHORT).show()
            VideoController.getInstance().setVideoModeType(java.lang.Long.valueOf(mPeerId), currentDefinition)// 切换时间为0-2s
            mHandler.postDelayed({ ib_speaker_switcher.setEnabled(true) }, 2000)
        })


        ib_switch_camera.setOnClickListener({
            mSwitchCameraIndex++
            if (mSwitchCameraIndex % 2 == 0L) {//打开摄像头，对方和自己画面都显示
                VideoController.getInstance().execute(openCamera, null)
                mGlSmallVideoView.visibility = GLView.VISIBLE
                mGlBigVideoView.visibility = GLView.VISIBLE
                ib_switch_camera.setImageResource(R.drawable.button_open_video)
            } else {//关闭摄像头，根据条件是来显示大画面还是显示小画面
                VideoController.getInstance().execute(closeCamera, null)
                if (mSwitchVideoIndex % 2 == 0L) {
                    mGlSmallVideoView.visibility = GLView.INVISIBLE
                    mGlBigVideoView.visibility = GLView.VISIBLE
                } else {
                    mGlBigVideoView.visibility = GLView.INVISIBLE
                    mGlSmallVideoView.visibility = GLView.VISIBLE
                }
                ib_switch_camera.setImageResource(R.drawable.button_close_the_video)
            }
        })
        ib_switch_voice.setOnClickListener({
            if (VideoController.getInstance().isSelfMute) {
                VideoController.getInstance().setSelfMute2(false)
                ib_switch_voice.setImageResource(R.drawable.button_speaker)
            } else {
                VideoController.getInstance().setSelfMute2(true)
                ib_switch_voice.setImageResource(R.drawable.button_mute)
            }
        })
        ib_hangup.setOnClickListener({
            finish()
        })
        beforeReceive()
    }


    /**
     * 接听之后的视图
     */
    private fun beforeReceive() {
        ll_receive_after.setVisibility(View.GONE)
        tv_time.setVisibility(View.GONE)

    }

    /**
     * 接听之后的视图
     */
    private fun afterReceive() {
        ll_recieve_before.setVisibility(View.GONE)
        tv_time.setVisibility(View.VISIBLE)
        ll_receive_after.setVisibility(View.VISIBLE)
    }


    var openCamera: Runnable = Runnable {
        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "resumeCamera begin.")
        }
        val holder = av_video_surfaceView.holder

        VideoController.getInstance().execute(AsyncOpenCamera(holder))

        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "resumeCamera end.")
        }
    }

    inner class AsyncOpenCamera(var mHolder: SurfaceHolder) : Runnable {

        override fun run() {
            try {
                if (QLog.isColorLevel()) {
                    QLog.d(TAG, QLog.CLR, "asyncOpenCamera start.")
                }
                if (mCamera == null || !mCamera.openCamera(mHolder)) {
                    LoggerUtils.d(TAG, "asyncOpenCamera failed to start camera.")
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera failed to start camera.")
                    }
                    return
                } else {
                    LoggerUtils.d(TAG, "asyncOpenCamera success.")
                    if (QLog.isColorLevel()) {
                        QLog.d(TAG, QLog.CLR, "asyncOpenCamera success.")
                    }
                }
                LoggerUtils.d(TAG, "asyncOpenCamera end ")
                if (QLog.isColorLevel()) {
                    QLog.d(TAG, QLog.CLR, "asyncOpenCamera end.")
                }
            } catch (e: Exception) {
                if (QLog.isColorLevel()) {
                    QLog.d(TAG, QLog.CLR, "asyncOpenCamera", e)
                }
            }

        }
    }

    var closeCamera: Runnable = Runnable {
        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "closeCamera begin.")
        }
        if (mCamera != null) {
            val flag = mCamera.closeCamera()
            LoggerUtils.d(TAG, "close camera successful : $flag")
        }
        if (QLog.isColorLevel()) {
            QLog.d(TAG, QLog.CLR, "closeCamera end.")
        }
    }

    override fun initData() {

    }

    override fun onResume() {
        super.onResume()
        initReceiver()
    }

    private fun initReceiver() {
        mBroadcastHandler = BroadcastHandler()
        val filter = IntentFilter()
        filter.addAction(VideoConstants.ACTION_STOP_VIDEO_CHAT)
        filter.addAction(VideoController.ACTION_NETMONIOTR_INFO)
        filter.addAction(VideoController.ACTION_VIDEO_QOS_NOTIFY)
        filter.addAction(VideoController.ACTION_CHANNEL_READY)
        filter.addAction(VideoController.ACTION_NET_LEVEL)
        filter.addAction(VideoController.ACTION_NET_BAD)
        filter.addAction(TXDeviceService.BinderListChange)
        filter.addAction(TXDeviceService.OnEraseAllBinders)
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        registerReceiver(mBroadcastHandler, filter)
    }

    override fun onPause() {
        super.onPause()
        terminateVideo()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                showQuitDialog()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun terminateVideo() {
        VideoController.getInstance().execute(closeCamera, null)
        VideoController.getInstance().stopRing()

        if (mIsReceiver) {
            if (mVideoConnected) {
                VideoController.getInstance().closeVideo(mPeerId)
            } else {
                VideoController.getInstance().rejectRequest(mPeerId)
            }
        } else {
            VideoController.getInstance().closeVideo(mPeerId)
        }
        GraphicRenderMgr.getInstance().setGlRender(mPeerId, null)
        GraphicRenderMgr.getInstance().setGlRender(mSelfDin, null)
        super.unregisterReceiver(mBroadcastHandler)
        if (TXDeviceService.VideoProcessEnable) {
            VideoController.getInstance().exitProcess()
        }
    }

    inner class BroadcastHandler : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }
            val action = intent.action
            when (action) {
                VideoConstants.ACTION_STOP_VIDEO_CHAT -> {

                    var reason = intent.getIntExtra("reason", VideoConstants.VOIP_REASON_OTHERS)

                    var reasonString: String = ""
                    when (reason) {
                        VideoConstants.VOIP_REASON_REJECT_BY_FRIEND -> {
                            //发起请求之后，对方拒绝
                            reasonString = context!!.getString(R.string.voip_reason_reject_by_friend)
                        }
                        VideoConstants.VOIP_REASON_SELF_WAIT_RELAYINFO_TIMEOUT -> {
                            //发起请求之后，对方一直不接听，最后超时
                            reasonString = context!!.getString(R.string.voip_reason_self_wait_relayinfo_timeout)
                        }
                        VideoConstants.VOIP_REASON_CLOSED_BY_FRIEND -> {
                            //连通之后，对方主动关闭
                            reasonString = context!!.getString(R.string.voip_reason_closed_by_friend)
                        }
                        else -> {
                            //其它原因
                            reasonString = context!!.getString(R.string.voip_reason_other)
                        }
                    }
                    Toast.makeText(context, reasonString, Toast.LENGTH_LONG).show()
                    finish()

                }
                VideoController.ACTION_NETMONIOTR_INFO -> {

                }
                VideoController.ACTION_CHANNEL_READY -> {
                    VideoController.getInstance().stopRing()
                    VideoController.getInstance().stopShake()
                    VideoController.getInstance().startShake()
                    mVideoConnected = true
                    startTime = System.currentTimeMillis()
                    mHandler.post(updateTime)

                }

                TXDeviceService.BinderListChange -> {
                    var bFind = false
                    val listBinder = intent.extras!!.getParcelableArray("binderlist")
                    for (i in listBinder!!.indices) {
                        val binder = listBinder[i] as TXBinderInfo
                        if (binder.tinyid == java.lang.Long.parseLong(mPeerId)) {
                            bFind = true
                            break
                        }
                    }
                    if (bFind == false) {
                        finish()
                    }
                }

                TXDeviceService.OnEraseAllBinders -> {
                    finish()
                }

                VideoController.ACTION_VIDEO_QOS_NOTIFY -> {

                }

                VideoController.ACTION_NET_LEVEL -> {
                    val tips = intent.getStringExtra("displayNetState")
                    if (tips != null) {
                        Toast.makeText(context, tips, Toast.LENGTH_SHORT).show()
                    }

                }

                VideoController.ACTION_NET_BAD -> {
                    if (currentDefinition == VideoController.DEFINITION_TYPE_CLEAR) {
                        Toast.makeText(context, R.string.network_poor, Toast.LENGTH_SHORT).show()
                    }
                }
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reasonKey = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
                    if (SYSTEM_DIALOG_REASON_HOME_KEY == reasonKey) {
                        LoggerUtils.d(TAG, "press home key: ")
                        ///按下Home键
                        finish()
                    }

                }
                else -> {
                    LoggerUtils.d(TAG, "BroadcastHandler else !! ")
                }

            }
        }

    }


    private fun showQuitDialog() {
        val builder = AlertDialog.Builder(this@BooyueVideoChatActivitySF)
        builder.setMessage("确认退出吗?")
        builder.setTitle("提示")
        builder.setPositiveButton("确认") { dialog, which ->
            dialog.dismiss()
            this@BooyueVideoChatActivitySF.finish()
        }
        builder.setNegativeButton("取消") { dialog, which -> dialog.dismiss() }
        builder.create().show()
    }

    /**
     * 按钮点击切换窗口
     *
     * @param view
     */
    fun onBtnSwitchVideo(view: View) {
        switchVideo()
    }

    var updateTime: Runnable = object : Runnable {
        override fun run() {
            if (tv_time.getVisibility() == View.GONE) {
                tv_time.setVisibility(View.VISIBLE)
            }
            tv_time.setText(TimeUtils.long2TimeFormat(startTime))
            mHandler.postDelayed(this, 1000)
        }
    }
}