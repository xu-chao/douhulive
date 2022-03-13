package com.xuchao.douhu.ui.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.blankj.utilcode.util.BarUtils
import com.xuchao.douhu.R
import com.xuchao.douhu.SunnyWeatherApplication.Companion.context
import shortbread.Shortcut

@Shortcut(id = "sc_setting", icon = R.drawable.sc_setting, shortLabel = "基本设置", rank = 1)
class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var theme = sharedPreferences.getInt("theme", R.style.SunnyWeather)
        setTheme(theme)
        setContentView(R.layout.activity_setting)
        if (theme != R.style.nightTheme) {
            BarUtils.setStatusBarLightMode(this, true)
        } else {
            BarUtils.setStatusBarLightMode(this, false)
        }
    }

}