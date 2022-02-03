package com.xuchao.douhu.ui.area

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.xuchao.douhu.R
import com.xuchao.douhu.logic.model.AreaInfo

class AreaListAdapter(private val fragment: AreaSingleFragment, private val areaList: List<AreaInfo>) :
    RecyclerView.Adapter<AreaListAdapter.ViewHolder>(){

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val areaPic: ImageView = view.findViewById(R.id.areaPic)
        val areaName: TextView = view.findViewById(R.id.areaName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AreaListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.area_item, parent, false)
        val holder = ViewHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.layoutPosition
            val areaInfo = areaList[position]
            fragment.selectArea(areaInfo.typeName, areaInfo.areaName)

        }
        return holder
    }

    override fun onBindViewHolder(holder: AreaListAdapter.ViewHolder, position: Int) {
        val areaInfo = areaList[position]
        holder.areaName.text = areaInfo.areaName
        Glide.with(fragment).load(areaInfo.areaPic).transition(DrawableTransitionOptions.withCrossFade()).into(holder.areaPic)
    }

    override fun getItemCount(): Int {
        return areaList.size
    }
}