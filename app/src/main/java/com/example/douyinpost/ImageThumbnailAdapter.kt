//小图预览
package com.example.douyinpost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class ImageThumbnailAdapter(
    private val images: MutableList<PostImage>,
    private val onImageClick: (Int) -> Unit, 
    private val onAddClick: () -> Unit,      
    private val onDeleteClick: (Int) -> Unit 
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_IMAGE = 1
        private const val TYPE_ADD = 2
    }
    var selectedPosition = 0

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)
        val vBorder: View = itemView.findViewById(R.id.v_border)
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == images.size) TYPE_ADD else TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_thumbnail, parent, false)
        
        return if (viewType == TYPE_ADD) {
            AddViewHolder(view)
        } else {
            ImageViewHolder(view)
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_ADD) {
            val addHolder = holder as AddViewHolder
            addHolder.ivThumbnail.setImageResource(android.R.drawable.ic_input_add)
            addHolder.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
            addHolder.itemView.setOnClickListener { onAddClick() }

        } else {
            val imageHolder = holder as ImageViewHolder
            val image = images[position]
            // 相机就用Bitmap，不行就用Uri
            if (image.bitmap != null) {
                imageHolder.ivThumbnail.setImageBitmap(image.bitmap)
            } else if (image.uri != null) {
                imageHolder.ivThumbnail.setImageURI(image.uri)
            } else {
                // 以防万一
                imageHolder.ivThumbnail.setImageDrawable(null)
            }
            imageHolder.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
            imageHolder.btnDelete.visibility = View.VISIBLE
            
            imageHolder.itemView.setOnClickListener { 
                selectedPosition = holder.bindingAdapterPosition
                onImageClick(holder.bindingAdapterPosition)
                notifyDataSetChanged() 
            }

            imageHolder.btnDelete.setOnClickListener {
                onDeleteClick(holder.bindingAdapterPosition)
            }
            //选中的图片不动，未选中的半透明(后续用高亮边框替代了，记得最后完成的时候做取舍)
            if (position == selectedPosition) {
                imageHolder.vBorder.visibility = View.VISIBLE
                imageHolder.ivThumbnail.alpha = 1.0f
            } else {
                imageHolder.vBorder.visibility = View.INVISIBLE
                imageHolder.ivThumbnail.alpha = 0.5f
            }

        }
    }
//统计图片总数，因为有个加号，所以+1
    override fun getItemCount(): Int = images.size + 1
    fun onItemMove(fromPosition: Int, toPosition: Int) {    //调整顺序
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(images, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(images, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
}