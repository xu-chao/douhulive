package com.xuchao.douhu.ui.area

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ScreenUtils
import com.xuchao.douhu.R
import com.xuchao.douhu.logic.model.AreaInfo
import com.xuchao.douhu.ui.roomList.SpaceItemDecoration
import kotlinx.android.synthetic.main.fragment_arealist.*
import java.lang.IllegalArgumentException

class AreaSingleFragment(private val areaList: List<AreaInfo>) : Fragment() {
    constructor() : this(ArrayList<AreaInfo>())

    private lateinit var mFragmentListener: FragmentListener

    private lateinit var adapter: AreaListAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_arealist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var cardNum = ScreenUtils.getAppScreenWidth()/ ConvertUtils.dp2px(129F)
        if (cardNum < 2) cardNum = 2
        val layoutManager = GridLayoutManager(context, cardNum)
        recyclerView_area.addItemDecoration(SpaceItemDecoration(10))
        recyclerView_area.layoutManager = layoutManager
        adapter = AreaListAdapter(this, areaList)
        recyclerView_area.adapter = adapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mFragmentListener = if (context is FragmentListener) {
            context
        } else {
            throw IllegalArgumentException("Activity must implements FragmentListener")
        }
    }

    fun selectArea(areaType: String, areaName: String) {
        mFragmentListener.onFragment(areaType, areaName)
    }

    interface FragmentListener {
        fun onFragment(areaType: String, areaName: String)
    }
}