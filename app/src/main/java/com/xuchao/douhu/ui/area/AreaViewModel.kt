package com.xuchao.douhu.ui.area

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.xuchao.douhu.logic.Repository
import com.xuchao.douhu.logic.model.AreaInfo

class AreaViewModel : ViewModel() {
    private val temp = MutableLiveData<Int>()

    var areaList = ArrayList<AreaInfo>()

    var areaListLiveDate = Transformations.switchMap(temp) {
        Repository.getAllAreas()
    }

    fun getAllAreas() {
        temp.value = 1
    }
}