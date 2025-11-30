package com.example.douyinpost

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/**
 * 代表一张投稿图片
 * 支持从相册选的图 (Uri) 和相机拍的图 (Bitmap)
 */
data class PostImage(
    val uri: Uri? = null,         //图片（相册选图用）
    val bitmap: Bitmap? = null,   //图片位图（相机拍照用）
    val id: String = UUID.randomUUID().toString() //ID
)

/**
 * 代表一个话题或位置，后边有用
 */
data class Topic(
    val name: String,
    val type:  Int = 0 // 0: 话题, 1: 位置
)