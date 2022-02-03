package com.xuchao.douhu.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ScreenUtils
import com.xuchao.douhu.R
import com.xuchao.douhu.SunnyWeatherApplication
import com.xuchao.douhu.logic.model.RoomInfo
import com.xuchao.douhu.ui.roomList.RoomListAdapter
import com.xuchao.douhu.ui.roomList.SpaceItemDecoration
import kotlinx.android.synthetic.main.fragment_roomlist.*

class RecommendFragment(val platform: String) : Fragment()  {
    constructor(): this("all")
    private val viewModel by lazy { ViewModelProvider(this, HomeViewModelFactory(platform)).get(HomeViewModel::class.java) }
    private lateinit var adapter: RoomListAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_roomlist, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var cardNum = ScreenUtils.getAppScreenWidth()/ConvertUtils.dp2px(195F)
        if (cardNum < 2) cardNum = 2
        val layoutManager = GridLayoutManager(context, cardNum)
        recyclerView.addItemDecoration(SpaceItemDecoration(10))
        recyclerView.layoutManager = layoutManager
        adapter = RoomListAdapter(this, viewModel.roomList)
        recyclerView.adapter = adapter
        //下拉刷新，加载更多
        refresh_home_foot.setFinishDuration(0)//设置Footer 的 “加载完成” 显示时间为0
        refresh_home.setOnRefreshListener {
            viewModel.clearPage()
            viewModel.getRecommend(SunnyWeatherApplication.areaType.value?:"all", SunnyWeatherApplication.areaName.value?:"all")
        }
        refresh_home.setOnLoadMoreListener {
            viewModel.getRecommend(SunnyWeatherApplication.areaType.value?:"all", SunnyWeatherApplication.areaName.value?:"all")
        }
        //绑定LiveData监听器
        SunnyWeatherApplication.areaName.observe(viewLifecycleOwner, {
            viewModel.clearPage()
            viewModel.clearList()
            progressBar_roomList.isVisible = true
            recyclerView.isGone = true
            viewModel.getRecommend(SunnyWeatherApplication.areaType.value?:"all", SunnyWeatherApplication.areaName.value?:"all")
        })
        viewModel.roomListLiveDate.observe(viewLifecycleOwner, { result ->
            val temp = result.getOrNull()
            var rooms: ArrayList<RoomInfo>? = null
            if (temp != null) rooms = temp as ArrayList<RoomInfo>
            if (rooms != null && rooms.size > 0) {
                if(refresh_home.isRefreshing) {
                    viewModel.clearList()
                }
                viewModel.roomList.addAll(rooms)
                adapter.notifyDataSetChanged()
                progressBar_roomList.isGone = true
                recyclerView.isVisible = true
                refresh_home.finishRefresh() //传入false表示刷新失败
                refresh_home.finishLoadMore() //传入false表示加载失败
            } else {
                progressBar_roomList.isGone = true
                recyclerView.isVisible = true
                refresh_home.finishLoadMoreWithNoMoreData()
                if (viewModel.roomList.size == 0) {
                    state.showEmpty()
                }
                result.exceptionOrNull()?.printStackTrace()
            }
        })
        viewModel.getRecommend(SunnyWeatherApplication.areaType.value?:"all", SunnyWeatherApplication.areaName.value?:"all")
        progressBar_roomList.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SunnyWeatherApplication.areaName.removeObservers(viewLifecycleOwner)
        viewModel.roomListLiveDate.removeObservers(viewLifecycleOwner)
    }
}