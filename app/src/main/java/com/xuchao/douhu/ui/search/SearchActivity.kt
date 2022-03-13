package com.xuchao.douhu.ui.search

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.paulrybitskyi.persistentsearchview.adapters.model.SuggestionItem
import com.paulrybitskyi.persistentsearchview.listeners.OnSearchConfirmedListener
import com.paulrybitskyi.persistentsearchview.listeners.OnSearchQueryChangeListener
import com.paulrybitskyi.persistentsearchview.listeners.OnSuggestionChangeListener
import com.paulrybitskyi.persistentsearchview.utils.SuggestionCreationUtil
import com.xuchao.douhu.R
import com.xuchao.douhu.logic.model.Owner
import com.xuchao.douhu.ui.customerUIs.AnimationUtils
import com.xuchao.douhu.ui.customerUIs.HeaderedRecyclerViewListener
import com.xuchao.douhu.ui.customerUIs.VerticalSpacingItemDecorator
import com.xuchao.douhu.ui.roomList.SpaceItemDecoration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_search.*
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.blankj.utilcode.util.BarUtils
import shortbread.Shortcut

@Shortcut(id = "sc_search", icon = R.drawable.sc_search, shortLabel = "搜索主播", rank = 2)
class SearchActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var historySearchList: ArrayList<String>
    private val viewModel by lazy { ViewModelProvider(this).get(SearchViewModel::class.java) }
    private lateinit var searchAdapter: SearchAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //颜色主题
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeActived = sharedPreferences.getInt("theme", R.style.SunnyWeather)
        setTheme(themeActived)
        setContentView(R.layout.activity_search)
        if (themeActived != R.style.nightTheme) {
            BarUtils.setStatusBarLightMode(this, true)
        } else {
            BarUtils.setStatusBarLightMode(this, false)
        }
        BarUtils.transparentStatusBar(this)
        init()
//        val window: Window = this.window
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            window.statusBarColor = Color.TRANSPARENT
//            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
//
//            val decor: View = this.window.decorView
//                decor.systemUiVisibility =
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            SunnyWeatherApplication.MIUISetStatusBarLightMode(this, true)
//        }
        //****************************
        recyclerView_search.addItemDecoration(SpaceItemDecoration(10))
        searchAdapter = SearchAdapter(this, viewModel.ownersList as List<Owner>)
        recyclerView_search.adapter = searchAdapter
        viewModel.ownerListLiveData.observe(this, {result ->
            val rooms = result.getOrNull()
            if (rooms is ArrayList<*>) {
                viewModel.ownersList.addAll(rooms as Collection<Owner>)
                searchAdapter.notifyDataSetChanged()
                progressBar.isGone = true
                recyclerView_search.animate()
                    .alpha(1f)
                    .setInterpolator(LinearInterpolator())
                    .setDuration(300L)
                    .start()
            } else if (rooms is String) {
                Toast.makeText(this, rooms, Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
        intent.getStringExtra("query")?.also { query ->
            viewModel.search("all", query, "0")
        }

    }


    private fun init() {
        sharedPref = this.getSharedPreferences("JustLive", Context.MODE_PRIVATE)
        val tempString = sharedPref.getString("historySearch", "")
        historySearchList = if (tempString == "") {
            ArrayList()
        } else {
            val temp = java.util.ArrayList(tempString.toString().trim().split(","))
            temp
        }

        initProgressBar()
        initSearchView()
        emptyViewLl.isVisible = viewModel.ownersList.isEmpty()
        initRecyclerView()
    }

    private fun initRecyclerView() = with(recyclerView_search) {
        layoutManager = LinearLayoutManager(context)

        addItemDecoration(initVerticalSpacingItemDecorator())
        addOnScrollListener(initHeaderedRecyclerViewListener())
    }

    private fun initHeaderedRecyclerViewListener(): HeaderedRecyclerViewListener {
        return object : HeaderedRecyclerViewListener(this@SearchActivity) {

            override fun showHeader() {
                AnimationUtils.showHeader(persistentSearchView)
            }

            override fun hideHeader() {
                AnimationUtils.hideHeader(persistentSearchView)
            }

        }
    }

    private fun initVerticalSpacingItemDecorator(): VerticalSpacingItemDecorator {
        return VerticalSpacingItemDecorator(
            verticalSpacing = 2.dpToPx(this),
            verticalSpacingCompensation = 2.dpToPx(this)
        )
    }

    private fun initProgressBar() {
        progressBar.isGone = true
    }

    private fun initSearchView() = with(persistentSearchView) {
        setOnLeftBtnClickListener(this@SearchActivity)
        setOnClearInputBtnClickListener(this@SearchActivity)
        setOnSearchConfirmedListener(mOnSearchConfirmedListener) //提交搜索监听
        setOnSearchQueryChangeListener(mOnSearchQueryChangeListener)//输入监听
        setOnSuggestionChangeListener(mOnSuggestionChangeListener)  //选择历史记录或删除
        setDismissOnTouchOutside(true)
        setDimBackground(true)
        isProgressBarEnabled = true
        isClearInputButtonEnabled = true
        setSuggestionsDisabled(false)
        setQueryInputGravity(Gravity.START or Gravity.CENTER)
        setQueryInputHint("斗鱼用房间号搜")
    }
    //选择历史记录或删除
    private val mOnSuggestionChangeListener = object : OnSuggestionChangeListener {
        override fun onSuggestionPicked(suggestion: SuggestionItem) {
            val query = suggestion.itemModel.text
            setSuggestions(getSuggestionsForQuery(query), false)
            performSearch(query)
        }
        override fun onSuggestionRemoved(suggestion: SuggestionItem) {
            removeSearchQuery(suggestion.itemModel.text)
        }
    }

    private val mOnSearchQueryChangeListener = OnSearchQueryChangeListener { searchView, oldQuery, newQuery ->
        setSuggestions(
            if(newQuery.isBlank()) {
                getInitialSearchQueries()
            } else {
                getSuggestionsForQuery(newQuery)
            },
            true
        )
    }

    private fun getInitialSearchQueries(): List<String> {
        return historySearchList
    }

    private fun getSuggestionsForQuery(query: String): List<String> {
        val resultList = ArrayList<String>()

        if(query.isEmpty()) {
            return historySearchList
        } else {
            historySearchList.forEach {
                if(it.lowercase().startsWith(query.lowercase())) {
                    resultList.add(it)
                }
            }
        }

        return resultList
    }

    //删除历史记录
    private fun removeSearchQuery(query: String) {
        historySearchList.remove(query)
        sharedPref.edit().putString("historySearch", listToString(historySearchList, ',')).commit()
    }

    //设置搜索结果
    private fun setSuggestions(queries: List<String>, expandIfNecessary: Boolean) {
        val suggestions: List<SuggestionItem> = SuggestionCreationUtil.asRecentSearchSuggestions(queries)
        persistentSearchView.setSuggestions(suggestions, expandIfNecessary)
    }

    private val mOnSearchConfirmedListener = OnSearchConfirmedListener { searchView, query ->
        saveQuery(query)//保存历史记录
        searchView.collapse()//折叠历史记录
        performSearch(query)
    }

    private fun performSearch(query: String) {
        emptyViewLl.isGone = true
        recyclerView_search.alpha = 0f
        progressBar.isVisible = true
        viewModel.clearList()
        persistentSearchView.hideProgressBar(false)
        persistentSearchView.showLeftButton()
        viewModel.search("all", query, "0")
    }

    //保存历史记录
    private fun saveQuery(query: String) {
        historySearchList.add(0, query)
        if (historySearchList.size > 8) {
            historySearchList.removeLast()
        }
        val historyString = listToString(historySearchList, ',')
        sharedPref.edit().putString("historySearch", historyString).commit()
    }

    private fun listToString(list: List<String>, separator: Char): String? {
        if (list.isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        for (i in list.indices) {
            sb.append(list[i]).append(separator)
        }
        return sb.toString().substring(0, sb.toString().length - 1)
    }

    private fun Int.dpToPx(context: Context): Int {
        return toFloat().dpToPx(context).roundToInt()
    }

    private fun Float.dpToPx(context: Context): Float {
        return (this * context.resources.displayMetrics.density)
    }

    override fun onClick(view: View) {
        when(view.id) {
            R.id.leftBtnIv -> onLeftButtonClicked()
            R.id.clearInputBtnIv -> onClearInputButtonClicked()
        }
    }

    private fun onLeftButtonClicked() {
        onBackPressed()
    }

    private fun onClearInputButtonClicked() {
        //
    }

    override fun onResume() {
        super.onResume()

        val searchQueries = if(persistentSearchView.isInputQueryEmpty) {
            getInitialSearchQueries()
        } else {
            getSuggestionsForQuery(persistentSearchView.inputQuery)
        }

        setSuggestions(searchQueries, false)

        if(shouldExpandSearchView()) {
            persistentSearchView.expand(false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    private fun shouldExpandSearchView(): Boolean {
        return (
            (persistentSearchView.isInputQueryEmpty && (viewModel.ownersList.isEmpty())) ||
                    persistentSearchView.isExpanded
        )
    }

}