//大图预览
package com.example.douyinpost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImagePreviewAdapter(
    private val images: List<PostImage>
) : RecyclerView.Adapter<ImagePreviewAdapter.PreviewViewHolder>() {

    inner class PreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPreview: ImageView = itemView.findViewById(R.id.iv_preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_preview, parent, false)
        return PreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val image = images[position]

        if (image.bitmap != null) {
            holder.ivPreview.setImageBitmap(image.bitmap)
        } else if (image.uri != null) {
            holder.ivPreview.setImageURI(image.uri)
        }else {
            // 依旧以防万一
            holder.ivPreview.setImageDrawable(null)
        }
        
        holder.ivPreview.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    override fun getItemCount(): Int = images.size
}