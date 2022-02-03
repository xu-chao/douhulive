package com.xuchao.douhu.ui.follows

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.xuchao.douhu.logic.Repository
import com.xuchao.douhu.logic.model.RoomInfo

class FollowViewModel : ViewModel(){
    private val uidLiveData = MutableLiveData<String>()
    var roomList = ArrayList<RoomInfo>()

    val userRoomListLiveDate = Transformations.switchMap(uidLiveData) {
            uid -> Repository.getRoomsOn(uid)
    }

    fun getRoomsOn(uid: String?) {
        if(uid != null) {
            uidLiveData.value = uid!!
        }
    }

    fun clearRoomList() {
        roomList.clear()
    }
}