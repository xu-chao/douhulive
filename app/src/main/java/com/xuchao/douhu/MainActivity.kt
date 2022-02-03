package com.xuchao.douhu

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.angcyo.tablayout.delegate2.ViewPager2Delegate
import com.xuchao.douhu.logic.model.UserInfo
import com.xuchao.douhu.ui.area.AreaFragment
import com.xuchao.douhu.ui.area.AreaSingleFragment
import com.xuchao.douhu.ui.follows.FollowsFragment
import com.xuchao.douhu.ui.home.HomeFragment
import com.xuchao.douhu.ui.login.LoginViewModel
import com.xuchao.douhu.ui.search.SearchActivity
import kotlinx.android.synthetic.main.activity_main.*

import android.view.*
import android.widget.CompoundButton
import androidx.core.view.GravityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.blankj.utilcode.util.BarUtils
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.*

import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.xuchao.douhu.logic.model.UpdateInfo
import com.xuchao.douhu.ui.login.LoginActivity
import com.xuchao.douhu.ui.setting.SettingActivity
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.dialog_update.*

class MainActivity : AppCompatActivity(), AreaSingleFragment.FragmentListener {
    private val viewModel by lazy { ViewModelProvider(this).get(LoginViewModel::class.java) }
    private lateinit var areaFragment: AreaFragment
    private lateinit var viewPager: ViewPager2
    private var isVersionCheck = false
    private lateinit var mMenu: Menu
    private var themeActived = R.style.SunnyWeather

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //颜色主题
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        themeActived = sharedPreferences.getInt("theme", R.style.SunnyWeather)
        Log.i("test", "theme:" + themeActived)
        setTheme(themeActived)
        setContentView(R.layout.activity_main)
        var nightChecked: Boolean
        if (themeActived != R.style.nightTheme) {
            nightChecked = false
            BarUtils.setStatusBarLightMode(this, true)
        } else {
            nightChecked = true
            BarUtils.setStatusBarLightMode(this, false)
        }
        setSupportActionBar(main_toolBar)
        initLogin()
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.icon_menu)
            it.setDisplayShowTitleEnabled(false)
        }

        val nightChangeListener = object : OnCheckedChangeListener {
            override fun onCheckedChanged(drawerItem: IDrawerItem<*>, buttonView: CompoundButton, isChecked: Boolean) {
            if (isChecked) {
                    sharedPreferences.edit().putInt("theme", R.style.nightTheme).commit()
                    nightChecked = false
                    recreate()
                } else {
                    sharedPreferences.edit().putInt("theme", R.style.SunnyWeather).commit()
                    nightChecked = true
                    recreate()
                }
            }
        }
        //关闭抽屉滑动打开
//        main_drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        //if you want to update the items at a later time it is recommended to keep it in a variable
        AccountHeaderView(this).apply {
            attachToSliderView(slider) // attach to the slider
            addProfiles(
                ProfileDrawerItem().apply { nameText = "原来是你这个崽"; descriptionText = "douhulive@gmail.com"; identifier = 102 }
            )
            withSavedInstance(savedInstanceState)
        }
        // get the reference to the slider and add the items
        slider.itemAdapter.add(
            PrimaryDrawerItem().apply { nameRes = R.string.drawItem1; isSelectable = false; identifier = 1; },
            DividerDrawerItem(),
            SecondaryDrawerItem().apply { nameRes = R.string.drawItem2; identifier = 2; isSelectable = false },
            DividerDrawerItem(),
            SecondaryDrawerItem().apply { identifier = 3; nameRes = R.string.setting; iconicsIcon = GoogleMaterial.Icon.gmd_settings; isSelectable = false},
            SwitchDrawerItem().apply { nameText = "夜间模式"; iconicsIcon = GoogleMaterial.Icon.gmd_brightness_4;
                onCheckedChangeListener = nightChangeListener; isSelectable = false; isChecked = nightChecked}
        )
        // specify a click listener
        slider.onDrawerItemClickListener = { v, drawerItem, position ->
            // do something with the clicked item :D
            var intent: Intent? = null
            when {
                drawerItem.identifier == 3L -> intent = Intent(this, SettingActivity::class.java)

            }
            if (intent != null) {
                this.startActivity(intent)
            }
            false
        }


        //ViewPager2
        viewPager = main_fragment
        viewPager.isUserInputEnabled = false
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewModel.updateResponseLiveData.observe(this, { result ->
            val updateInfo = result.getOrNull()
            if (updateInfo is UpdateInfo) {
                var sharedPref = getSharedPreferences("JustLive", Context.MODE_PRIVATE)
                val ignoreVersion = sharedPref.getInt("ignoreVersion",0)
                val versionNum = SunnyWeatherApplication.getVersionCode(SunnyWeatherApplication.context)
                if (versionNum == updateInfo.versionNum || ignoreVersion == updateInfo.versionNum) {
                    if (isVersionCheck) {
                        Toast.makeText(SunnyWeatherApplication.context, "当前已是最新版本^_^", Toast.LENGTH_SHORT).show()
                    }
                    return@observe
                }
                var descriptions = ""
                var index = 1
                for (item in updateInfo.description) {
                    descriptions = "$descriptions$index.$item<br>"
                    index++
                }
                val dialogContent = Html.fromHtml("<div>$descriptions</div>")
                MaterialDialog(this).show {
                    customView(R.layout.dialog_update)
                    update_description.text = dialogContent
                    update_version.text = "版本: ${updateInfo.latestVersion}"
                    update_size.text = "下载体积: ${updateInfo.apkSize}"
                    ignore_btn.setOnClickListener {
                        var sharedPref = context.getSharedPreferences("JustLive", Context.MODE_PRIVATE)
                        sharedPref.edit().putInt("ignoreVersion", updateInfo.versionNum).commit()
                        Toast.makeText(context, "已忽略", Toast.LENGTH_SHORT).show()
                        cancel()
                    }
                    versionchecklib_version_dialog_cancel.setOnClickListener {
                        dismiss()
                    }
                    versionchecklib_version_dialog_commit.setOnClickListener {
                        val uri = Uri.parse(updateInfo.updateUrl)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.addCategory(Intent. CATEGORY_BROWSABLE)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    if (isVersionCheck) {
                        ignore_btn.visibility = View.GONE
                    }
                }
            } else if(updateInfo is String){
                Toast.makeText(this, "用户密码已修改，请重新登录", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        val title = SunnyWeatherApplication.areaName.value
                        if (title == "all" || title == null) {
                            main_toolBar_title.text = "全部推荐"
                        } else {
                            main_toolBar_title.text = title
                        }
                        val drawable = resources.getDrawable(R.drawable.icon_arrow_down)
                        main_toolBar_title.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
                    }
                    1 -> {
                        main_toolBar_title.text = "关注"
                        main_toolBar_title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                    }
                }
            }
        })
        //tabLayout
        ViewPager2Delegate.install(viewPager, tab_main)

        //启动页
        val startPage = sharedPreferences.getString("start_page", "0")
        if (startPage == "1") {
            viewPager.currentItem = 1
        }
        //标题栏的标题click事件
        main_toolBar_title.setOnClickListener {
            val fragmentManager = supportFragmentManager
            areaFragment = AreaFragment()
            areaFragment.show(fragmentManager, "areaFragment")
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val newTheme = sharedPreferences.getInt("theme", R.style.SunnyWeather)
        if (newTheme != themeActived){
            recreate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the options menu from XML
        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar, menu)
        mMenu = menu
        SunnyWeatherApplication.isLogin.observe(this, {result ->
            if (result) {
                mMenu.findItem(R.id.toolbar_login).isVisible = false
                mMenu.findItem(R.id.toolbar_logout).isVisible = true
            } else {
                mMenu.findItem(R.id.toolbar_login).isVisible = true
                mMenu.findItem(R.id.toolbar_logout).isVisible = false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> main_drawerLayout.openDrawer(GravityCompat.START)
//            android.R.id.home -> {
//                Toast.makeText(this, "开发中", Toast.LENGTH_SHORT).show()
//            }
            R.id.toolbar_setting -> {
                val intent = Intent(this, SettingActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_search -> {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }
            R.id.toolbar_login -> {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            R.id.toolbar_update -> {
                isVersionCheck = true
                viewModel.checkVersion()
            }
            R.id.toolbar_logout -> {
                SunnyWeatherApplication.clearLoginInfo(this)
                main_fragment.currentItem = 0
            }
        }
        return true
    }

    override fun onFragment(areaType: String, areaName:String) {
        main_toolBar_title.text = areaName
        SunnyWeatherApplication.areaType.value = areaType
        SunnyWeatherApplication.areaName.value = areaName
        areaFragment.dismiss()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> HomeFragment()
                1 -> FollowsFragment()
                else -> Fragment()
            }
    }
    fun toFirst(){
        viewPager.currentItem = 0
    }

    private fun initLogin(){
        var sharedPref = this.getSharedPreferences("JustLive", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "").toString()
        val password = sharedPref.getString("password", "").toString()
        val hsa = sharedPref.contains("")
        viewModel.loginResponseLiveDate.observe(this, { result ->
            val userInfo = result.getOrNull()
            if (userInfo is UserInfo) {
                MobclickAgent.onProfileSignIn(userInfo.userName)//友盟账号登录
                SunnyWeatherApplication.userInfo = userInfo
                SunnyWeatherApplication.isLogin.value = true
            } else if(userInfo is String){
                SunnyWeatherApplication.clearLoginInfo(this)
                Toast.makeText(this, "用户密码已修改，请重新登录", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
        if (password.length > 1) {
            viewModel.doLogin(username, password)
        }
        viewModel.checkVersion()
    }
}