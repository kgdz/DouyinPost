package com.example.douyinpost

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log // 引入 Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.text.TextWatcher
import android.text.Editable
import androidx.core.widget.addTextChangedListener
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {
    private val imageList = mutableListOf<PostImage>()
    private lateinit var previewAdapter: ImagePreviewAdapter //大图适配器
    private lateinit var thumbnailAdapter: ImageThumbnailAdapter//小图适配器
    private lateinit var vpImagePreview: ViewPager2//大图控件
    private lateinit var rvImageThumbnails: RecyclerView//小图控件
    private lateinit var etTitle: EditText//标题
    private lateinit var etContent: EditText//内同正文
    private lateinit var tvCharCount: TextView//字数统计
    private lateinit var btnClose: ImageView//左上角返回
    private lateinit var btnPreview: TextView//右上角预览
    private lateinit var btnEditCover: TextView//编辑封面按钮，仅限第一张大图显示
    private lateinit var btnAIWrite: TextView //AI帮写按钮
    private lateinit var btnAtFriends: TextView //@朋友
    private lateinit var rvHotTopics: RecyclerView//话题列表
    private lateinit var btnTopic: TextView//话题按钮
    private val hotTopics = mutableListOf("#热门挑战", "#风景", "#美食", "#自拍", "#猫咪", "#emo", "#上分")//默认热点话题
    //ai模型初始化
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = "your api key"//密钥写在readme文件里了，有效期7天，1美元额度
    )

    //region --- 图片选择与相机模块 ---
    //相册
    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                imageList.add(PostImage(uri = uri))
            }
            previewAdapter.notifyDataSetChanged() //刷新
            thumbnailAdapter.notifyDataSetChanged()
            val newIndex = imageList.size - uris.size //自动切到最新的那张图
            if (newIndex >= 0) {//然后大图切换，小图选中它
                vpImagePreview.currentItem = newIndex
                thumbnailAdapter.selectedPosition = newIndex
            }
            generateTopicsByAI()//自动生成标签
        }
    }
    //相机启动
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageList.add(PostImage(bitmap = bitmap))
        }
        previewAdapter.notifyDataSetChanged()
        thumbnailAdapter.notifyDataSetChanged()
        val newIndex = imageList.lastIndex
        vpImagePreview.currentItem = newIndex
        thumbnailAdapter.selectedPosition = newIndex
        generateTopicsByAI()//根据拍照生成标签
    }
    //点击加号选择拍张照还是从相册里选
    private fun showAddImageDialog() { val options = arrayOf("从相册选择", "拍摄一张")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("添加图片")
            .setItems(options) { _,which -> when (which) {
                0 -> pickImagesLauncher.launch("image/*")//照片
                1 -> takePictureLauncher.launch(null)}}//拍照
                .show()
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        initViews()
        setupAdapters()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    //region --- UI 初始化与交互逻辑 ---
    private fun initViews() {//绑定控件
        rvHotTopics = findViewById(R.id.recycler_view_hot_topics)//横向滚动的话题选择栏
        tvCharCount = findViewById(R.id.tv_char_count)//字数统计
        vpImagePreview = findViewById(R.id.vp_image_preview)//大图预览
        rvImageThumbnails = findViewById(R.id.rv_image_thumbnails)//小缩略图
        etTitle=findViewById(R.id.et_title)//标题
        etContent = findViewById(R.id.et_content)//内容
        btnClose = findViewById(R.id.btn_close)//关闭按钮
        btnPreview = findViewById(R.id.btn_preview)//预览
        btnEditCover = findViewById(R.id.btn_edit_cover) //编辑封面
        btnAIWrite = findViewById(R.id.btn_ai_write) //AI帮写按钮
        btnAtFriends = findViewById(R.id.btn_at_friends) //@朋友按钮
        btnTopic = findViewById(R.id.btn_topic)//#话题按钮
        val tvLocation: TextView = findViewById(R.id.tv_location)//你在哪里
        val rvNearbyLocations: RecyclerView = findViewById(R.id.rv_nearby_locations)
        val tvPrivacy: TextView = findViewById(R.id.tv_privacy)//隐私选项
        val btnZoom: ImageView = findViewById(R.id.btn_zoom);var isZoomed = false//放大扩展正文部分,并为按钮初始化，默认普通模式
        //初始化
        //如果在标题那一栏里面按回车，直接跳到正文的文末。
        etTitle.setOnEditorActionListener { _, actionId, event ->
            //按回车后触发，防止抬起后触发两次
            val isNext = actionId == EditorInfo.IME_ACTION_NEXT
            val isEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (isNext || isEnter) {
                etContent.requestFocus()
                etContent.setSelection(etContent.length())//光标定位到文末
                true
            } else {
                false
            }
        }
        //标题超过20字自动截断，而且弹出提示
        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 20) {
                    s.delete(20, s.length)//把第21个字及后面的全部删掉
                    //弹警告
                    Toast.makeText(this@MainActivity, "标题最多只能输入20个字哦", Toast.LENGTH_SHORT).show()
                     etTitle.setSelection(20) //偶尔光标会乱跑，不烧脑了
                }
            }
        })
        //实时监控正文字数，并做好提示
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentCount = s?.length ?: 0
                tvCharCount.text = "$currentCount/50"//实时显示字数
                //超字数变红
                if (currentCount > 50) {
                    tvCharCount.setTextColor(Color.RED)
                } else {
                    tvCharCount.setTextColor(Color.WHITE)
                }
            }
        })
        //由于正文部分限制了最大高度，所以要处理正文部分滑动和外面的父容器的滑动冲突。
        etContent.setOnTouchListener { v, event ->
            if (v.id == R.id.et_content) {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
        //ai帮写
        btnAIWrite.setOnClickListener {
            performAIWrite()
        }
        //@朋友(Mock 数据)
        btnAtFriends.setOnClickListener {
            val mockUsers = arrayOf("牢大", "老二", "老三", "老四", "老五")
            //底部弹出选择栏
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val listView = android.widget.ListView(this)
            listView.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, mockUsers)
            listView.setOnItemClickListener { _, _, position, _ ->
                val user = mockUsers[position]
                val tag = "@$user "
                //避免重复@
                if (etContent.text.contains(tag)) {
                    Toast.makeText(this, "你已经@过Ta啦", Toast.LENGTH_SHORT).show()
                } else {
                    val start = etContent.selectionStart
                    etContent.text.insert(start, tag)
                    bottomSheetDialog.dismiss()
                }
            }
            bottomSheetDialog.setContentView(listView)
            bottomSheetDialog.show()
        }

        btnClose.setOnClickListener { finish() } //点叉关闭
        btnPreview.setOnClickListener { //记得有空做个跳转，意思意思
            Toast.makeText(this, "功能待开发", Toast.LENGTH_SHORT).show()
        }
        btnEditCover.setOnClickListener {
             Toast.makeText(this, "封面编辑功能待开发", Toast.LENGTH_SHORT).show()
        }
        //定位
        tvLocation.setOnClickListener {
            //检查并请求权限
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                return@setOnClickListener
            }
            LocationHelper.getCurrentCity(this, object : LocationHelper.LocationCallback {
                override fun onCityFound(city: String) {
                    runOnUiThread {
                        //显示城市名
                        tvLocation.text = "📍 $city"
                        //根据城市名，让ai推荐几个该城市的著名地点
                        generateAttractionsByAI(city, rvNearbyLocations)
                    }
                }
                override fun onFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "定位失败: $error", Toast.LENGTH_SHORT).show()
                        tvLocation.text = "📍 定位失败，请稍后再试"
                    }
                }
            })
        }
        //隐私选项
        tvPrivacy.setOnClickListener {
            val options = arrayOf("公开·所有人可见", "互相关注的人可见","私密·仅自己可见", "部分可见")
            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val listView = android.widget.ListView(this)
            listView.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
            listView.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0, 1 -> { //公开或互相关注
                        tvPrivacy.text = "\uD83D\uDC65 ${options[position]}"
                        bottomSheet.dismiss()
                    }
                    2 -> {
                        tvPrivacy.text = "\uD83D\uDD12 ${options[position]}"
                        bottomSheet.dismiss()
                        Toast.makeText(this, "即使是私密内容也请遵守国家法律法规哦~", Toast.LENGTH_SHORT).show()
                    }
                    3 ->{//部分可见就直接复用@朋友的逻辑
                        tvPrivacy.text = "🔐 ${options[position]}"
                        bottomSheet.dismiss() // 先关掉当前的
                        btnAtFriends.performClick() //直接触发@朋友按钮的点击事件
                    }
                }
            }
            bottomSheet.setContentView(listView)
            bottomSheet.show()
        }
        //放大正文部分的空间
        btnZoom.setOnClickListener {
            isZoomed = !isZoomed//点击之后切换状态
            if (isZoomed) {//放大
                etContent.maxLines = 20
                etContent.minLines = 10
                btnZoom.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)//图标变叉号
                //隐藏下面的话题栏让用户聚焦于正文创作
                rvHotTopics.visibility = View.GONE
            } else {
                //恢复！
                etContent.maxLines = 5
                etContent.minLines = 2
                btnZoom.setImageResource(android.R.drawable.ic_menu_crop)//图标变回来
                rvHotTopics.visibility = View.VISIBLE
            }
        }


    }
    //用ai根据所在地址生成本市的推荐景点
    private fun generateAttractionsByAI(city: String, recyclerView: RecyclerView) {
        val prompt = "我现在在 $city，请推荐 6 个当地最著名的景点或商圈，只返回名字，用中文逗号分隔，不要换行。例如：故宫,三里屯,环球影城"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (text != null) {
                    //解析拆分ai的反馈
                    val spots = text.split("，", ",").map { "📍${it.trim()}" }.toMutableList()
                    spots.add(0, "📍 $city")//把所在城市排第一个
                    withContext(Dispatchers.Main) {
                        //因为跟话题的横向滑动栏是复用的，但是这里点击之后不能消失，所以加个参数
                        recyclerView.adapter = ChipAdapter(spots,autoRemove = false) { spotName ->
                            //点击任何一项，就替换“你在哪里”的文字
                            val tvLocation: TextView = findViewById(R.id.tv_location)
                            tvLocation.text = spotName
                        }
                        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAI", "Location AI failed", e)
                //出问题了，显示不了景点，那至少显示这个城市名字
                withContext(Dispatchers.Main) {
                    val fallbackList = mutableListOf("📍 $city")
                    recyclerView.adapter = ChipAdapter(fallbackList) { name ->
                        findViewById<TextView>(R.id.tv_location).text = name
                    }
                }
            }
        }
    }
    //endregion

    //region --- AI 核心功能模块 ---
    //ai帮写的实现，支持多图，把uri转换成bitmap再发送，利用提示词约束好
    private fun performAIWrite() {
        val title = etTitle.text.toString()
        val currentContent = etContent.text.toString()
        val promptText = "请帮我写一段抖音风格的短视频文案。标题是：'$title'，目前我想到的内容是：'$currentContent'。要求：结合我发的所有图片内容，简短、有趣、吸引人、带emoji表情。不要太长，未经特殊要求，字数控制在40字左右。直接回复文案内容，禁止输出无关信息"
        etContent.hint = "请稍后，亮眼文案马上就来..."
        Toast.makeText(this, "AI正在创作中...", Toast.LENGTH_SHORT).show()
        
        Log.d("GeminiAI", "Requesting AI with images count: ${imageList.size}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //如果是相机拍摄的图，直接传，
                val bitmaps = imageList.mapNotNull { postImage ->
                    if (postImage.bitmap != null) {
                        postImage.bitmap
                    } else if (postImage.uri != null) {
                        loadBitmapFromUri(postImage.uri)//如果是相册里面的，转成bitmap再传
                    } else {
                        null
                    }
                }
                //支持多模态，直接图文一起传
                val inputContent = content {
                    for (bmp in bitmaps) {
                        image(bmp)
                    }
                    text(promptText)
                }
                //发送请求
                val response = generativeModel.generateContent(inputContent)
                val aiText = response.text
                //调试信息
                Log.d("GeminiAI", "Success: $aiText")
                withContext(Dispatchers.Main) {
                    if (aiText != null) {
                        etContent.setText(aiText)
                        etContent.setSelection(aiText.length)
                    } else {
                        Toast.makeText(this@MainActivity, "AI 没话说了", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiAI", "Error calling Gemini API", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "AI 出错了: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    etContent.hint = "添加作品描述……"
                }
            }
        }
    }
    //相册里面的返回的是uri，所以要转换成bitmap
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            Log.e("GeminiAI", "Failed to load image: $uri", e)
            null
        }
    }
    //用ai帮忙生成标签
    private fun generateTopicsByAI() {
        if (imageList.isEmpty()) return//只有在用户放入图片后才运行，静默运行，不弹窗不提示
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmaps = imageList.take(3).mapNotNull { postImage -> // 只取前3张省流量
                    if (postImage.bitmap != null) postImage.bitmap
                    else if (postImage.uri != null) loadBitmapFromUri(postImage.uri)
                    else null
                }
                if (bitmaps.isEmpty()) return@launch
                //提示词发请求
                val inputContent = content {
                    for (bmp in bitmaps) image(bmp)
                    text("请根据这些图片，生成 15 个相关的抖音话题标签。直接返回标签，用中文逗号或英文逗号分隔，不要带任何其他解释性文字。例如：#风景,#旅行,#OOTD")
                }
                val response = generativeModel.generateContent(inputContent)
                val text = response.text ?: ""
                //根据ai返回的结果解析拆分
                val newTags = text.split(",", "，", " ").map { it.trim() }.filter { it.startsWith("#") }
                withContext(Dispatchers.Main) {
                    if (newTags.isNotEmpty()) {
                        hotTopics.clear()
                        hotTopics.addAll(newTags)
                        rvHotTopics.adapter?.notifyDataSetChanged()//刷新列表
                    }
                }
            } catch (e: Exception) {
                //失败了就闭嘴，假装无事发生，沿用之前的热点话题
            }
        }
    }
    //endregion

    //region --- 适配器与列表逻辑 ---
    private fun setupAdapters() {//配置适配器
        previewAdapter = ImagePreviewAdapter(imageList)//配置大图
        vpImagePreview.adapter = previewAdapter
        thumbnailAdapter = ImageThumbnailAdapter(//配置小图
            images = imageList,
            // 小图点哪个，大图切哪个
            onImageClick = { position ->
                vpImagePreview.currentItem = position
            },
            //点加号新增图片，由用户选择拍照或者从相册选择
            onAddClick = {
                showAddImageDialog()
            },
            //点叉号删除
            onDeleteClick = { position ->
                if (position in imageList.indices) {
                    imageList.removeAt(position)
                    previewAdapter.notifyDataSetChanged()
                    thumbnailAdapter.notifyDataSetChanged()
                    //防止下标越界
                    if (imageList.isNotEmpty()) {
                        val newPos = if (position >= imageList.size) imageList.size - 1 else position
                        thumbnailAdapter.selectedPosition = newPos
                        vpImagePreview.currentItem = newPos
                    }
                }
            }
        )

        //设置小图列表为横向滚动
        rvImageThumbnails.adapter = thumbnailAdapter
        rvImageThumbnails.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        //大小图联动
        vpImagePreview.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position in imageList.indices) {
                    // 更新选中位置，让红框动起来
                    thumbnailAdapter.selectedPosition = position
                    thumbnailAdapter.notifyDataSetChanged()
                    rvImageThumbnails.smoothScrollToPosition(position)//优化体验 ，确保高亮图可见
                }
                // 编辑封面按钮的控制，只有在第0页时显示，其他时候隐藏
                if (position == 0 && imageList.isNotEmpty()) {
                    btnEditCover.visibility = View.VISIBLE
                } else {
                    btnEditCover.visibility = View.INVISIBLE
                }
            }
        })
        //拖拽排序
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (viewHolder is ImageThumbnailAdapter.AddViewHolder) return 0 //加号不许拽
                val dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT//允许左右拽
                return makeMovementFlags(dragFlags, 0)
            }
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                //如果试图拖到加号按钮的位置，或者目标是加号，则禁止
                if (target is ImageThumbnailAdapter.AddViewHolder) return false
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                thumbnailAdapter.onItemMove(fromPos, toPos)
                previewAdapter.notifyDataSetChanged()
                //让大图跟着被拖拽的项走,如果当前显示的就是被拖拽的这张图，或者目标位置变成了当前位置
                //直接让ViewPager切到toPos
                vpImagePreview.setCurrentItem(toPos, false)
                thumbnailAdapter.selectedPosition = toPos//更新高亮边框
                thumbnailAdapter.notifyDataSetChanged()
                // 拖拽后，如果第0张换了，更新编辑封面那个按钮的状态
                if (vpImagePreview.currentItem == 0){
                    btnEditCover.visibility = View.VISIBLE
                }
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 这里预留给“侧滑删除”，有时间做没时间算了
            }
        })
        // 绑定到小图列表上
        itemTouchHelper.attachToRecyclerView(rvImageThumbnails)
        //热门话题，点击弹出话题选择栏
        btnTopic.setOnClickListener {
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val listView = android.widget.ListView(this)
            //复用hotTopics数据，用的是hotTopics的副本，防止弹窗里删了影响外面，或者反之
            val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, hotTopics)
            listView.adapter = adapter
            listView.setOnItemClickListener { _, _, position, _ ->
                val tag = hotTopics[position]
                val start = etContent.selectionStart
                etContent.text.insert(start, "$tag ")
                bottomSheetDialog.dismiss()

                //同时也从外面的横向栏里移除
            hotTopics.removeAt(position)
            rvHotTopics.adapter?.notifyDataSetChanged()
            }
            bottomSheetDialog.setContentView(listView)
            bottomSheetDialog.show()
        }
        //横向栏选择
        rvHotTopics.adapter = ChipAdapter(hotTopics) { text ->
            //插入话题到光标处
            val start = etContent.selectionStart
            etContent.text.insert(start, "$text ")
            //点击后消失
            hotTopics.remove(text)
            rvHotTopics.adapter?.notifyDataSetChanged()
        }
        rvHotTopics.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

    }
    //endregion
}