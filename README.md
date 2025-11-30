DouyinPost - 仿抖音图文投稿工具 📱✨
DouyinPost 是一个高保真还原抖音/小红书“图文投稿”页面的 Android 应用。它不仅实现了流畅的图文编辑交互，更实现了“AI 帮写”、“智能话题推荐”和“基于位置的景点推荐”等智能化功能。

✨ 核心功能

1. 🖼️ 图文管理
   
多图选择：支持从系统相册批量选择图片，或直接调用相机拍摄。

双屏联动：顶部大图预览 (ViewPager2) 与底部缩略图列表 (RecyclerView) 实时联动。

拖拽排序：支持长按底部缩略图进行拖拽排序，大图预览顺序同步更新。

智能删除：删除图片后，自动处理光标位置与选中状态，防止越界。

2. 🤖 AI 赋能
   
AI 帮写文案：上传图片后，点击“AI 帮写”，模型会分析图片内容、标题及现有草稿，自动生成一段幽默、带 Emoji 的抖音风文案。

智能话题生成：根据选中的前 3 张图片，自动识别场景并生成相关的热门话题标签（如 #风景, #美食）。

景点推荐：定位成功后，AI 会根据所在城市自动推荐当地著名的旅游景点或商圈。

##注意：本项目使用Gemini-2.0-flash模型，请用户确保稳定的网络环境，使用自己的api key，测试用api key已私聊老师


3.精确位置服务

原生定位：利用 LocationManager和Geocoder获取当前经纬度并解析为城市名称。

智能备选：结合 AI 与本地逻辑，提供精确街道、城市及周边热门地点的备选列表。


4.✍️ 文本与交互优化

标题限制：标题输入框限制 20 字，超限自动截断并弹窗提示；回车键自动跳转正文。

字数统计：正文实时统计字数，超限变红警示。

软键盘适配：优化动作（下一项/完成），处理正文和父容器的滑动冲突。

富文本插入：支持插入 #话题 和 @好友，并智能处理光标位置。



🛠️ 技术栈

语言: Kotlin

最低兼容: Android 7.0 (API 24)

核心组件:

ViewPager2 + RecyclerView (复杂列表交互)

ConstraintLayout (复杂 UI 布局)

Coroutine (协程 - 处理异步任务与 AI 请求)

ActivityResultContracts (新版页面结果回调 API)

AI 模型: Google Gemini Android SDK

模型版本: gemini-2.0-flash 


🚀 如何运行 (Getting Started)

1. 克隆项目

git clone [https://github.com/kgdz/DouyinPost.git](https://github.com/kgdz/DouyinPost.git)


2. 配置 API Key (重要！) 🔑

本项目依赖 Google Gemini API。为了安全起见，API Key 未上传至代码仓库。
请在运行前，打开 app/src/main/java/com/example/douyinpost/MainActivity.kt，找到以下代码并填入你的 Key：

// MainActivity.kt 第 40 行左右
private val generativeModel = GenerativeModel(
    modelName = "gemini-2.0-flash",
    apiKey = "在此处填入你的_GEMINI_API_KEY" // <--- 修改这里
)


如果没有 Key，请访问 Google AI Studio 免费申请。

3. 编译运行

使用 Android Studio (建议 Ladybug 或更新版本) 打开项目。

等待 Gradle Sync 完成。

连接真机或使用模拟器（需配置代理）运行。

⚠️ 注意事项

网络环境：Gemini API 需要访问 Google 服务器。

真机：请确保手机网络可以访问外网。

模拟器：如果遇到 ConnectTimeout，请检查模拟器的代理设置 (Proxy Host: 10.0.2.2)。

定位功能：

模拟器默认没有 GPS 信号，请在模拟器设置 (Extended Controls -> Location) 中手动设置一个坐标（如北京/上海），否则无法获取位置。

📝 开发心得

在大作业开发过程中，主要解决了以下技术难点：

ViewPager2 与 RecyclerView 的双向联动：通过监听器与状态标志位，解决了滑动冲突和循环更新的问题。

异步操作与 UI 刷新：使用 Kotlin 协程 (lifecycleScope) 将 AI 请求、地理编码等耗时操作放入 IO 线程，保证了主线程的流畅度。

多模态 AI 调用：封装了图片转 Bitmap 的逻辑，实现了图文混合发送给 LLM
