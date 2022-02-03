package com.xuchao.douhu.ui.login

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.xuchao.douhu.logic.Repository
import com.xuchao.douhu.logic.model.UserInfo

class LoginViewModel : ViewModel() {
    data class LoginRequest(val username: String, val password: String)
    data class RegisterRequest(val username: String, val nickName: String, val password: String)

    private val loginLiveData = MutableLiveData<LoginRequest>()
    private val registerLiveData = MutableLiveData<RegisterRequest>()
    private val userInfoLiveData = MutableLiveData<UserInfo>()
    private val updateLiveData = MutableLiveData<Int>()

    val updateResponseLiveData = Transformations.switchMap(updateLiveData) {
         Repository.versionUpdate()
    }
    val loginResponseLiveDate = Transformations.switchMap(loginLiveData) {
            value -> Repository.login(value.username, value.password)
    }
    val registerResponseLiveDate = Transformations.switchMap(registerLiveData) {
            value -> Repository.register(value.username, value.nickName, value.password)
    }
    val updateUserInfoLiveDate = Transformations.switchMap(userInfoLiveData) {
            value -> Repository.changeUserInfo(value)
    }

    fun checkVersion() {
        updateLiveData.value = 0
    }

    fun doLogin(username: String, password: String) {
        loginLiveData.value = LoginRequest(username, password)
    }

    fun doRegister(username: String, password: String, nickName: String) {
        registerLiveData.value = RegisterRequest(username, nickName, password)
    }

    fun changeUserInfo(userInfo: UserInfo) {
        userInfoLiveData.value = userInfo
    }
}