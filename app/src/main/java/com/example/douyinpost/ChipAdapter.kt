package com.example.douyinpost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChipAdapter(
    private val dataList: MutableList<String>,
    private val autoRemove: Boolean = true,//由于复用，所以要做区分
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {
    inner class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_topic_name)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        //复用之前的item_topic_chip.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val text = dataList[position]
        holder.tvName.text = text
        
        holder.itemView.setOnClickListener {
            onItemClick(text)
            //根据应用的场景(话题那里就自动消失，地址就留着)
            if (autoRemove) {
                if (position != RecyclerView.NO_POSITION) {
                    // 使用 holder.bindingAdapterPosition 获取准确位置
                    val actualPos = holder.bindingAdapterPosition
                    if (actualPos in dataList.indices) {
                        dataList.removeAt(actualPos)
                        notifyItemRemoved(actualPos)
                        // 刷新受影响的后续项（防止位置错乱）
                        notifyItemRangeChanged(actualPos, dataList.size)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}