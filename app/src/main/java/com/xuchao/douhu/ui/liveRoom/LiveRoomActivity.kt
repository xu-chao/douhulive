package com.xuchao.douhu.ui.liveRoom

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.gson.internal.LinkedTreeMap
import com.xuchao.douhu.R
import com.xuchao.douhu.SunnyWeatherApplication.Companion.context
import com.xuchao.douhu.logic.model.RoomInfo
import kotlinx.android.synthetic.main.activity_liveroom.*
import xyz.doikki.videocontroller.StandardVideoController
import xyz.doikki.videocontroller.component.*
import xyz.doikki.videoplayer.exo.ExoMediaPlayer
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.player.VideoViewManager
import xyz.doikki.videoplayer.util.PlayerUtils
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xuchao.douhu.logic.model.DanmuSetting
import com.xuchao.douhu.util.dkplayer.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xuchao.douhu.SunnyWeatherApplication
import com.xuchao.douhu.ui.login.LoginActivity
import java.lang.Exception
import android.view.WindowManager

import android.app.Activity
import android.net.Uri
import android.view.Window
import androidx.preference.PreferenceManager
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.Utils
import com.efs.sdk.base.newsharedpreferences.SharedPreferencesUtils

import com.hjq.permissions.XXPermissions

import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.xuchao.douhu.logic.service.ForegroundService

class LiveRoomActivity : AppCompatActivity(), Utils.OnAppStatusChangedListener, YJLiveControlView.OnRateSwitchListener, DanmuSettingFragment.OnDanmuSettingChangedListener {
    private val viewModel by lazy { ViewModelProvider(this).get(LiveRoomViewModel::class.java) }
    private var mDefinitionControlView: YJLiveControlView? = null
    private lateinit var adapter: LiveRoomAdapterNew
    private lateinit var mPIPManager: PIPManager
    private var danmuShow = true
    private var controller: YJstandardController? = null
    private var videoView: VideoView<ExoMediaPlayer>? = null
    private lateinit var mMyDanmakuView: MyDanmakuView
    private lateinit var danmuSetting: DanmuSetting
    private lateinit var sharedPref: SharedPreferences
    private var toBottom = true
    private var updateList = true

    private var isFollowed = false
    private var platform = ""
    private var roomId = ""
    private var isFirstGetInfo = true
    private val definitionArray = arrayOf("??????", "??????", "??????", "??????", "??????")
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun startFullScreen() {
        updateList = false
        if (danmuSetting.isShow) {
            mMyDanmakuView.show()
        }
    }

    fun stopFullScreen() {
        adapter.setList(viewModel.danmuList)
        danMu_recyclerView.scrollToPosition(adapter.itemCount-1)
        mMyDanmakuView.hide()
        toBottom = true
        updateList = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var theme = sharedPreferences.getInt("theme", R.style.SunnyWeather)
        setTheme(theme)
        setContentView(R.layout.activity_liveroom)
        val playBackGround = sharedPreferences.getBoolean("play_background", false)
        val backTiny = sharedPreferences.getBoolean("tiny_when_back", false)
        if (playBackGround || backTiny) {
            AppUtils.registerAppStatusChangedListener(this)
        }

        //????????????????????????????????????
        val linearLayoutManager: LinearLayoutManager = object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
            override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
                val smoothScroller: LinearSmoothScroller =
                    object : LinearSmoothScroller(recyclerView.context) {
                        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                            // ???????????????1px??????????????????(ms)???
                            return 20f / displayMetrics.densityDpi
                        }
                        override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                            return boxStart - viewStart
                        }
                    }
                smoothScroller.targetPosition = position
                startSmoothScroll(smoothScroller)
            }
        }
        setStatusBarColor(this, R.color.black)
        //????????????????????????
        sharedPref = this.getSharedPreferences("JustLive", Context.MODE_PRIVATE)
        danmuSetting = getDanmuSetting()

        danMu_recyclerView.layoutManager = linearLayoutManager
        adapter = LiveRoomAdapterNew()
        danMu_recyclerView.adapter = adapter
        danMu_recyclerView.itemAnimator = null
        //????????????????????????
        to_bottom_danmu.setOnClickListener {
            danMu_recyclerView.scrollToPosition(adapter.itemCount-1)
            to_bottom_danmu.visibility = View.GONE
            toBottom = true
        }
        //????????????????????????????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            danMu_recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
                if (danMu_recyclerView.canScrollVertically(1)) {
                    toBottom = false
                    to_bottom_danmu.visibility = View.VISIBLE
                } else {
                    to_bottom_danmu.visibility = View.GONE
                    toBottom = true
                }
            }
        }
        //??????16:9?????????
        val lp = player_container.layoutParams
        val point = Point()
        this.windowManager.defaultDisplay.getRealSize(point)
        lp.height = point.x * 9 / 16
        player_container.layoutParams = lp

        controller = YJstandardController(this)
        val display = windowManager.defaultDisplay
        val refreshRate = display.refreshRate
        mMyDanmakuView = MyDanmakuView(this, danmuSetting, refreshRate)
        mMyDanmakuView.hide()
        addControlComponents(controller!!)
        controller!!.setDoubleTapTogglePlayEnabled(false)
        controller!!.setEnableInNormal(true)

        platform = intent.getStringExtra("platform")?:""
        roomId = intent.getStringExtra("roomId")?:""
        mPIPManager = PIPManager.getInstance(platform, roomId)
        mPIPManager.actClass = LiveRoomActivity::class.java
        var uid = ""
        if (SunnyWeatherApplication.userInfo != null) {
            uid = SunnyWeatherApplication.userInfo!!.uid
            viewModel.startDanmu(platform, roomId, SunnyWeatherApplication.userInfo!!.selectedContent, SunnyWeatherApplication.userInfo!!.isActived == "1")
        } else {
            viewModel.startDanmu(platform, roomId, "", false)
        }
        viewModel.getRoomInfo(uid, platform, roomId)



        //?????????
        to_web.setOnClickListener {
            toWeb(platform, roomId)
        }
        //????????????
        viewModel.danmuNum.observe(this, {
            if (viewModel.danmuList.size > 0){
                mMyDanmakuView.addDanmaku(viewModel.danmuList.last().content)
                if (updateList) {
                    adapter.addData(viewModel.danmuList.last())
                    if (toBottom) {
                        danMu_recyclerView.scrollToPosition(adapter.itemCount-1)
                    }
                }
            }
        })
        //????????????????????????
        viewModel.urlResponseData.observe(this, {result ->
            val urls : LinkedTreeMap<String, String> = result.getOrNull() as LinkedTreeMap<String, String>
            if (urls != null && urls.size > 0) {
                videoView?.setVideoController(controller) //???????????????
                var sharedPref = SharedPreferencesUtils.getSharedPreferences(context, "JustLive")

                when (sharedPref.getInt("playerSize", R.id.radio_button_1)) {
                    R.id.radio_button_1 -> {
                        changeVideoSize(VideoView.SCREEN_SCALE_DEFAULT)
                    }
                    R.id.radio_button_2 -> {
                        changeVideoSize(VideoView.SCREEN_SCALE_MATCH_PARENT)
                    }
                    R.id.radio_button_3 -> {
                        changeVideoSize(VideoView.SCREEN_SCALE_CENTER_CROP)
                    }
                }
                val isMobileData = NetworkUtils.isMobileData()
                if (isMobileData) {
                    Toast.makeText(context, "??????????????????", Toast.LENGTH_SHORT).show()
                    val defaultDefinition = sharedPreferences.getString("default_definition_4G", "??????")
                    if (urls.containsKey(defaultDefinition)) {
                        mDefinitionControlView?.setData(urls, defaultDefinition)
                        videoView?.setUrl(urls[defaultDefinition]) //??????????????????
                    } else {
                        for (item in definitionArray) {
                            if (urls.containsKey(item)) {
                                mDefinitionControlView?.setData(urls, item)
                                videoView?.setUrl(urls[item]) //??????????????????
                                break
                            }
                        }
                    }
                } else {
                    val defaultDefinition = sharedPreferences.getString("default_definition_wifi", "??????")

                    if (urls.containsKey(defaultDefinition)) {
                        mDefinitionControlView?.setData(urls, defaultDefinition)
                        videoView?.setUrl(urls[defaultDefinition]) //??????????????????
                    } else {
                        for (item in definitionArray) {
                            if (urls.containsKey(item)) {
                                mDefinitionControlView?.setData(urls, item)
                                videoView?.setUrl(urls[item]) //??????????????????
                                break
                            }
                        }
                    }
                }
                videoView?.start() //??????????????????????????????????????????
            }
        })
//        tinyScreen.setOnClickListener {
//            videoView!!.startTinyScreen()
//        }
        viewModel.followResponseLiveDate.observe(this, {result ->
            val result = result.getOrNull()
            if (result is String) {
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                if (result == "????????????") {
                    follow_roomInfo.text = "?????????"
                    isFollowed = true
                } else if (result == "??????????????????") {
                    follow_roomInfo.text = "??????"
                    isFollowed = false
                }
            }
        })
        //?????????????????????
        viewModel.roomInfoResponseData.observe(this, {result ->
            val roomInfo = result.getOrNull()
            if (roomInfo is RoomInfo) {
//                changeRoomInfoVisible(roomInfo_liveRoom.layoutParams.height == 0)
                //????????????
                if (isFirstGetInfo) {
                    follow_roomInfo.setOnClickListener {
                        if (SunnyWeatherApplication.isLogin.value!!) {
                            if (isFollowed) {
                                viewModel.unFollow(roomInfo.platForm, roomInfo.roomId, SunnyWeatherApplication.userInfo!!.uid)
                            } else {
                                viewModel.follow(roomInfo.platForm, roomInfo.roomId, SunnyWeatherApplication.userInfo!!.uid)
                            }
                        } else {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("????????????")
                                .setMessage("????????????????????????")
                                .setCancelable(true)
                                .setNegativeButton("??????") { _, _ ->

                                }
                                .setPositiveButton("??????") { _, _ ->
                                    val intent = Intent(context, LoginActivity::class.java)
                                    startActivity(intent)
                                }
                                .show()
                        }
                    }
                    //?????????????????????
                    if (roomInfo.platForm == "egame" || roomInfo.platForm == "cc") {
                        danmu_not_support.visibility = View.VISIBLE
                        danmu_not_support.text = "????????????${SunnyWeatherApplication.platformName(roomInfo.platForm)}??????"
                    }
                    if (roomInfo.platForm == "huya" && DeviceUtils.getSDKVersionCode() < 26) {
                        danmu_not_support.visibility = View.VISIBLE
                        danmu_not_support.text = "??????8.0?????????????????????????????????"
                    }
                    //?????????
                    if (roomInfo.isLive == 0) {
                        liveRoom_not_live.visibility = View.VISIBLE
                        //???????????????????????????????????????
                        liveRoom_not_live.setOnClickListener {
                            changeRoomInfoVisible(roomInfo_liveRoom.layoutParams.height == 0)
                        }
                    } else {
                        videoView = VideoViewManager.instance().get(platform + roomId) as VideoView<ExoMediaPlayer>?
                        if (mPIPManager.isStartFloatWindow) {
                            mPIPManager.stopFloatWindow()
//                            controller?.setPlayerState(videoView!!.currentPlayerState)
                            mMyDanmakuView.stopFloatPrepare()
                        }
                        player_container.addView(videoView)
                        if (platform == "huya" && roomInfo.categoryName == "?????????") {
                            viewModel.getRealUrl("huyaTest", roomId)
                        } else {
                            viewModel.getRealUrl(platform, roomId)
                        }
                    }
                    Glide.with(this).load(roomInfo.ownerHeadPic).transition(
                        DrawableTransitionOptions.withCrossFade()
                    ).into(ownerPic_roomInfo)
                    ownerName_roomInfo.text = SunnyWeatherApplication.platformName(roomInfo.platForm)
                    roomName_roomInfo.text = roomInfo.ownerName
                    liveRoom_bar_txt.text = roomInfo.roomName
                    isFirstGetInfo = false
                }
                isFollowed = (roomInfo.isFollowed == 1)
                if (isFollowed) follow_roomInfo.text = "?????????"
            } else if (roomInfo is String) {
                Toast.makeText(this, roomInfo, Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, ForegroundService::class.java)
        stopService(intent)
        mMyDanmakuView.resume()
    }

    private fun addControlComponents(controller: StandardVideoController) {
        val completeView = CompleteView(this)
        val errorView = ErrorView(this)
        val prepareView = PrepareView(this)
        prepareView.setClickStart()
        val titleView = TitleView(this)
        mDefinitionControlView = YJLiveControlView(this, this)
        mDefinitionControlView!!.setOnRateSwitchListener(this)
        val gestureView = DragGestureView(this)
        controller.addControlComponent(
            completeView,
            errorView,
            prepareView,
            titleView,
            mDefinitionControlView,
            gestureView,
            mMyDanmakuView
        )
        controller.setCanChangePosition(false)
    }

    fun changeRoomInfoVisible(isVisible: Boolean) {
        val height = PlayerUtils.dp2px(context, 60f)
        var va: ValueAnimator = if(isVisible){
            ValueAnimator.ofInt(0,height)
        }else{
            ValueAnimator.ofInt(height,0)
        }
        va.addUpdateListener {
            val h: Int = it.animatedValue as Int
            roomInfo_liveRoom.layoutParams.height = h
            roomInfo_liveRoom.requestLayout()
            danMu_recyclerView.scrollToPosition(adapter.itemCount-1)
        }
        va.duration = 200
        va.start()
    }

    override fun onPause() {
        super.onPause()
        val playBackGround = sharedPreferences.getBoolean("play_background", false)
        val backTiny = sharedPreferences.getBoolean("tiny_when_back", false)
        if (playBackGround || backTiny) {
            return
        }
        mPIPManager.pause()
    }

    override fun onResume() {
        super.onResume()
        var uid = ""
        if (SunnyWeatherApplication.isLogin.value!!) {
            uid = SunnyWeatherApplication.userInfo!!.uid
        }
        viewModel.getRoomInfo(uid, platform, roomId)
        if (!isFirstGetInfo && !viewModel.isConnecting()) {
            viewModel.startDanmu(platform, roomId, SunnyWeatherApplication.userInfo!!.selectedContent, SunnyWeatherApplication.userInfo!!.isActived == "1")
        }
        mPIPManager.resume()
    }

    override fun onBackPressed() {
        viewModel.stopDanmu()
        if (mPIPManager.onBackPress()) return
        val playBackGround = sharedPreferences.getBoolean("play_background", false)
        val backTiny = sharedPreferences.getBoolean("tiny_when_back", false)
        if (playBackGround || backTiny) {
            AppUtils.unregisterAppStatusChangedListener(this)
        }
        if (backTiny) {
            startFloatWindow()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPIPManager!!.reset()
    }

    override fun onRateChange(url: String?) {
        videoView?.setUrl(url)
        videoView?.replay(false)
    }

    override fun onDanmuShowChange() {
        danmuShow = if (danmuShow) {
            mMyDanmakuView.hide()
            danmuSetting.isShow = !danmuShow
            setDanmuSetting(danmuSetting)
            !danmuShow
        } else {
            mMyDanmakuView.show()
            danmuSetting.isShow = !danmuShow
            setDanmuSetting(danmuSetting)
            !danmuShow
        }
    }

    override fun onDanmuSettingShowChanged() {
        controller!!.stopFadeOut()
    }

    override fun startFloat() {
        startFloatWindow()
    }

    fun stopFloat() {
        mPIPManager.stopFloatWindow()
    }

    private fun startFloatWindow() {
        XXPermissions.with(this)
            // ?????????????????????
            .permission(Permission.SYSTEM_ALERT_WINDOW)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        mPIPManager.startFloatWindow()
                        mPIPManager.resume()
                        finish()
                    }
                }
                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        Toast.makeText(context, "???????????????????????????", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "???????????????????????????", Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    //SharedPreferences????????????
    private fun setDanmuSetting(data: DanmuSetting) {
        val gson = Gson()
        //change data to json
        val strJson = gson.toJson(data)
        sharedPref.edit().putString("danmuSetting", strJson).commit()
    }

    //SharedPreferences????????????
    private fun getDanmuSetting(): DanmuSetting {
        val strJson: String? = sharedPref.getString("danmuSetting", null)
        if (strJson != null) {
            try {
                val gson = Gson()
                val jsonElement = JsonParser().parse(strJson)
                return gson.fromJson(jsonElement, DanmuSetting::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //??????????????????
        return DanmuSetting(true,20f,1f,2f,1.5f,8f,false, false, false)
    }

    override fun getSetting(): DanmuSetting {
        return danmuSetting
    }

    override fun changeSetting(setting: DanmuSetting, updateItem: String) {
        danmuSetting = setting
        setDanmuSetting(setting)
        mMyDanmakuView.setContext(setting, updateItem)
    }

    override fun changeVideoSize(size: Int) {
        videoView?.setScreenScaleType(size)
    }

    fun hideViews(){
        controller!!.hide()
    }

    fun banChanged(isActiveArray: ArrayList<String>){
        viewModel.banChanged(isActiveArray)
    }

    fun changeBanActive(isActive: Boolean) {
        viewModel.activeChange(isActive)
    }

    /**
     * ??????????????????????????????4.4????????????
     * @param activity
     * @param colorId
     */
    private fun setStatusBarColor(activity: Activity, colorId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = activity.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = activity.resources.getColor(colorId)
        }
    }

    private fun toWeb(platform: String, roomId: String) {
        var url = when (platform) {
            "bilibili" -> "https://live.bilibili.com/$roomId"
            "douyu" -> "https://www.douyu.com/$roomId"
            "huya" -> "https://m.huya.com/$roomId"
            "cc" -> "https://cc.163.com/$roomId"
            "egame" -> "https://egame.qq.com/$roomId"
            else -> "http://live.xuchaoyufei.xyz/"
        }
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addCategory(Intent. CATEGORY_BROWSABLE)
        startActivity(intent)
    }

    override fun onForeground(activity: Activity?) {
        val intent = Intent(this, ForegroundService::class.java)
        stopService(intent)
    }

    override fun onBackground(activity: Activity?) {
        AppUtils.unregisterAppStatusChangedListener(this)
        val backTiny = sharedPreferences.getBoolean("tiny_when_back", false)
        if (backTiny) {
            startFloatWindow()
            return
        }
        val intent = Intent(this, ForegroundService::class.java)
        intent.putExtra("platform", platform)
        intent.putExtra("roomId", roomId)
        intent.putExtra("roomInfo", "${ownerName_roomInfo.text}:${roomName_roomInfo.text}")
        startService(intent)
    }
}