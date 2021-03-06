package com.xuchao.douhu.util.dkplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xuchao.douhu.R
import com.xuchao.douhu.SunnyWeatherApplication
import com.xuchao.douhu.logic.model.UserInfo
import com.xuchao.douhu.ui.liveRoom.LiveRoomActivity
import com.xuchao.douhu.ui.login.LoginActivity
import com.xuchao.douhu.ui.login.LoginViewModel
import kotlinx.android.synthetic.main.fragment_danmu_banned.*

class DanmuBanFragment: Fragment() {
    private val viewModel by lazy { ViewModelProvider(this).get(LoginViewModel::class.java) }
    private var banContents =  ArrayList<String>()
    private var isSelectedArray =  ArrayList<String>()
    private var loadedBan = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_danmu_banned, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (SunnyWeatherApplication.isLogin.value!!) {
            active_ban.isChecked = SunnyWeatherApplication.userInfo!!.isActived == "1"
            val banContentsString = SunnyWeatherApplication.userInfo!!.allContent
            val isSelectString = SunnyWeatherApplication.userInfo!!.selectedContent
            if (banContentsString != ""){
                if (banContentsString.contains(";")){
                    banContents = banContentsString.split(";") as ArrayList<String>
                } else {
                    banContents.add(banContentsString)
                }
            }
            if (isSelectString != ""){
                if (isSelectString.contains(";")) {
                    isSelectedArray = isSelectString.split(";") as ArrayList<String>
                } else {
                    isSelectedArray.add(isSelectString)
                }

            }
            addClips()
        }
        if (SunnyWeatherApplication.isLogin!!.value!!) {
            active_ban.setOnCheckedChangeListener { buttonView, isChecked ->
                if (SunnyWeatherApplication.isLogin!!.value!!) {
                    saveBanActive(isChecked)
                    val context = context as LiveRoomActivity
                    context.changeBanActive(isChecked)
                    if (isChecked) {
                        Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            active_ban.visibility = View.INVISIBLE
            active_ban_txt.visibility = View.VISIBLE
            active_ban_txt.setOnClickListener {
                //??????????????????
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("??????")
                    .setMessage("???????????????????????????")
                    .setCancelable(true)
                    .setNegativeButton("??????") { _, _ ->

                    }
                    .setPositiveButton("??????") { _, _ ->
                        val intent = Intent(SunnyWeatherApplication.context, LoginActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
            }
        }

        add_ban_btn.setOnClickListener {
            //??????????????????
            if (!SunnyWeatherApplication.isLogin.value!!) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("??????")
                    .setMessage("???????????????????????????")
                    .setCancelable(true)
                    .setNegativeButton("??????") { _, _ ->

                    }
                    .setPositiveButton("??????") { _, _ ->
                        val intent = Intent(SunnyWeatherApplication.context, LoginActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
            }else {
                val text = add_ban_content.text.toString() //??????????????????
                if (text.isEmpty()) {
                    Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
                } else if (banContents.contains(text)) {
                    Toast.makeText(context, "?????????", Toast.LENGTH_SHORT).show()
                } else {
                    banContents.add(text)
                    isSelectedArray.add(text)
                    saveBanInfo(banContents, isSelectedArray)
                    val chip = createNewChip(text, true)
                    ban_chipGroup.addView(chip)
                    add_ban_content.setText("")
                    add_ban_TextField.clearFocus()
                }
            }
        }
        SunnyWeatherApplication.isLogin.observe(viewLifecycleOwner, { result ->
            if (!loadedBan && result) {
                active_ban.visibility = View.VISIBLE
                active_ban_txt.visibility = View.GONE
                val isActive = SunnyWeatherApplication.userInfo!!.isActived == "1"
                val context = context as LiveRoomActivity
                active_ban.isChecked = isActive
                context.changeBanActive(isActive)
                active_ban.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (SunnyWeatherApplication.isLogin!!.value!!) {
                        saveBanActive(isChecked)
                        context.changeBanActive(isChecked)
                        if (isChecked) {
                            Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show()
                        }
                    } else {

                    }
                }
                val banContentsString = SunnyWeatherApplication.userInfo!!.allContent
                val isSelectString = SunnyWeatherApplication.userInfo!!.selectedContent
                if (banContentsString != ""){
                    if (banContentsString.contains(";")){
                        banContents = banContentsString.split(";") as ArrayList<String>
                    } else {
                        banContents.add(banContentsString)
                    }
                }
                if (isSelectString != ""){
                    if (isSelectString.contains(";")) {
                        isSelectedArray = isSelectString.split(";") as ArrayList<String>
                    } else {
                        isSelectedArray.add(isSelectString)
                    }

                }
                addClips()
            }
        })
        viewModel.updateUserInfoLiveDate.observe(viewLifecycleOwner, { result ->
            val temp = result.getOrNull()
            if (temp is UserInfo) {
                SunnyWeatherApplication.userInfo = temp
                Log.i("test", temp.toString())
                Toast.makeText(context, "????????????????????????", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, temp.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        Log.i("test", "onPause")
    }

    //??????list??????clips
    private fun addClips() {
        loadedBan = true
        for (banContent in banContents) {
            val chip = createNewChip(banContent, isSelectedArray.contains(banContent))
            ban_chipGroup.addView(chip)
        }
    }

    //??????chip
    private fun createNewChip(banContent: String, checked: Boolean): Chip{
        val chip = Chip(context)
        chip.isCloseIconVisible = true
        chip.text = banContent
        chip.isCheckable = true
        chip.isChecked = checked
        //??????????????????
        chip.setOnCloseIconClickListener {
            banContents.remove(banContent)
            if (chip.isChecked) {
                isSelectedArray.remove(banContent)
            }
            ban_chipGroup.removeView(chip)
            saveBanInfo(banContents, isSelectedArray)
        }
        //????????????
        chip.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                isSelectedArray.add(chip.text.toString())
            } else {
                isSelectedArray.remove(chip.text.toString())
            }
            saveBanInfo(banContents, isSelectedArray)
        }
        return chip
    }

    //????????????????????????
    private fun saveBanInfo(banArray: ArrayList<String>, isSelectedArray: ArrayList<String>) {
        val context = context as LiveRoomActivity
        if (!SunnyWeatherApplication.isLogin.value!!) {
            return
        }
        val userInfo = SunnyWeatherApplication.userInfo
        userInfo!!.allContent = banArray.joinToString(";")
        userInfo!!.selectedContent = isSelectedArray.joinToString(";")
        context.banChanged(isSelectedArray)
        viewModel.changeUserInfo(userInfo)
    }

    private fun saveBanActive(isActive: Boolean){
        val userInfo = SunnyWeatherApplication.userInfo
        userInfo!!.isActived = when(isActive){
            true -> "1"
            false -> "0"
        }
        viewModel.changeUserInfo(userInfo)
    }
}