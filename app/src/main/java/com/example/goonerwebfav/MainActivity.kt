package com.example.goonerwebfav

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: WebsiteAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var topPanel: LinearLayout
    private lateinit var bottomPanel: LinearLayout
    private lateinit var infoBar: TextView
    private lateinit var websiteList: RecyclerView
    private lateinit var zoneTabsContainer: LinearLayout
    private lateinit var previewContainer: FrameLayout

    private lateinit var galleryOverlay: LinearLayout
    private lateinit var rvGallery: RecyclerView
    private lateinit var tvGalleryTitle: TextView

    private lateinit var webProgressBar: ProgressBar



    private lateinit var previewPlaceholder: LinearLayout
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var sbBuiltinContainer: LinearLayout
    private lateinit var ivBackground: ImageView



    private var fabContainer: View? = null
    private var sidebarToggle: View? = null

    private var currentZone = "默认分区"
    private var currentFilter = "all"
    private var searchKeyword = ""
    private var excludeVisitedOnly = false
    private var allZones = listOf<ZoneEntity>()
    private var allWebsitesInZone = listOf<WebsiteEntity>()

    private var isSelectMode = false
    private val selectedUrls = mutableSetOf<String>()
    private var batchBar: LinearLayout? = null

    private var webView1: WebView? = null
    private var webView2: WebView? = null
    private var layoutMode = "single"
    private var activePane = 1
    private var sharedZoom = 1.0f
    private var lastClickTime = 0L
    private var failureView: LinearLayout? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var pauseBg = true
    private var isFullScreenMode = false
    private var lastFullScreenToggleTime = 0L
    private var unloadHidden = true
    private var disableScale = false

    private var adBlockEnabled = true
    private val defaultAdHosts = listOf(
        // 1. 全球弹窗/重定向巨头（导致网页乱跳的罪魁祸首）
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "googletagservices.com",
        "adnxs.com", "rubiconproject.com", "pubmatic.com", "openx.net", "criteo.com",
        "moatads.com", "adcolony.com", "unityads.unity3d.com", "applovin.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com", "onclickads.net",
        "exoclick.com", "exosrv.com", "exdynsrv.com", "juicyads.com", "juicyads.net",
        "trafficjunky.net", "trafficjunky.com", "trafficfactory.biz", "tsyndicate.com",
        "trafficstars.com", "clickadu.com", "hilltopads.net", "adcash.com", "admaven.com",
        "zeroredirect.com", "zedo.com", "revcontent.com", "mgid.com", "adskeeper.com",

        // 2. 原生信息流/恶心横幅推荐（"你可能感兴趣"）
        "taboola.com", "outbrain.com", "media.net", "adroll.com", "bidswitch.net",
        "smartadserver.com", "contextweb.com", "yieldmo.com", "sovrn.com", "lijit.com",

        // 3. 统计、追踪器与反爬（有些站不放行这个就不给看，但很多会拖慢速度）
        "google-analytics.com", "googletagmanager.com", "quantserve.com", "scorecardresearch.com",
        "hotjar.com", "mouseflow.com", "crazyegg.com", "yandex.ru/metrika", "facebook.net",
        "connect.facebook.net", "analytics.twitter.com", "histats.com", "statcounter.com",

        // 4. 成人/影视/盗版站重灾区专属（Exoclick 马甲及其他）
        "adnium.com", "ero-advertising.com", "ero-advertising.net", "clickaine.com",
        "trafficshop.com", "plugrush.com", "adnium.net", "reporo.net", "pemsrv.com",
        "tsyndicate.net", "adnium.com", "creativcdn.net", "a.adtng.com", "hwcdn.net",

        // 5. 恶意下载/博彩/病毒跳转
        "adf.ly", "shorte.st", "ouo.io", "shrinkme.io", "cpmlink.net", "cuty.io",
        "bet365.com", "1xbet.com", "xbet.com", "22bet.com", "mostbet.com", "melbet.com",

        // 6. 常见广告资源后缀特征（直接拦截带有这些字眼的域名）
        "adserver.", "adservice.", "banner.", "popunder.", "ad-delivery.", "adsystem."
    )
    private var customAdRuleList = mutableListOf<String>()
    private var adWhiteList = mutableSetOf<String>()

    private var currentEngineIndex = 0
    private val engineNames = arrayOf("Google 翻译", "Yandex 翻译", "Bing 翻译代理", "自定义 API 代理")
    private lateinit var prefs: android.content.SharedPreferences

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (filePathCallback == null) return@registerForActivityResult
        var results: Array<Uri>? = null
        if (result.resultCode == RESULT_OK) {
            val dataString = result.data?.dataString
            val clipData = result.data?.clipData
            if (dataString != null) results = arrayOf(Uri.parse(dataString))
            else if (clipData != null) {
                val list = ArrayList<Uri>()
                for (i in 0 until clipData.itemCount) list.add(clipData.getItemAt(i).uri)
                results = list.toTypedArray()
            }
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            try { prefs.edit().putString("custom_bg_uri", uri.toString()).apply(); loadCustomBackground(); Toast.makeText(this, "背景已更新", Toast.LENGTH_SHORT).show() }
            catch (e: Exception) { Toast.makeText(this, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private var jsonExportContent = ""
    private val createJsonLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.openOutputStream(uri)?.use { it.write(jsonExportContent.toByteArray()) }; Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show() }
            catch (e: Exception) { Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show() }
        }
    }
    private val openJsonLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { val inputStream = contentResolver.openInputStream(uri); val reader = BufferedReader(InputStreamReader(inputStream)); val jsonStr = reader.readText(); reader.close(); importDataFromJson(jsonStr) }
            catch (e: Exception) { Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private var htmlExportContent = ""
    private val createHtmlLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.openOutputStream(uri)?.use { it.write(htmlExportContent.toByteArray()) }; Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show() }
            catch (e: Exception) { Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show() }
        }
    }
    private val openHtmlLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { val inputStream = contentResolver.openInputStream(uri); val reader = BufferedReader(InputStreamReader(inputStream)); val htmlStr = reader.readText(); reader.close(); importHtmlBookmarks(htmlStr) }
            catch (e: Exception) { Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show() }
        }
    }

    private var isRandomBtnFixed = false // 随机按钮是否固定显示

    private var currentPreviewUrl: String? = null

    private var appLockPassword: String? = null
    private var lastPauseTime: Long = 0L

    private val sniffedVideoUrls = mutableSetOf<String>()

    private var lastVisitedUrl: String? = null

    private var autoJob: Job? = null
    private var isAutoRunning = false
    private val randomPlaylist = mutableListOf<WebsiteEntity>()
    private var autoMinSec = 10
    private var autoMaxSec = 20
    private var floatTag = "FLOAT_WINDOW_TAG" // 【新增】浮窗识别标签

    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideUiRunnable = Runnable {
        fabContainer?.let { it.animate().alpha(0f).setDuration(500).withEndAction { it.visibility = View.GONE }.start() }
        sidebarToggle?.let { it.animate().alpha(0f).setDuration(500).withEndAction { it.visibility = View.GONE }.start() }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // === 防小米侧滑误退：设置系统手势排除区 ===
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val rootView = window.decorView
            rootView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val exclusionRects = mutableListOf<android.graphics.Rect>()
                val density = resources.displayMetrics.density
                val exclusionWidth = (30 * density).toInt() // 30dp的宽度，拦截更宽泛

                // 左边边缘的排斥区
                exclusionRects.add(android.graphics.Rect(left, top, left + exclusionWidth, bottom))
                // 右边边缘的排斥区
                exclusionRects.add(android.graphics.Rect(right - exclusionWidth, top, right, bottom))

                v.systemGestureExclusionRects = exclusionRects
            }
        }
        db = AppDatabase.getDatabase(this)
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentEngineIndex = prefs.getInt("translate_engine_idx", 0)
        appLockPassword = prefs.getString("app_lock_pwd", null)
        adBlockEnabled = prefs.getBoolean("ad_block_enabled", true)
        loadCustomAdRules()
        loadAdWhiteList()
        initViews()
        setupWebViews()
        setupListeners()
        setupBackPressedHandler()
        loadData()


        resetUiTimer()
        handleSharedIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        prefs.edit().apply {
            putLong("last_exit_time", System.currentTimeMillis())
            putString("last_zone", currentZone)
            putString("last_layout", layoutMode)
            putInt("last_active_pane", activePane)
            putString("last_url1", webView1?.url)
            putString("last_url2", webView2?.url)
            apply()
        }

        lastPauseTime = System.currentTimeMillis()

    }

    override fun onPause() {
        super.onPause()
        // 暂停所有网页渲染和音频播放
        webView1?.onPause()
        webView2?.onPause()
    }

    override fun onResume() {
        super.onResume()
        // 恢复网页渲染
        webView1?.onResume()
        webView2?.onResume()

        if (appLockPassword != null) {
            if (lastPauseTime == 0L || System.currentTimeMillis() - lastPauseTime > 5000) {
                findViewById<LinearLayout>(R.id.lockOverlay).visibility = View.VISIBLE
                //findViewById<EditText>(R.id.etUnlockPwd).requestFocus()
            }
        }

    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()



        // 1. 移除所有 UI 定时器，防止内存泄漏
        uiHandler.removeCallbacks(hideUiRunnable)

        // 2. 安全销毁 WebView1
        webView1?.let { web ->
            (web.parent as? ViewGroup)?.removeView(web)
            web.stopLoading()
            web.clearHistory()
            web.clearCache(true)
            web.removeAllViews()
            web.destroy()
        }
        webView1 = null

        // 3. 安全销毁 WebView2
        webView2?.let { web ->
            (web.parent as? ViewGroup)?.removeView(web)
            web.stopLoading()
            web.clearHistory()
            web.clearCache(true)
            web.removeAllViews()
            web.destroy()
        }
        webView2 = null

        // 4. 清理可能残留的全屏 View
        if (customView != null) {
            (customView?.parent as? ViewGroup)?.removeView(customView)
            customView = null
            customViewCallback = null
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        topPanel = findViewById(R.id.topPanel)
        bottomPanel = findViewById(R.id.bottomPanel)
        infoBar = findViewById(R.id.infoBar)
        websiteList = findViewById(R.id.websiteList)
        zoneTabsContainer = findViewById(R.id.zoneTabsContainer)
        previewContainer = findViewById(R.id.previewContainer)
        galleryOverlay = findViewById(R.id.galleryOverlay)
        rvGallery = findViewById(R.id.rvGallery)
        tvGalleryTitle = findViewById(R.id.tvGalleryTitle)

// 设置画廊为双列瀑布流
        rvGallery.layoutManager = androidx.recyclerview.widget.StaggeredGridLayoutManager(2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL)
        webProgressBar = findViewById(R.id.webProgressBar)
        previewPlaceholder = findViewById(R.id.previewPlaceholder)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        sbBuiltinContainer = findViewById(R.id.sbBuiltinList)
        fabContainer = findViewById(R.id.fabContainer)
        sidebarToggle = findViewById(R.id.sidebarToggle)
        findViewById<TextView>(R.id.btnCloseGallery)?.setOnClickListener {
            galleryOverlay.visibility = View.GONE
        }
        ivBackground = findViewById(R.id.ivBackground)
        adapter = WebsiteAdapter()
        websiteList.layoutManager = LinearLayoutManager(this)
        websiteList.adapter = adapter
    }

    private fun loadCustomBackground() {
        val customUriStr = prefs.getString("custom_bg_uri", null)
        if (customUriStr != null) {
            try {
                val uri = Uri.parse(customUriStr)
                val options = BitmapFactory.Options()

                // 1. 第一次读取：只获取图片的宽高，不加载进内存
                options.inJustDecodeBounds = true
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

                // 2. 计算缩放比例（限制最大宽高不超过屏幕尺寸）
                val maxWidth = resources.displayMetrics.widthPixels
                val maxHeight = resources.displayMetrics.heightPixels
                var inSampleSize = 1
                if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                        inSampleSize *= 2
                    }
                }

                // 3. 第二次读取：带着计算好的缩放比例，安全地加载图片
                val realOptions = BitmapFactory.Options()
                realOptions.inSampleSize = inSampleSize
                val bitmap = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, realOptions)
                }

                if (bitmap != null) {
                    ivBackground.setImageBitmap(bitmap)
                    ivBackground.visibility = View.VISIBLE
                    return
                }
            } catch (e: Exception) {
                // 读取失败则安全回退
            }
        }
        ivBackground.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebViews() {
        webView1 = createWebView(1)
        webView2 = createWebView(2)
    }

    private fun createWebView(paneNum: Int): WebView {
        val webView = WebView(this)
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        webView.layoutParams = FrameLayout.LayoutParams(-1, -1)
        val settings = webView.settings
        settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = true; settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true); settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.useWideViewPort = true; settings.loadWithOverviewMode = true
        settings.allowFileAccess = true; settings.allowContentAccess = true
        settings.builtInZoomControls = !disableScale; settings.displayZoomControls = false
        // 1. 允许第三方 Cookie，解决跨域登录失效、账号无法保存问题
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        CookieManager.getInstance().setAcceptCookie(true)

// 2. 伪装成普通手机浏览器，防止部分网站因为识别到 WebView 而拒绝登录
        settings.userAgentString = settings.userAgentString.replace("; wv", "")

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, null, null))
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this@MainActivity, "已保存到下载目录", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@MainActivity, "下载失败", Toast.LENGTH_SHORT).show() }
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                // 👇 【核心修复：先把视频抓了，不受广告拦截开关影响】 👇
                val originalUrl = request?.url?.toString() ?: ""
                val checkUrl = originalUrl.lowercase()
                if (checkUrl.contains(".mp4") || checkUrl.contains(".m3u8")) {
                    sniffedVideoUrls.add(originalUrl)
                }
                // 👆 抓取结束 👆

                // 如果没开广告拦截，后面的代码直接不执行了
                if (!adBlockEnabled) return super.shouldInterceptRequest(view, request)

                try {
                    val requestHost = request?.url?.host ?: ""
                    for (whiteHost in adWhiteList) {
                        if (requestHost.contains(whiteHost)) {
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                } catch (e: Exception) {}

                // 如果网址为空，直接放行
                if (checkUrl.isEmpty()) return super.shouldInterceptRequest(view, request)

                // 1. 检查内置黑名单
                for (host in defaultAdHosts) {
                    if (checkUrl.contains(host)) {
                        return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream("".toByteArray()))
                    }
                }

                // 2. 检查自定义黑名单
                if (customAdRuleList.isNotEmpty()) {
                    for (rule in customAdRuleList) {
                        if (checkUrl.contains(rule)) {
                            return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream("".toByteArray()))
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                removeFailureView(); bottomNavBar.visibility = View.VISIBLE; adjustFabPosition(true)
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) showFailureView(request.url.toString())
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && !url.startsWith("about:") && !url.startsWith("data:")) {
                    updateVisitCountAndTitle(url, view?.title ?: "")
                    if (adBlockEnabled) {
                        val antiAdBlockJs = """
        (function(){
            var selectors = [
                '.adblock-warning', '#adblock-warning', '.ad-block-message', '.adblock-notice', '.adblock-popup', '.adblock-modal', '.adblock-overlay', '.adblock-dialog',
                'div[class*="adblock"]', 'div[id*="adblock"]', 'div[class*="ad-block"]', 'div[id*="ad-block"]', 'div[class*="anti-ad"]', 'div[id*="anti-ad"]'
            ];
            selectors.forEach(function(s) {
                var els = document.querySelectorAll(s);
                for (var i = 0; i < els.length; i++) {
                    var el = els[i];
                    if (el.innerText && (el.innerText.includes('广告') || el.innerText.includes('拦截') || el.innerText.includes('Adblock') || el.innerText.includes('AdBlock'))) {
                        el.style.setProperty('display', 'none', 'important');
                    }
                }
            });
        })();
    """.trimIndent()
                        view?.evaluateJavascript(antiAdBlockJs, null)
                    }
                    if (layoutMode == "single") activePane = 1 else activePane = paneNum
                }
            }
            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                Toast.makeText(this@MainActivity, "页面崩溃，正在重建...", Toast.LENGTH_SHORT).show()
                if (activePane == 1) { webView1?.destroy(); webView1 = createWebView(1) } else { webView2?.destroy(); webView2 = createWebView(2) }
                switchLayout(layoutMode); return true
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("intent://") || url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || url.startsWith("alipays://") || url.startsWith("weixin://")) {
                    try { startActivity(Intent.parseUri(url, Intent.URI_INTENT_SCHEME)) } catch (e: Exception) { Toast.makeText(this@MainActivity, "未安装对应 App", Toast.LENGTH_SHORT).show() }
                    return true
                }
                // 拦截广告常用的空白页和无效跳转
                if (url == "about:blank" || url == "javascript:void(0)" || url == "javascript:;") {
                    return true // 直接吃掉，不让它跳转
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                val isCurrentWebView = if (activePane == 1) view == webView1 else view == webView2

                if (isCurrentWebView) {
                    if (newProgress == 100) {
                        webProgressBar.visibility = View.GONE
                    } else {
                        webProgressBar.visibility = View.VISIBLE
                        webProgressBar.progress = newProgress
                    }
                }
            }
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) { callback?.onCustomViewHidden(); return }
                customView = view; customViewCallback = callback
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                topPanel.visibility = View.GONE; previewContainer.visibility = View.VISIBLE
                bottomNavBar.visibility = View.GONE; fabContainer?.visibility = View.GONE; sidebarToggle?.visibility = View.GONE
                previewContainer.addView(customView, FrameLayout.LayoutParams(-1, -1))
            }
            override fun onHideCustomView() {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (customView != null) { previewContainer.removeView(customView); customView = null }
                customViewCallback?.onCustomViewHidden(); customViewCallback = null
                topPanel.visibility = View.VISIBLE; bottomNavBar.visibility = View.VISIBLE
                fabContainer?.visibility = View.VISIBLE; sidebarToggle?.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({ webView1?.requestLayout(); webView1?.invalidate(); webView2?.requestLayout(); webView2?.invalidate() }, 100)
            }
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                // 1. 取消上一次未完成的回调
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                try {
                    // 2. 尝试使用系统提供的标准方式创建 Intent
                    val intent = fileChooserParams?.createIntent()
                    if (intent != null) {
                        fileChooserLauncher.launch(intent)
                        return true
                    }
                } catch (e: Exception) {
                    // 忽略错误，进入备用方案
                }

                try {
                    // 3. 备用方案：如果系统 Intent 失败，手动创建一个兼容所有手机的 Intent
                    val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT)
                    fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    fallbackIntent.type = "*/*" // 允许选择所有文件

                    // 如果网页指定了只能选图片或视频，我们尽量适配
                    if (fileChooserParams?.acceptTypes != null && fileChooserParams.acceptTypes.isNotEmpty()) {
                        val acceptType = fileChooserParams.acceptTypes[0]
                        if (acceptType.isNotEmpty() && acceptType != "*/*") {
                            fallbackIntent.type = acceptType
                        }
                    }

                    fileChooserLauncher.launch(fallbackIntent)
                    return true
                } catch (e: Exception) {
                    // 4. 彻底失败，恢复状态防止网页卡死
                    this@MainActivity.filePathCallback = null
                    return false
                }
            }
            override fun onPermissionRequest(request: PermissionRequest?) { runOnUiThread { request?.let { it.grant(it.resources) } } }
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage(message).setPositiveButton("确定") { _, _ -> result?.confirm() }.setCancelable(false).show(); return true
            }
            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage(message).setPositiveButton("确定") { _, _ -> result?.confirm() }.setNegativeButton("取消") { _, _ -> result?.cancel() }.show(); return true
            }
            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                val input = EditText(this@MainActivity).apply { setText(defaultValue) }
                AlertDialog.Builder(this@MainActivity).setMessage(message).setView(input).setPositiveButton("确定") { _, _ -> result?.confirm(input.text.toString()) }.setNegativeButton("取消") { _, _ -> result?.cancel() }.show(); return true
            }


            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                // 如果用户没有主动点击（纯脚本自动弹窗），直接拦截！
                if (!isUserGesture) {
                    return false
                }

                val newWebView = WebView(this@MainActivity)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(v: WebView?, u: String?) {
                        super.onPageFinished(v, u)
                        // 如果加载出来的是空白页，不要覆盖原网页，防止卡死
                        if (u != null && !u.startsWith("about:") && !u.startsWith("javascript:")) {
                            view?.loadUrl(u)
                        }
                    }
                }
                val transport = resultMsg?.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_DOWN) activePane = paneNum; false }
        webView.setOnLongClickListener {
            val result = webView.hitTestResult; val extraUrl = result.extra
            if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) { if (extraUrl != null) showLinkLongClickMenu(extraUrl); return@setOnLongClickListener true }
            if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) { if (extraUrl != null) showImageLongClickMenu(extraUrl); return@setOnLongClickListener true }
            return@setOnLongClickListener false
        }
        return webView
    }




    private fun showAddCurrentPageToFavoriteDialog(url: String, title: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val etUrl = EditText(this).apply {
            hint = "网址"
            setText(url)
            setSingleLine(true)
        }
        val etTitle = EditText(this).apply {
            hint = "标题（可留空）"
            setText(title)
            setSingleLine(true)
        }
        val tvZoneLabel = TextView(this).apply {
            text = "选择分区："
            setTextColor(Color.GRAY)
            setPadding(0, 30, 0, 10)
        }
        val zoneNames = allZones.map { it.name }
        val finalZoneNames = if (zoneNames.isEmpty()) listOf("默认分区") else zoneNames
        var selectedZone = if (finalZoneNames.contains(currentZone)) currentZone else finalZoneNames[0]
        val tvZoneValue = TextView(this).apply {
            text = selectedZone
            setTextColor(0xFFFFD700.toInt())
            setPadding(0, 10, 0, 10)
            textSize = 16f
        }
        tvZoneValue.setOnClickListener {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("选择分区")
                .setSingleChoiceItems(finalZoneNames.toTypedArray(), finalZoneNames.indexOf(selectedZone)) { dialog, which ->
                    selectedZone = finalZoneNames[which]
                    tvZoneValue.text = selectedZone
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        container.addView(etUrl)
        container.addView(etTitle)
        container.addView(tvZoneLabel)
        container.addView(tvZoneValue)
        AlertDialog.Builder(this)
            .setTitle("加入收藏")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val finalUrl = etUrl.text.toString().trim()
                val finalTitle = etTitle.text.toString().trim()
                if (finalUrl.isEmpty()) {
                    Toast.makeText(this@MainActivity, "网址不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                this@MainActivity.lifecycleScope.launch {
                    val resultId = db.appDao().insertWebsite(
                        WebsiteEntity(url = finalUrl, zoneName = selectedZone, title = finalTitle)
                    )
                    if (resultId != -1L) {
                        Toast.makeText(this@MainActivity, "已加入收藏：$selectedZone", Toast.LENGTH_SHORT).show()
                        filterAndDisplay()
                        updateBuiltinSites()
                    } else {
                        Toast.makeText(this@MainActivity, "该网址已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun adjustFabPosition(isUp: Boolean) {
        val fab = fabContainer; val side = sidebarToggle
        if (fab == null || side == null) return
        val fabParams = fab.layoutParams as? ViewGroup.MarginLayoutParams; val sideParams = side.layoutParams as? ViewGroup.MarginLayoutParams
        if (fabParams != null && sideParams != null) {
            if (isUp) { fabParams.bottomMargin = 160; sideParams.bottomMargin = 160 } else { fabParams.bottomMargin = 50; sideParams.bottomMargin = 50 }
            fab.layoutParams = fabParams; side.layoutParams = sideParams
        }
    }

    private fun setupListeners() {

        // 绑定侧边栏的退出按钮
        findViewById<TextView>(R.id.btnExitApp)?.setOnClickListener {
            finish()
        }

        sidebarToggle?.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        findViewById<TextView>(R.id.randomBtn).setOnClickListener { doRandomJump() }
        findViewById<TextView>(R.id.randomBtn).setOnLongClickListener {
            isRandomBtnFixed = !isRandomBtnFixed
            val fabContainer = findViewById<ViewGroup>(R.id.fabContainer)

            if (isRandomBtnFixed) {
                // 开启固定模式：取消隐藏倒计时
                uiHandler.removeCallbacks(hideUiRunnable)

                // 1. 隐藏 fabContainer 里除了随机按钮之外的所有按钮
                if (fabContainer != null) {
                    for (i in 0 until fabContainer.childCount) {
                        val child = fabContainer.getChildAt(i)
                        if (child.id != R.id.randomBtn) {
                            child.visibility = View.GONE
                        }
                    }
                }

                // 2. 【核心修复】单独隐藏左下角的侧边栏按钮
                sidebarToggle?.visibility = View.GONE

                Toast.makeText(this, "已固定【随机】按钮，再次长按恢复", Toast.LENGTH_SHORT).show()
            } else {
                // 恢复模式：把所有按钮都显示出来
                if (fabContainer != null) {
                    for (i in 0 until fabContainer.childCount) {
                        fabContainer.getChildAt(i).visibility = View.VISIBLE
                    }
                }

                // 恢复左下角的侧边栏按钮
                sidebarToggle?.visibility = View.VISIBLE

                Toast.makeText(this, "已恢复全功能面板", Toast.LENGTH_SHORT).show()
                resetUiTimer() // 重新开启 4 秒倒计时
            }
            true
        }
        findViewById<TextView>(R.id.fabAutoBtn).setOnClickListener { toggleAutoJump(it as TextView) }
        findViewById<TextView>(R.id.fabTimeBtn).setOnClickListener {
            val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
            val etMin = EditText(this).apply { hint = "最小秒数"; inputType = InputType.TYPE_CLASS_NUMBER; setText(autoMinSec.toString()) }
            val etMax = EditText(this).apply { hint = "最大秒数"; inputType = InputType.TYPE_CLASS_NUMBER; setText(autoMaxSec.toString()) }
            container.addView(TextView(this).apply { text = "设置自动跳转秒数范围:" }); container.addView(etMin); container.addView(etMax)
            AlertDialog.Builder(this).setTitle("时间设置").setView(container).setPositiveButton("保存") { _, _ ->
                autoMinSec = etMin.text.toString().toIntOrNull() ?: 10; autoMaxSec = etMax.text.toString().toIntOrNull() ?: 20
                if (autoMaxSec < autoMinSec) autoMaxSec = autoMinSec
            }.setNegativeButton("取消", null).show()
        }

        findViewById<TextView>(R.id.tvPreviewFavBtn)?.setOnClickListener {
            val currentWeb = if (activePane == 1) webView1 else webView2
            // 👇 优先使用记录的真实网址，防止因 loadData 导致获取不到网址
            val url = currentPreviewUrl ?: currentWeb?.url
            val title = currentWeb?.title ?: ""

            if (url.isNullOrEmpty() || url.startsWith("about:") || url.startsWith("data:")) {
                Toast.makeText(this, "当前没有打开的网页", Toast.LENGTH_SHORT).show()
            } else {
                showAddCurrentPageToFavoriteDialog(url, title)
            }
        }

        findViewById<TextView>(R.id.tvFindBtn)?.setOnClickListener { showFindDialog() }

        // 👇 提取网页图片并展示画廊（完美修复版） 👇
        findViewById<TextView>(R.id.fabExtractImg)?.setOnClickListener {
            hideUiRunnable.run()
            val currentWeb = (if (activePane == 1) webView1 else webView2) ?: return@setOnClickListener

            // 1. 强行收起软键盘，防止遮挡画廊
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(currentWeb.windowToken, 0)
            currentWeb.clearFocus()

            val extractJs = """
    (function() {
        var imgs = document.querySelectorAll('img');
        var urls = [];
        for (var i = 0; i < imgs.length; i++) {
            var img = imgs[i];
            var candidates = [];
            
            // 把所有可能的属性都装进候选池
            if (img.src) candidates.push(img.src);
            if (img.getAttribute('data-src')) candidates.push(img.getAttribute('data-src'));
            if (img.getAttribute('data-original')) candidates.push(img.getAttribute('data-original'));
            if (img.getAttribute('data-lazy-src')) candidates.push(img.getAttribute('data-lazy-src'));
            if (img.getAttribute('data-url')) candidates.push(img.getAttribute('data-url'));
            
            // 解析 srcset
            var srcset = img.getAttribute('srcset');
            if (srcset) {
                var parts = srcset.split(',');
                for (var j = 0; j < parts.length; j++) {
                    var url = parts[j].trim().split(' ')[0];
                    if (url) candidates.push(url);
                }
            }
            
            // 核心：在这些候选链接中，选长度最长的一个
            var bestUrl = '';
            for (var k = 0; k < candidates.length; k++) {
                var u = candidates[k];
                if (u && !u.startsWith('data:') && u.length > bestUrl.length) {
                    bestUrl = u;
                }
            }
            
            // 补全协议
            if (bestUrl && bestUrl.startsWith('//')) {
                bestUrl = 'https:' + bestUrl;
            }
            
            if (bestUrl && bestUrl.startsWith('http')) {
                urls.push(bestUrl);
            }
        }
        return urls.join('\n');
    })();
""".trimIndent()

            // 3. 处理 JS 返回的数据
            currentWeb.evaluateJavascript(extractJs) { result ->
                if (result != null && result != "null") {
                    try {
                        val cleanResult = result.trim('"').replace("\\n", "\n")

                        if (cleanResult.isBlank()) {
                            Toast.makeText(this, "当前网页没有找到可提取的图片", Toast.LENGTH_SHORT).show()
                        } else {
                            val imageUrls = cleanResult.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .distinct()

                            if (imageUrls.isEmpty()) {
                                Toast.makeText(this, "当前网页没有找到可提取的图片", Toast.LENGTH_SHORT).show()
                            } else {
                                // 显示画廊
                                tvGalleryTitle.text = "已提取 ${imageUrls.size} 张图片"

                                // 【核心修复】：获取大图控件，并绑定点击缩回逻辑
                                val ivFullScreenImage = findViewById<ImageView>(R.id.ivFullScreenImage)
                                ivFullScreenImage.setOnClickListener {
                                    ivFullScreenImage.visibility = View.GONE
                                    ivFullScreenImage.setImageDrawable(null) // 点击缩回时，立刻清空画布，防止残影
                                }

                                // 【核心修复】：获取底部关闭按钮，绑定彻底清理逻辑
                                findViewById<TextView>(R.id.btnCloseGallery)?.setOnClickListener {
                                    galleryOverlay.visibility = View.GONE
                                    ivFullScreenImage.visibility = View.GONE
                                    ivFullScreenImage.setImageDrawable(null) // 关闭画廊时，彻底清空大图
                                }

                                val adapter = GalleryAdapter(imageUrls, currentPreviewUrl ?: currentWeb.url ?: "")
                                adapter.onItemClick = { clickedUrl ->
                                    // 当点击瀑布流里的某张图时：
                                    ivFullScreenImage.setImageDrawable(null) // 先清空画布
                                    ivFullScreenImage.visibility = View.VISIBLE // 再显示出来

                                    // 异步加载高清大图
                                    Thread {
                                        try {
                                            val inputStream = java.net.URL(clickedUrl).openStream()
                                            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                            if (bitmap != null) {
                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                    ivFullScreenImage.setImageBitmap(bitmap)
                                                }
                                            }
                                        } catch (e: Exception) {}
                                    }.start()
                                }
                                rvGallery.adapter = adapter
                                galleryOverlay.visibility = View.VISIBLE
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "提取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "未能提取到图片数据", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 👇 悬浮面板里的【抓视频】按钮 👇
        findViewById<TextView>(R.id.fabExtractVideo)?.setOnClickListener {
            hideUiRunnable.run() // 点击后立刻隐藏悬浮面板

            val currentWeb = (if (activePane == 1) webView1 else webView2) ?: return@setOnClickListener

            // 用 JS 强行扫一遍网页里所有的 video 和 source 标签，并把封面(poster)一起抓下来
            val extractVideoJs = """
    (function() {
        var urls = [];
        var videos = document.querySelectorAll('video');
        
        // 智能寻找当前网页的“标题”
        var pageTitle = document.title || "网页视频";
        var titleNode = document.querySelector('h1') || document.querySelector('.title') || document.querySelector('.video-title');
        if (titleNode && titleNode.innerText) {
            pageTitle = titleNode.innerText.trim();
        }
        
        for (var i = 0; i < videos.length; i++) {
            var video = videos[i];
            var src = video.src;
            if (!src && video.querySelector('source')) {
                src = video.querySelector('source').src;
            }
            
            if (src) {
                // 1. 找封面：优先找 poster 属性
                var poster = video.poster || video.getAttribute('data-poster') || video.getAttribute('data-cover') || "";
                
                // 2. 如果没封面，看看外层包裹的 div 里有没有 <img>
                if (!poster && video.parentElement) {
                    var img = video.parentElement.querySelector('img');
                    if (img && img.src) poster = img.src;
                }
                
                // 3. 名字：如果页面有标题，就用页面标题；如果有多个视频，在后面加个序号
                var vName = pageTitle;
                if (videos.length > 1) {
                    vName = pageTitle + " (视频 " + (i + 1) + ")";
                }
                
                // 处理相对路径的封面
                if (poster && poster.startsWith('//')) poster = 'https:' + poster;
                if (poster && poster.startsWith('/')) poster = window.location.origin + poster;
                
                urls.push(src + "|" + poster + "|" + vName);
            }
        }
        return urls.join('\n');
    })();
""".trimIndent()

            currentWeb.evaluateJavascript(extractVideoJs) { result ->
                val jsUrls = mutableListOf<String>()
                if (result != null && result != "null") {
                    val cleanResult = result.trim('"').replace("\\n", "\n")
                    jsUrls.addAll(cleanResult.split("\n").map { it.trim() }.filter { it.isNotEmpty() })
                }

                // 组装数据：视频链接 和 封面图
                // 👇 改成 Triple 存放：链接, 封面, 名字
                val allVideoData = mutableListOf<Triple<String, String, String>>()

                jsUrls.forEach { item ->
                    val parts = item.split("|")
                    val vUrl = parts.getOrNull(0) ?: ""
                    val poster = parts.getOrNull(1) ?: ""
                    val vName = parts.getOrNull(2) ?: "未知视频"
                    if (vUrl.isNotEmpty()) {
                        allVideoData.add(Triple(vUrl, poster, vName))
                    }
                }

                sniffedVideoUrls.forEach { vUrl ->
                    if (allVideoData.none { it.first == vUrl }) {
                        // 底层拦截的没名字，就用网址凑合一下
                        allVideoData.add(Triple(vUrl, "", "嗅探到的视频流"))
                    }
                }

                if (allVideoData.isEmpty()) {
                    Toast.makeText(this, "暂未发现视频，请先在网页里点一下播放", Toast.LENGTH_SHORT).show()
                } else {
                    // 👇 弹出一个全屏半透明的带封面的视频列表 👇
                    val dialog = android.app.Dialog(this)
                    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

                    val container = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(0xEE000000.toInt()) // 半透明黑底
                        setPadding(30, 30, 30, 30)
                    }

                    val titleTv = TextView(this).apply {
                        text = "共发现 ${allVideoData.size} 个视频"
                        setTextColor(0xFFFFD700.toInt()) // 金色标题
                        textSize = 18f
                        setPadding(0, 0, 0, 20)
                    }

                    val rv = RecyclerView(this).apply {
                        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
                        adapter = SniffedVideoAdapter(allVideoData).also { adapter ->
                            adapter.onItemClick = { clickedUrl ->
                                dialog.dismiss()
                                showVideoActionDialog(clickedUrl)
                            }
                        }
                    }

                    val btnClose = TextView(this).apply {
                        text = "关闭列表"
                        setTextColor(0xFFFF3366.toInt()) // 红色关闭按钮
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 20, 0, 20)
                        textSize = 16f
                        setOnClickListener { dialog.dismiss() }
                    }

                    container.addView(titleTv)
                    container.addView(rv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                    container.addView(btnClose)

                    dialog.setContentView(container)
                    dialog.window?.setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    dialog.show()
                }
            }
        }

        // 👇 重新绑定退出阅读模式按钮 👇
        findViewById<TextView>(R.id.btnCloseTextReader)?.setOnClickListener {
            val readerOverlay = findViewById<View>(R.id.textReaderOverlay)
            readerOverlay?.visibility = View.GONE

            // 顺便收起键盘，防止键盘挡住屏幕
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        // 👇 字体大小控制变量（默认18sp） 👇
        var currentFontSize = 18f




// 👇 字体放大/缩小 👇
        findViewById<TextView>(R.id.btnFontSmaller)?.setOnClickListener {
            if (currentFontSize > 12f) {
                currentFontSize -= 2f
                findViewById<TextView>(R.id.tvReaderContent).textSize = currentFontSize
            }
        }
        findViewById<TextView>(R.id.btnFontLarger)?.setOnClickListener {
            if (currentFontSize < 36f) {
                currentFontSize += 2f
                findViewById<TextView>(R.id.tvReaderContent).textSize = currentFontSize
            }
        }

// 👇 翻页逻辑（平滑滚动一屏的高度） 👇
        findViewById<TextView>(R.id.btnPrevPage)?.setOnClickListener {
            val scrollView = findViewById<ScrollView>(R.id.readerScrollView)
            scrollView.smoothScrollBy(0, -scrollView.height * 4 / 5)
        }
        findViewById<TextView>(R.id.btnNextPage)?.setOnClickListener {
            val scrollView = findViewById<ScrollView>(R.id.readerScrollView)
            scrollView.smoothScrollBy(0, scrollView.height * 4 / 5)
        }

// 👇 抓取网页文本逻辑 👇
        findViewById<TextView>(R.id.fabExtractText)?.setOnClickListener {
            hideUiRunnable.run() // 隐藏悬浮面板
            val currentWeb = (if (activePane == 1) webView1 else webView2) ?: return@setOnClickListener

            // 每次抓取时，重置字体大小
            currentFontSize = 18f
            findViewById<TextView>(R.id.tvReaderContent).textSize = currentFontSize

            val extractTextJs = """
        (function() {
            try {
                var garbage = document.querySelectorAll('script, style, nav, header, footer, aside, iframe, noscript, .ad, .ads, #comments, .sidebar');
                for (var i = 0; i < garbage.length; i++) {
                    garbage[i].style.display = 'none';
                    garbage[i].remove();
                }
                var contentNode = document.querySelector('article') || document.querySelector('main') || document.querySelector('.post-content') || document.querySelector('#content') || document.body;
                var text = contentNode.innerText || "";
                text = text.replace(/\n\s*\n/g, '\n\n'); 
                text = text.replace(/^\s+|\s+$/g, ''); 
                if (text.length < 50) { text = document.body.innerText; }
                if (text.length > 50000) { text = text.substring(0, 50000) + "\n\n... (内容过长已截断)"; }
                return text;
            } catch (e) {
                return "提取失败: " + e.message;
            }
        })();
    """.trimIndent()

            currentWeb.evaluateJavascript(extractTextJs) { result ->
                if (result != null && result != "null") {
                    try {
                        val cleanText = result
                            .removePrefix("\"")
                            .removeSuffix("\"")
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\t", "\t")
                            .replace("\\\\", "\\")

                        if (cleanText.isBlank()) {
                            Toast.makeText(this, "未能提取到有效文本", Toast.LENGTH_SHORT).show()
                        } else {
                            findViewById<TextView>(R.id.tvReaderContent).text = cleanText
                            findViewById<View>(R.id.textReaderOverlay).visibility = View.VISIBLE
                            // 👇 暴力清理可能残留的遮挡层，防止点不到按钮
                            findViewById<View>(R.id.lockOverlay)?.visibility = View.GONE
                            findViewById<View>(R.id.galleryOverlay)?.visibility = View.GONE

// 收键盘，防止键盘挡住底部退出按钮
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.hideSoftInputFromWindow(currentWeb.windowToken, 0)
                            // 每次打开自动滚到顶部
                            findViewById<ScrollView>(R.id.readerScrollView).scrollTo(0, 0)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "解析文本失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }



        findViewById<CheckBox>(R.id.cbPauseBg)?.apply { isChecked = pauseBg; setOnCheckedChangeListener { _, b -> pauseBg = b; loadCustomBackground() } }
        findViewById<CheckBox>(R.id.cbUnloadHidden)?.apply { isChecked = unloadHidden; setOnCheckedChangeListener { _, b -> unloadHidden = b; if (b && layoutMode == "single") webView2?.loadUrl("about:blank") } }
        findViewById<CheckBox>(R.id.cbDisableScale)?.apply { isChecked = disableScale; setOnCheckedChangeListener { _, b -> disableScale = b; webView1?.settings?.builtInZoomControls = !b; webView2?.settings?.builtInZoomControls = !b; if (b) setZoom(1.0f) } }

        findViewById<CheckBox>(R.id.cbAdBlock)?.apply {
            isChecked = adBlockEnabled
            setOnCheckedChangeListener { _, b ->
                adBlockEnabled = b
                prefs.edit().putBoolean("ad_block_enabled", b).apply()
                Toast.makeText(this@MainActivity, if (b) "广告拦截已开启" else "广告拦截已关闭", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.tvCustomAdRulesBtn)?.setOnClickListener {
            showCustomAdRulesDialog()
        }

        findViewById<TextView>(R.id.tvAddWhiteListBtn)?.setOnClickListener {
            val currentWeb = if (activePane == 1) webView1 else webView2
            val url = currentWeb?.url
            if (url.isNullOrEmpty() || url.startsWith("about:")) {
                Toast.makeText(this, "当前没有打开的网页", Toast.LENGTH_SHORT).show()
            } else {
                val host = try { Uri.parse(url).host } catch (e: Exception) { null }
                if (host != null) {
                    if (adWhiteList.contains(host)) {
                        Toast.makeText(this, "该网站已在白名单中", Toast.LENGTH_SHORT).show()
                    } else {
                        addToAdWhiteList(host)
                        Toast.makeText(this, "已加入白名单：$host，请刷新网页", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "无法获取当前网站域名", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<TextView>(R.id.tvManageWhiteListBtn)?.setOnClickListener {
            showManageWhiteListDialog()
        }

        findViewById<TextView>(R.id.btnCloseSidebar).setOnClickListener { drawerLayout.closeDrawer(GravityCompat.START) }
        findViewById<TextView>(R.id.btnNotepad).setOnClickListener { showNotepadDialog() }
        findViewById<TextView>(R.id.btnClearLogin).setOnClickListener { clearWebLoginState() }
        findViewById<TextView>(R.id.btnAppearance).setOnClickListener { showAppearanceDialog() }
        findViewById<TextView>(R.id.sbExportBtn)?.setOnClickListener { exportDataToJson() }
        findViewById<TextView>(R.id.sbImportBtn)?.setOnClickListener { openJsonLauncher.launch(arrayOf("application/json")) }
        findViewById<TextView>(R.id.sbExportBookmarksBtn)?.setOnClickListener { exportHtmlBookmarks() }
        findViewById<TextView>(R.id.sbImportBookmarksBtn)?.setOnClickListener { openHtmlLauncher.launch(arrayOf("text/html")) }
        findViewById<TextView>(R.id.sbHealthBtn)?.setOnClickListener { showHealthCheck() }
        findViewById<TextView>(R.id.sbStatsBtn)?.setOnClickListener { showStatistics() }
        findViewById<TextView>(R.id.sbStorageBtn)?.setOnClickListener { showStorageInfo() }
        findViewById<TextView>(R.id.sbClearAllBtn)?.setOnClickListener { showClearAllDataDialog() }
        findViewById<TextView>(R.id.sbHelpBtn)?.setOnClickListener { showHelpDialog() }

        findViewById<TextView>(R.id.btnSetAppLock)?.setOnClickListener {
            val input = EditText(this).apply {
                hint = "输入4位数字密码（留空则取消密码）"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setText(appLockPassword ?: "")
            }
            AlertDialog.Builder(this)
                .setTitle("设置应用锁")
                .setView(input)
                .setPositiveButton("保存") { _, _ ->
                    val pwd = input.text.toString().trim()
                    if (pwd.isNotEmpty() && pwd.length != 4) {
                        Toast.makeText(this, "密码必须是4位数字", Toast.LENGTH_SHORT).show()
                    } else {
                        appLockPassword = if (pwd.isEmpty()) null else pwd
                        prefs.edit().putString("app_lock_pwd", appLockPassword).apply()
                        Toast.makeText(this, if (pwd.isEmpty()) "已取消密码" else "密码设置成功", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 👇 关于与赞助 👇
        findViewById<TextView>(R.id.btnSponsor)?.setOnClickListener {
            val platforms = arrayOf(
                " 前往爱发电支持开发者",
                " 访问我的 GitHub 项目主页",
                " 查看项目说明"
            )

            AlertDialog.Builder(this)
                .setTitle("关于与赞助\n(本软件完全免费，欢迎支持开发者)")
                .setItems(platforms) { _, which ->
                    when (which) {
                        0 -> {
                            // 👇 1. 替换成你真实的爱发电链接（等你以后注册了再改）
                            val afdianUrl = "https://afdian.com/a/LilithKate"
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(afdianUrl)))
                            } catch (e: Exception) {
                                Toast.makeText(this, "未找到浏览器", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {
                            // 👇 2. 这里已经填好了你真实的 GitHub 仓库链接
                            val githubUrl = "https://github.com/LilithKate202/GoonerWebFav"
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
                            } catch (e: Exception) {
                                Toast.makeText(this, "未找到浏览器", Toast.LENGTH_SHORT).show()
                            }
                        }
                        2 -> {
                            AlertDialog.Builder(this)
                                .setTitle("关于本项目")
                                .setMessage("这是一款由个人开发者打造的定制化浏览器。\n主打无广告、轻量级与极客体验。\n\n感谢你的使用！")
                                .setPositiveButton("关闭", null)
                                .show()
                        }
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        findViewById<TextView>(R.id.btnUnlock)?.setOnClickListener {
            val inputPwd = findViewById<EditText>(R.id.etUnlockPwd).text.toString().trim()
            if (inputPwd == appLockPassword) {
                findViewById<LinearLayout>(R.id.lockOverlay).visibility = View.GONE
                findViewById<EditText>(R.id.etUnlockPwd).text.clear()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            } else {
                Toast.makeText(this, "密码错误！", Toast.LENGTH_SHORT).show()
            }
        }

        val websiteInput = findViewById<EditText>(R.id.websiteInput); val saveBtn = findViewById<TextView>(R.id.saveWebsiteBtn)
        saveBtn.setOnClickListener { addWebsite(websiteInput.text.toString().trim()); websiteInput.text.clear() }
        websiteInput.setOnEditorActionListener { _, actionId, _ -> if (actionId == EditorInfo.IME_ACTION_DONE) { saveBtn.performClick(); true } else false }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { searchKeyword = s.toString(); filterAndDisplay() }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<TextView>(R.id.excludeVisitedBtn).setOnClickListener {
            excludeVisitedOnly = !excludeVisitedOnly; it.setBackgroundResource(if (excludeVisitedOnly) R.drawable.bg_tab_active else R.drawable.bg_input); filterAndDisplay()
        }

        val chips = mapOf(
            R.id.chipAll to "all",
            R.id.chipAudio to "audio",
            R.id.chipImage to "image",
            R.id.chipVideo to "video",
            R.id.chipText to "text",
            R.id.chipUnvisited to "unvisited"
        )

        chips.forEach { (id, filter) ->
            findViewById<TextView>(id).setOnClickListener {
                currentFilter = filter

                chips.keys.forEach { chipId ->
                    val btn = findViewById<TextView>(chipId)
                    if (chipId == id) {
                        // 当前点击的按钮：变成粉红背景，纯白文字
                        btn.setBackgroundResource(R.drawable.bg_tab_active)
                        btn.setTextColor(0xFFFFFFFF.toInt())
                    } else {
                        // 其他按钮：恢复原背景，金色文字
                        btn.setBackgroundResource(R.drawable.bg_input)
                        btn.setTextColor(0xFFFFD700.toInt())
                    }
                }

                filterAndDisplay()
            }
        }
        // 双击拖拽条进入全屏
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isFullScreenMode) {
                    toggleFullScreen()
                    return true
                }
                return false
            }
        })

        val hDivider = findViewById<View>(R.id.hDivider)
        var initialY = 0f
        var initialWeight = 58f
        hDivider.setOnTouchListener { _, event ->
            // 【核心修复1】全屏状态：只允许点击退出（且防滑动误触）
            if (isFullScreenMode) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    initialY = event.rawY
                } else if (event.action == MotionEvent.ACTION_UP) {
                    // 如果手指移动距离很小，说明是点击，触发退出
                    if (Math.abs(event.rawY - initialY) < 50) {
                        toggleFullScreen()
                    }
                }
                return@setOnTouchListener true
            }

            // 【核心修复2】非全屏状态：识别双击进全屏，并允许拖拽
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    initialWeight = (topPanel.layoutParams as LinearLayout.LayoutParams).weight
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    val screenHeight = resources.displayMetrics.heightPixels.toFloat()
                    var newWeight = initialWeight + (deltaY / screenHeight) * 100f
                    if (newWeight < 20f) newWeight = 20f
                    if (newWeight > 80f) newWeight = 80f
                    (topPanel.layoutParams as LinearLayout.LayoutParams).weight = newWeight
                    (bottomPanel.layoutParams as LinearLayout.LayoutParams).weight = 100f - newWeight
                    topPanel.requestLayout()
                    bottomPanel.requestLayout()
                    true
                }
                else -> false
            }
        }

        adapter.onItemClick = { val currentTime = System.currentTimeMillis(); if (currentTime - lastClickTime > 500) { lastClickTime = currentTime; openUrlInPreview(it.url) } }
        adapter.onItemLongClick = { view, website -> showItemLongClickMenu(view, website) }
        adapter.onCheckChanged = { website, isChecked -> if (isChecked) selectedUrls.add(website.url) else selectedUrls.remove(website.url); infoBar.text = "已选中 ${selectedUrls.size} 个" }

        findViewById<TextView>(R.id.selectModeBtn).setOnClickListener { toggleSelectMode() }
        findViewById<TextView>(R.id.btnLayoutSingle).setOnClickListener { switchLayout("single") }
        findViewById<TextView>(R.id.btnLayoutSplitV).setOnClickListener { switchLayout("split-v") }
        findViewById<TextView>(R.id.btnLayoutSplitH).setOnClickListener { switchLayout("split-h") }
        findViewById<TextView>(R.id.btnZoomIn).setOnClickListener { if (!disableScale) setZoom(sharedZoom + 0.1f) }
        findViewById<TextView>(R.id.btnZoomOut).setOnClickListener { if (!disableScale) setZoom(sharedZoom - 0.1f) }
        findViewById<TextView>(R.id.btnZoomReset).setOnClickListener { if (!disableScale) setZoom(1.0f) }
        findViewById<TextView>(R.id.btnRelease).setOnClickListener { releaseAllWebViews() }

        // 【终极暴力开关】：直接在屏幕里找有没有带着标签的浮窗
        findViewById<TextView>(R.id.btnFloatWindow).setOnClickListener {
            val existingFloat = findViewById<ViewGroup>(R.id.mainRoot)?.findViewWithTag<View>(floatTag)
            if (existingFloat != null) {
                findViewById<ViewGroup>(R.id.mainRoot)?.removeView(existingFloat)
                Toast.makeText(this, "浮动窗口已关闭", Toast.LENGTH_SHORT).show()
            } else {
                createFloatingWindow()
            }
        }

        val btnTranslate = findViewById<TextView>(R.id.btnTranslatePage)
        btnTranslate.setOnClickListener {
            val currentWeb = if (activePane == 1) webView1 else webView2
            val url = currentWeb?.url
            if (url.isNullOrEmpty() || url.startsWith("about:")) Toast.makeText(this, "当前没有打开的网页", Toast.LENGTH_SHORT).show()
            else launchWebTranslate(currentWeb, url, currentEngineIndex)
        }

        btnTranslate.setOnLongClickListener {
            AlertDialog.Builder(this).setTitle("选择翻译引擎")
                .setSingleChoiceItems(engineNames, currentEngineIndex) { dialog, which ->
                    currentEngineIndex = which
                    prefs.edit().putInt("translate_engine_idx", which).apply()
                    if (which == 3) showCustomTranslateApiDialog()
                    else {
                        val currentWeb = if (activePane == 1) webView1 else webView2
                        val url = currentWeb?.url
                        if (!url.isNullOrEmpty() && !url.startsWith("about:")) launchWebTranslate(currentWeb, url, which)
                    }
                    dialog.dismiss()
                }.setNegativeButton("取消", null).show()
            true
        }

        val activeWebView = { if (activePane == 1) webView1 else webView2 }
        findViewById<TextView>(R.id.btnGoBack).setOnClickListener { if (activeWebView()?.canGoBack() == true) activeWebView()?.goBack() }
        findViewById<TextView>(R.id.btnRefresh).setOnClickListener { activeWebView()?.reload() }
        findViewById<TextView>(R.id.btnToggleFab).setOnClickListener {
            if (fabContainer?.visibility == View.VISIBLE) {
                hideUiRunnable.run() // 如果已经显示，点一下直接隐藏
            } else {
                resetUiTimer() // 如果隐藏，点一下显示，并触发2秒倒计时
            }
        }
        findViewById<TextView>(R.id.btnClear).setOnClickListener {
            activeWebView()?.loadUrl("about:blank"); previewPlaceholder.visibility = View.VISIBLE
            bottomNavBar.visibility = View.GONE; adjustFabPosition(false); loadCustomBackground()
        }
        findViewById<View>(android.R.id.content).setOnTouchListener { _, _ -> false }
    }

    private fun showCustomTranslateApiDialog() {
        val input = EditText(this).apply {
            hint = "填入代理前缀（如: https://xxx.com/translate?url=）"
            setText(prefs.getString("custom_translate_api", "https://translate.google.com/translate?sl=auto&tl=zh-CN&u="))
            setPadding(40, 20, 40, 20)
        }
        AlertDialog.Builder(this).setTitle("自定义网页翻译 API 前缀")
            .setMessage("App 会自动把当前网页链接拼接到该地址后面。")
            .setView(input)
            .setPositiveButton("保存并翻译") { _, _ ->
                prefs.edit().putString("custom_translate_api", input.text.toString().trim()).apply()
                val currentWeb = if (activePane == 1) webView1 else webView2
                val url = currentWeb?.url
                if (!url.isNullOrEmpty() && !url.startsWith("about:")) launchWebTranslate(currentWeb, url, 3)
            }.setNegativeButton("取消", null).show()
    }

    private fun loadCustomAdRules() {
        val customRules = prefs.getString("custom_ad_rules", "") ?: ""
        customAdRuleList.clear()
        if (customRules.isNotBlank()) {
            customAdRuleList.addAll(customRules.split(Regex("[,\\n\\s]+")).map { it.trim().lowercase() }.filter { it.isNotBlank() })
        }
    }

    private fun loadAdWhiteList() {
        val list = prefs.getStringSet("ad_white_list", emptySet()) ?: emptySet()
        adWhiteList.clear()
        adWhiteList.addAll(list)
    }

    private fun addToAdWhiteList(host: String) {
        adWhiteList.add(host)
        prefs.edit().putStringSet("ad_white_list", adWhiteList).apply()
    }

    private fun showManageWhiteListDialog() {
        if (adWhiteList.isEmpty()) {
            Toast.makeText(this, "白名单为空", Toast.LENGTH_SHORT).show()
            return
        }

        val hosts = adWhiteList.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("点击即可删除白名单")
            .setItems(hosts) { _, which ->
                val removeHost = hosts[which]
                adWhiteList.remove(removeHost)
                prefs.edit().putStringSet("ad_white_list", adWhiteList).apply()
                Toast.makeText(this, "已从白名单删除：$removeHost", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showCustomAdRulesDialog() {
        val currentRules = prefs.getString("custom_ad_rules", "") ?: ""
        val input = EditText(this).apply {
            setText(currentRules)
            hint = "输入要拦截的域名（换行或逗号分隔），如：ad.com"
            minLines = 6
            gravity = Gravity.TOP or Gravity.START
            setPadding(30, 30, 30, 30)
        }
        AlertDialog.Builder(this)
            .setTitle("自定义广告拦截规则")
            .setMessage("网址中包含这些关键字的请求将被拦截（一行一个）")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                prefs.edit().putString("custom_ad_rules", input.text.toString().trim()).apply()
                loadCustomAdRules()
                Toast.makeText(this, "规则已保存，刷新网页生效", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupBackPressedHandler() {
        // 【核心修复】如果当前卡在空白页，且没有历史记录，直接加载初始页逃逸
        val currentWebView = if (activePane == 1) webView1 else webView2
        val currentUrl = currentWebView?.url ?: ""
        if ((currentUrl == "about:blank" || currentUrl.isEmpty()) && currentWebView?.canGoBack() != true) {
            doRandomJump() // 或者用 currentWebView?.loadUrl("about:blank") 跳出
            return
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                // 如果阅读模式开着，按物理返回键直接退出
                val reader = findViewById<View>(R.id.textReaderOverlay)
                if (reader != null && reader.visibility == View.VISIBLE) {
                    reader.visibility = View.GONE
                    return
                }

                // 1. 侧边栏开着，先关侧边栏
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return
                }

                // 2. 如果有全屏视频，先关全屏
                if (customView != null) {
                    webView1?.let { (it.webChromeClient as? WebChromeClient)?.onHideCustomView() }
                    return
                }



                // 4. 网页后退判断
                val currentWeb = if (activePane == 1) webView1 else webView2
                val otherWeb = if (activePane == 1) webView2 else webView1

                if (currentWeb?.canGoBack() == true) {
                    currentWeb.goBack()
                } else if (layoutMode != "single" && otherWeb?.canGoBack() == true) {
                    // 分屏模式下，当前窗口不能退，去退另一个窗口
                    otherWeb.goBack()
                } else {
                    // 5. 【核心】网页无法后退时，绝不退出 App，直接触发随机跳转
                    doRandomJump()
                }
            }
        })
    }

    private fun resetUiTimer() {
        fabContainer?.let { if (it.visibility != View.VISIBLE && customView == null) { it.visibility = View.VISIBLE; it.animate().alpha(1f).setDuration(300).start() } }
        sidebarToggle?.let { if (it.visibility != View.VISIBLE && customView == null) { it.visibility = View.VISIBLE; it.animate().alpha(1f).setDuration(300).start() } }

        uiHandler.removeCallbacks(hideUiRunnable);

        // 如果固定显示，就不触发 4 秒后的隐藏
        if (!isRandomBtnFixed) {
            uiHandler.postDelayed(hideUiRunnable, 8000)
        }
    }

    private fun toggleFullScreen() {
        // 【核心修复】0.5秒防抖锁，防止双击时的抬手瞬间触发二次切换
        val now = System.currentTimeMillis()
        if (now - lastFullScreenToggleTime < 500) return
        lastFullScreenToggleTime = now

        isFullScreenMode = !isFullScreenMode
        val lpTop = topPanel.layoutParams as LinearLayout.LayoutParams
        val lpBottom = bottomPanel.layoutParams as LinearLayout.LayoutParams
        val hDivider = findViewById<View>(R.id.hDivider)
        val hDividerParams = hDivider.layoutParams as LinearLayout.LayoutParams

        if (isFullScreenMode) {
            topPanel.visibility = View.GONE
            hDivider.visibility = View.VISIBLE
            // 全屏时黄条变粗，方便点击
            hDividerParams.height = (40 * resources.displayMetrics.density).toInt()
            hDividerParams.topMargin = (20 * resources.displayMetrics.density).toInt()
            lpTop.weight = 0f
            lpBottom.weight = 100f
            Toast.makeText(this, "已进入全屏，点击顶部黄条恢复", Toast.LENGTH_SHORT).show()
        } else {
            topPanel.visibility = View.VISIBLE
            hDivider.visibility = View.VISIBLE
            // 恢复原本的细度
            hDividerParams.height = (8 * resources.displayMetrics.density).toInt()
            hDividerParams.topMargin = 0
            lpTop.weight = 58f
            lpBottom.weight = 42f
        }
        hDivider.layoutParams = hDividerParams
        topPanel.layoutParams = lpTop
        bottomPanel.layoutParams = lpBottom
        hDivider.requestLayout()
    }

    private fun loadData() {
        lifecycleScope.launch {
            if (db.appDao().getAllZones().isEmpty()) db.appDao().insertZone(ZoneEntity(name = "默认分区", isDefault = true, isRandomStart = true))
            if (db.appDao().getAllWebsites().isEmpty()) {
                val builtins = listOf(
                    WebsiteEntity(url = "https://cn.pornhub.com/", title = "Pornhub", zoneName = "默认分区"),
                    WebsiteEntity(url = "https://rule34.xxx/", title = "Rule34", zoneName = "默认分区"),
                    WebsiteEntity(url = "https://hypnotube.com/index.php", title = "Hypnotube", zoneName = "默认分区"),
                    WebsiteEntity(url = "https://nhentai.net", title = "Nhentai", zoneName = "默认分区"),
                    WebsiteEntity(url = "https://www.xvideos.com/", title = "Xvideos", zoneName = "默认分区"),
                    WebsiteEntity(url = "https://hgoon.booru.org/", title = "HgoonBooru", zoneName = "默认分区"),
                    WebsiteEntity(url = "https://theporndude.com/zh", title = "Porndude", zoneName = "默认分区")
                )

                builtins.forEach { db.appDao().insertWebsite(it) }
            }
            allZones = db.appDao().getAllZones(); renderZoneTabs(); filterAndDisplay(); updateBuiltinSites(); loadCustomBackground()

            val lastExit = prefs.getLong("last_exit_time", 0)
// 加上了 allWebsitesInZone.isEmpty() 判断，只有在刚启动时才恢复
            if (System.currentTimeMillis() - lastExit < 86400000L && allWebsitesInZone.isEmpty()) {
                currentZone = prefs.getString("last_zone", "默认分区") ?: "默认分区"
                val savedLayout = prefs.getString("last_layout", "single") ?: "single"
                activePane = prefs.getInt("last_active_pane", 1)
                val url1 = prefs.getString("last_url1", null)
                val url2 = prefs.getString("last_url2", null)

                // 👇 【核心修复】必须先切布局，把 WebView 挂载到屏幕上，再加载网址
                switchLayout(savedLayout)

                if (!url1.isNullOrEmpty() && !url1.startsWith("about:")) webView1?.loadUrl(url1)
                if (!url2.isNullOrEmpty() && !url2.startsWith("about:")) webView2?.loadUrl(url2)

                if (!url1.isNullOrEmpty() || !url2.isNullOrEmpty()) {
                    previewPlaceholder.visibility = View.GONE
                    bottomNavBar.visibility = View.VISIBLE
                    adjustFabPosition(true)
                }
            } else {
                switchLayout("single")
            }
        }
    }

    private fun filterAndDisplay() {
        lifecycleScope.launch {
            allWebsitesInZone = db.appDao().getWebsitesInZone(currentZone)

            // 1. 先拿全量数据
            var filtered = allWebsitesInZone

            // 2. 依次过滤
            if (excludeVisitedOnly) filtered = filtered.filter { it.visitCount == 0 }
            if (searchKeyword.isNotEmpty()) filtered = filtered.filter {
                it.title.contains(searchKeyword, true) || it.url.contains(searchKeyword, true) ||
                        it.note.contains(searchKeyword, true) || it.tags.contains(searchKeyword, true)
            }

            when (currentFilter) {
                "image" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("image", true) || it.url.contains("pic", true) || it.url.contains("photo", true) || it.url.contains("img", true) || it.url.contains("jpg", true) || it.url.contains("png", true)
                    val matchTitle = it.title.contains("图", true) || it.title.contains("image", true) || it.title.contains("pic", true)
                    matchUrl || matchTitle
                }
                "video" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("video", true) || it.url.contains("play", true) || it.url.contains("watch", true) || it.url.contains("mp4", true)
                    val matchTitle = it.title.contains("视频", true) || it.title.contains("video", true)
                    matchUrl || matchTitle
                }
                "audio" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("audio", true) || it.url.contains("music", true) || it.url.contains("mp3", true)
                    val matchTitle = it.title.contains("音", true) || it.title.contains("audio", true) || it.title.contains("music", true)
                    matchUrl || matchTitle
                }
                "text" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("text", true) || it.url.contains("txt", true) || it.url.contains("read", true)
                    val matchTitle = it.title.contains("文", true) || it.title.contains("read", true)
                    matchUrl || matchTitle
                }
                "unvisited" -> filtered = filtered.filter { it.visitCount == 0 }
            }

            // 👇 3. 【核心：排序逻辑放最后】上一次访问的强制置顶，其他的按最后访问时间降序 👇
            filtered = filtered.sortedWith(
                compareByDescending<WebsiteEntity> { it.url == lastVisitedUrl }
                    .thenByDescending { it.lastVisit }
            )

            adapter.updateData(filtered, isSelectMode, selectedUrls)

            infoBar.text = if (isSelectMode) {
                "已选中 ${selectedUrls.size} 个"
            } else {
                if (filtered.isEmpty() && allWebsitesInZone.isNotEmpty()) {
                    "当前分类下没有匹配的网页"
                } else {
                    "你已收藏 ${db.appDao().getTotalCount()} 个网页"
                }
            }
        }
    }

    private fun renderZoneTabs() {
        zoneTabsContainer.removeAllViews()
        allZones.forEach { zone ->
            val btn = TextView(this)
            btn.text = if (zone.isRandomStart) "${zone.name} (初始随机)" else zone.name
            btn.textSize = 12f; btn.setTextColor(if (zone.name == currentZone) 0xFFFFFFFF.toInt() else 0xFFFFD700.toInt())
            btn.setBackgroundResource(if (zone.name == currentZone) R.drawable.bg_tab_active else R.drawable.bg_input)
            btn.gravity = Gravity.CENTER; btn.setPadding(30, 0, 30, 0)
            btn.setOnClickListener { currentZone = zone.name; loadData() }
            btn.setOnLongClickListener { showZoneMenu(zone); true }
            zoneTabsContainer.addView(btn, LinearLayout.LayoutParams(-2, 90).apply { marginEnd = 10 })
        }
        val addBtn = TextView(this)
        addBtn.text = "+ 新建"; addBtn.textSize = 12f; addBtn.setTextColor(0xFFFF66CC.toInt()); addBtn.setBackgroundResource(R.drawable.bg_tab_dashed)
        addBtn.gravity = Gravity.CENTER; addBtn.setPadding(30, 0, 30, 0); addBtn.setOnClickListener { showAddZoneDialog() }
        zoneTabsContainer.addView(addBtn, LinearLayout.LayoutParams(-2, 90).apply { marginStart = 10 })
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {

            // 👇 情况 1：用户分享的是一个文件（JSON 文件） 👇
            val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val jsonStr = reader.readText()
                        reader.close()
                        withContext(Dispatchers.Main) {
                            importDataFromJson(jsonStr)
                            Toast.makeText(this@MainActivity, "已通过分享文件导入数据", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "文件读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                intent.action = ""
                return
            }

            // 👇 情况 2：用户分享的是一段纯文本（网址或普通文字） 👇
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                // 使用 Android 官方提供的正则匹配提取网址
                val matcher = android.util.Patterns.WEB_URL.matcher(sharedText)
                if (matcher.find()) {
                    val url = matcher.group()
                    if (url != null) {
                        // 延迟 0.3 秒打开，防止 App 刚启动 UI 还没初始化完就加载网页导致崩溃
                        uiHandler.postDelayed({
                            openUrlInPreview(url)
                            Toast.makeText(this, "已打开分享的网页", Toast.LENGTH_SHORT).show()
                        }, 300)
                    }
                } else {
                    // 如果分享的是纯文本（没带网址），把文字自动填入收藏输入框
                    val input = findViewById<EditText>(R.id.websiteInput)
                    input.setText(sharedText)
                    Toast.makeText(this, "已填入分享的文本", Toast.LENGTH_SHORT).show()
                }
            }
            // 处理完后清空 action，防止每次切回 App 时重复触发加载
            intent.action = ""
        }
    }

    private fun openUrlInPreview(url: String) {
        previewPlaceholder.visibility = View.GONE
        bottomNavBar.visibility = View.VISIBLE
        adjustFabPosition(true)

        var finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "http://$url" else url

        // 👇 【新增】在更新当前网址前，先把旧的网址存为“上一次访问”
        if (currentPreviewUrl != null && currentPreviewUrl != finalUrl) {
            lastVisitedUrl = currentPreviewUrl
        }

        // 👇 记录当前的真实网址，供收藏按钮使用
        currentPreviewUrl = finalUrl

        val targetWebView = if (activePane == 1) webView1 else webView2

        val isImage = url.matches(Regex("(?i).*\\.(jpe?g|png|gif|webp)(\\?|#|$)")) ||
                url.contains("/image", true) ||
                url.contains("/pic", true) ||
                url.contains("/photo", true) ||
                url.contains("/img", true)

        if (isImage) {
            // 👇 核心修复：把 null 换成 finalUrl 作为 baseURL，破解防盗链
            val html = "<html><body style='margin:0;background:#000;display:flex;justify-content:center;align-items:center;height:100vh;'><img src='$finalUrl' style='max-width:100%;max-height:100%;'></body></html>"
            targetWebView?.loadDataWithBaseURL(finalUrl, html, "text/html", "utf-8", null)
        } else if (url.matches(Regex("(?i).*\\.(mp3|wav|m4a|aac|ogg)(\\?|#|$)"))) {
            val html = "<html><body style='margin:0;background:#000;display:flex;justify-content:center;align-items:center;height:100vh;'><audio controls autoplay='false' src='$finalUrl' style='width:80%;'></audio></body></html>"
            targetWebView?.loadDataWithBaseURL(finalUrl, html, "text/html", "utf-8", null)
        } else if (url.matches(Regex("(?i).*\\.(txt|md|json|csv)(\\?|#|$)"))) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val text = URL(finalUrl).readText()
                    val html = "<html><body style='background:#111;color:#ffffff;padding:15px;word-wrap:break-word;white-space:pre-wrap;font-family:monospace;'>$text</body></html>"
                    withContext(Dispatchers.Main) { targetWebView?.loadDataWithBaseURL(finalUrl, html, "text/html", "utf-8", null) }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { showFailureView(finalUrl) }
                }
            }
        } else {
            targetWebView?.loadUrl(finalUrl)
        }
    }

    private fun launchWebTranslate(webView: WebView?, originalUrl: String, engineIndex: Int) {
        if (webView == null) return
        val encodedUrl = Uri.encode(originalUrl.trim())
        val translateUrl = when (engineIndex) {
            0 -> "https://translate.google.com/translate?sl=auto&tl=zh-CN&u=$encodedUrl"
            1 -> "https://translate.yandex.com/translate?lang=en-zh&url=$encodedUrl"
            2 -> "https://www.translatetheweb.com/?from=&to=zh-CHS&a=$encodedUrl"
            3 -> {
                val customPrefix = prefs.getString("custom_translate_api", "") ?: ""
                if (customPrefix.isBlank()) { Toast.makeText(this, "请先长按设置自定义API", Toast.LENGTH_SHORT).show(); return }
                if (customPrefix.contains("%s")) customPrefix.replace("%s", encodedUrl) else customPrefix + encodedUrl
            }
            else -> originalUrl
        }
        Toast.makeText(this, "正在通过【${engineNames[engineIndex]}】加载...", Toast.LENGTH_SHORT).show()
        webView.loadUrl(translateUrl)
        bottomNavBar.visibility = View.VISIBLE; adjustFabPosition(true)
    }

    private fun switchLayout(mode: String) {
        layoutMode = mode
        previewContainer.removeAllViews()
        (webView1?.parent as? ViewGroup)?.removeView(webView1)
        (webView2?.parent as? ViewGroup)?.removeView(webView2)
        (previewPlaceholder.parent as? ViewGroup)?.removeView(previewPlaceholder)

        val lpV = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        val lpH = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        // 【核心修复】单窗模式下的全屏尺寸
        val lpSingle = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        when (mode) {
            "single" -> {
                // 【核心修复】必须强制重置 webView1 的尺寸，否则会变成 0 宽度的白屏
                webView1?.layoutParams = lpSingle
                previewContainer.addView(webView1)
                // 如果不卸载，就让它进入睡眠状态，省电防偷跑
                if (!unloadHidden) webView2?.onPause()
                if (!unloadHidden) webView1?.onResume() // 确保当前显示的是活跃的
                if (unloadHidden) webView2?.loadUrl("about:blank")
                if (webView1?.url.isNullOrEmpty() || webView1?.url == "about:blank") {
                    previewContainer.addView(previewPlaceholder)
                    previewPlaceholder.visibility = View.VISIBLE
                } else {
                    previewPlaceholder.visibility = View.GONE
                }
            }
            "split-v" -> {
                val s = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                webView1?.layoutParams = lpV
                webView2?.layoutParams = lpV
                s.addView(webView1)
                s.addView(webView2)
                previewContainer.addView(s)
            }
            "split-h" -> {
                val s = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                webView1?.layoutParams = lpH
                webView2?.layoutParams = lpH
                s.addView(webView1)
                s.addView(webView2)
                previewContainer.addView(s)
            }
        }

        // 1. 立即请求重新布局
        webView1?.requestLayout()
        webView1?.invalidate()
        webView2?.requestLayout()
        webView2?.invalidate()

        // 2. 【核心修复】延迟 200 毫秒后再唤醒一次，彻底解决 WebView 布局切换时的白屏/黑屏问题
        uiHandler.postDelayed({
            webView1?.requestLayout()
            webView1?.invalidate()
            webView2?.requestLayout()
            webView2?.invalidate()
        }, 200)

        // 👇 【核心修复】同时控制背景和文字颜色，防止字和背景糊在一起 👇
        val btnSingle = findViewById<TextView>(R.id.btnLayoutSingle)
        val btnSplitV = findViewById<TextView>(R.id.btnLayoutSplitV)
        val btnSplitH = findViewById<TextView>(R.id.btnLayoutSplitH)

// 定义选中和未选中的样式
        val activeTextColor = 0xFFFFFFFF.toInt() // 选中：纯白字
        val inactiveTextColor = 0xFFFFD700.toInt() // 未选中：金色字

// 单窗按钮
        btnSingle?.setBackgroundResource(if (mode == "single") R.drawable.bg_tab_active else R.drawable.bg_input)
        btnSingle?.setTextColor(if (mode == "single") activeTextColor else inactiveTextColor)

// 左右分屏按钮
        btnSplitV?.setBackgroundResource(if (mode == "split-v") R.drawable.bg_tab_active else R.drawable.bg_input)
        btnSplitV?.setTextColor(if (mode == "split-v") activeTextColor else inactiveTextColor)

// 上下分屏按钮
        btnSplitH?.setBackgroundResource(if (mode == "split-h") R.drawable.bg_tab_active else R.drawable.bg_input)
        btnSplitH?.setTextColor(if (mode == "split-h") activeTextColor else inactiveTextColor)
    }

    private fun setZoom(zoom: Float) {
        sharedZoom = zoom.coerceIn(0.3f, 3.0f)
        webView1?.settings?.textZoom = (sharedZoom * 100).toInt(); webView2?.settings?.textZoom = (sharedZoom * 100).toInt()
        findViewById<TextView>(R.id.tvZoomLabel).text = "${(sharedZoom * 100).toInt()}%"
    }

    private fun exportHtmlBookmarks() {
        lifecycleScope.launch {
            val websites = db.appDao().getAllWebsites(); val sb = StringBuilder()
            sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n")
            websites.groupBy { it.zoneName }.forEach { (zone, list) ->
                sb.append("    <DT><H3>$zone</H3>\n    <DL><p>\n")
                list.forEach { sb.append("        <DT><A HREF=\"${it.url}\">${it.title.ifBlank { it.url }}</A>\n") }
                sb.append("    </DL><p>\n")
            }
            sb.append("</DL><p>\n"); htmlExportContent = sb.toString(); createHtmlLauncher.launch("bookmarks_${System.currentTimeMillis()}.html")
        }
    }

    private fun importHtmlBookmarks(htmlStr: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val regex = Regex("<A HREF=\"(.*?)\".*?>(.*?)</A>", RegexOption.IGNORE_CASE); var added = 0
                for (match in regex.findAll(htmlStr)) {
                    val url = match.groupValues[1]; val title = match.groupValues[2]
                    if (url.isNotBlank() && !url.startsWith("javascript:")) {
                        if (db.appDao().insertWebsite(WebsiteEntity(url = url, zoneName = "书签导入", title = title)) != -1L) added++
                    }
                }
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "书签导入完成，共 $added 条", Toast.LENGTH_SHORT).show(); loadData() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "书签解析失败", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun showFailureView(url: String) {
        if (failureView != null) return
        failureView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK); gravity = Gravity.CENTER; layoutParams = FrameLayout.LayoutParams(-1, -1)
            val tv = TextView(this@MainActivity).apply { text = "网页加载失败\n$url"; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
            val btnRetry = TextView(this@MainActivity).apply { text = "重试"; setTextColor(Color.WHITE); setPadding(30, 20, 30, 20); setOnClickListener { removeFailureView(); if (activePane == 1) webView1?.loadUrl(url) else webView2?.loadUrl(url) } }
            val btnBrowser = TextView(this@MainActivity).apply { text = "用浏览器打开"; setTextColor(Color.WHITE); setPadding(30, 20, 30, 20); setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
            addView(tv); addView(btnRetry); addView(btnBrowser)
        }
        previewContainer.addView(failureView)
    }

    private fun removeFailureView() { previewContainer.removeView(failureView); failureView = null }

    private fun releaseAllWebViews() {
        webView1?.loadUrl("about:blank"); webView2?.loadUrl("about:blank")
        (webView1?.parent as? ViewGroup)?.removeView(webView1); (webView2?.parent as? ViewGroup)?.removeView(webView2)
        (previewPlaceholder.parent as? ViewGroup)?.removeView(previewPlaceholder)
        findViewById<ViewGroup>(R.id.mainRoot)?.findViewWithTag<View>(floatTag)?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        previewContainer.removeAllViews()
        previewContainer.addView(webView1)
        previewContainer.addView(previewPlaceholder)
        previewPlaceholder.visibility = View.VISIBLE; bottomNavBar.visibility = View.GONE; adjustFabPosition(false); loadCustomBackground()
    }

    private fun exportDataToJson() {
        lifecycleScope.launch {
            val jsonArray = JSONArray()
            db.appDao().getAllWebsites().forEach {
                jsonArray.put(JSONObject().apply { put("url", it.url); put("zoneName", it.zoneName); put("title", it.title); put("tags", it.tags); put("note", it.note); put("visitCount", it.visitCount) })
            }
            jsonExportContent = jsonArray.toString(2); createJsonLauncher.launch("GoonerWebFav_Backup_${System.currentTimeMillis()}.json")
        }
    }

    private fun importDataFromJson(jsonStr: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rootObj = JSONObject(jsonStr); val zoneDataObj = rootObj.optJSONObject("zoneData")
                var added = 0; var newZones = 0
                if (zoneDataObj != null) {
                    val keys = zoneDataObj.keys()
                    while (keys.hasNext()) {
                        val zoneName = keys.next(); val array = zoneDataObj.getJSONArray(zoneName)
                        if (db.appDao().getAllZones().none { it.name == zoneName }) { db.appDao().insertZone(ZoneEntity(name = zoneName)); newZones++ }
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i); val url = obj.optString("url"); if (url.isBlank()) continue
                            var tagsStr = ""; val tagsArray = obj.optJSONArray("tags")
                            if (tagsArray != null) { val list = mutableListOf<String>(); for (j in 0 until tagsArray.length()) list.add(tagsArray.getString(j)); tagsStr = list.joinToString(",") }
                            val entity = WebsiteEntity(url = url, zoneName = zoneName, title = obj.optString("title"), tags = tagsStr, note = obj.optString("note"), visitCount = obj.optInt("visitCount", 0))
                            if (db.appDao().insertWebsite(entity) != -1L) added++
                        }
                    }
                } else {
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = WebsiteEntity(url = obj.getString("url"), zoneName = obj.optString("zoneName", "默认分区"), title = obj.optString("title", ""), tags = obj.optString("tags", ""), note = obj.optString("note", ""), visitCount = obj.optInt("visitCount", 0))
                        if (db.appDao().insertWebsite(entity) != -1L) added++
                    }
                }
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "导入完成：新增 $added 个，新分区 $newZones 个", Toast.LENGTH_LONG).show(); loadData() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "解析失败: ${e.message}", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun showHealthCheck() {
        lifecycleScope.launch {
            val allWebsites = db.appDao().getAllWebsites(); val duplicates = mutableListOf<String>(); val seen = mutableSetOf<String>()
            allWebsites.forEach { if (!seen.add(it.url)) duplicates.add(it.url) }
            val unvisited = allWebsites.count { it.visitCount == 0 }
            val msg = "【重复收藏】\n${if (duplicates.isEmpty()) "无" else duplicates.joinToString("\n")}\n\n【从未访问】\n共 $unvisited 个网址从未打开过。"
            AlertDialog.Builder(this@MainActivity).setTitle("收藏体检").setMessage(msg).setPositiveButton("确定", null).show()
        }
    }

    private fun showStatistics() {
        lifecycleScope.launch {
            val allWebsites = db.appDao().getAllWebsites(); val zonesCount = db.appDao().getAllZones().size
            val thisWeek = allWebsites.count { it.createdAt > System.currentTimeMillis() - 7 * 86400000L }
            val msg = "总收藏：${allWebsites.size}\n分区数：$zonesCount\n本周新增：$thisWeek"
            AlertDialog.Builder(this@MainActivity).setTitle("收藏统计").setMessage(msg).setPositiveButton("确定", null).show()
        }
    }

    private fun showStorageInfo() {
        val size = try { String.format("%.2f MB", getDatabasePath("gooner_web_fav_db").length() / 1024.0 / 1024.0) } catch (e: Exception) { "未知" }
        AlertDialog.Builder(this).setMessage("数据库大小：$size\n数据存在本设备，请定期备份。").setPositiveButton("确定", null).show()
    }

    private fun showClearAllDataDialog() {
        AlertDialog.Builder(this).setTitle("危险操作").setMessage("清空所有收藏、分区、统计数据？此操作不可逆！")
            .setPositiveButton("确认清空") { _, _ -> lifecycleScope.launch { db.appDao().getAllWebsites().forEach { db.appDao().deleteWebsite(it) }; db.appDao().getAllZones().forEach { if (!it.isDefault) db.appDao().deleteZone(it.name) }; Toast.makeText(this@MainActivity, "已清空", Toast.LENGTH_SHORT).show(); loadData() } }.setNegativeButton("取消", null).show()
    }

    private fun showVideoActionDialog(videoUrl: String) {
        val options = arrayOf("复制视频链接", "用系统播放器打开", "用浏览器打开")
        AlertDialog.Builder(this)
            .setTitle("视频操作")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("video", videoUrl))
                        Toast.makeText(this, "链接已复制，可以去下载器里下载了", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(videoUrl), "video/*")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "手机里没有能播放此格式的软件", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                        } catch (e: Exception) {
                            Toast.makeText(this, "未找到浏览器", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showFindDialog() {
        val currentWeb = (if (activePane == 1) webView1 else webView2) ?: return

        // 用一个自定义的 LinearLayout 充当整个弹窗
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)

            // 设置半透明黑底，带一点圆角
            val bg = android.graphics.drawable.GradientDrawable()
            bg.setColor(0x33000000.toInt()) // 92% 不透明度的黑色
            bg.cornerRadius = 30f
            background = bg
        }

        val etFind = EditText(this).apply {
            hint = "输入要查找的内容..."
            setSingleLine(true)
            setTextColor(0xFFFFFFFF.toInt()) // 白字
            setHintTextColor(0xFF888888.toInt()) // 灰色提示
            // 修改下划线颜色为粉色
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF66CC.toInt())
        }

        val tvResult = TextView(this).apply {
            setPadding(0, 20, 0, 30)
            setTextColor(0xFFFFD700.toInt()) // 金色提示
            text = "输入后自动查找"
            textSize = 12f
        }

        // 底部三个按钮的横向布局
        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        val btnPrev = TextView(this).apply {
            text = " 上一个 "
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(30, 15, 30, 15)
            setBackgroundResource(R.drawable.bg_input)
        }

        val btnNext = TextView(this).apply {
            text = " 下一个 "
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(30, 15, 30, 15)
            setBackgroundResource(R.drawable.bg_input)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = 20 }
        }

        val btnClose = TextView(this).apply {
            text = " 关闭 "
            setTextColor(0xFFFF3366.toInt()) // 红色关闭按钮
            setPadding(30, 15, 30, 15)
            setBackgroundResource(R.drawable.bg_input)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = 20 }
        }

        btnLayout.addView(btnPrev)
        btnLayout.addView(btnNext)
        btnLayout.addView(btnClose)

        container.addView(etFind)
        container.addView(tvResult)
        container.addView(btnLayout)

        // 【核心】不使用原生的 setTitle 和 setPositiveButton，全部自定义
        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .create()

        // 监听查找结果
        currentWeb.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (numberOfMatches > 0) {
                // 安卓的序号是从 0 开始的，所以 +1 显示给用户
                tvResult.text = "找到 ${numberOfMatches} 个结果，当前第 ${activeMatchOrdinal + 1} 个"
                tvResult.setTextColor(0xFFFF66CC.toInt())
            } else if (isDoneCounting) {
                tvResult.text = "未找到匹配结果"
                tvResult.setTextColor(0xFFFF3366.toInt())
            }
        }

        etFind.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    currentWeb.findAllAsync(query)
                } else {
                    currentWeb.clearMatches()
                    tvResult.text = "输入后自动查找"
                }
            }
        })

        btnNext.setOnClickListener { currentWeb.findNext(true) }
        btnPrev.setOnClickListener { currentWeb.findNext(false) }
        btnClose.setOnClickListener {
            currentWeb.clearMatches()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            currentWeb.clearMatches()
        }

        dialog.show()

        // 【核心】修改弹窗位置和背景
        dialog.window?.let { window ->
            // 1. 移到屏幕顶部
            window.setGravity(android.view.Gravity.TOP)

            // 2. 去掉系统默认的白底背景（否则自定义的黑底外面会有一圈白边）
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x00000000))

            // 3. 让弹窗铺满宽度，距离状态栏留一点距离
            val lp = window.attributes
            lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            lp.y = 100 // 距离顶部100像素（避免挡住状态栏）
            window.attributes = lp
        }
    }

    private fun showHelpDialog() {
        val helpText = """
    【核心操作与手势】
    - 底栏功能：清空（一键脱离卡死）、刷新、扩展、返回。
    - 双击中间黄条可进入全屏；全屏后点击顶部粗黄条恢复。
    - 点击底栏【扩展】，可唤出悬浮面板（含随机、自动、抓取工具），面板 8 秒后自动隐藏。
    - 顶栏支持【网页内查找】，输入关键词即可高亮跳转。
    - 预览区支持单窗、左右分屏、上下分屏自由切换与缩放。

    【资源提取神器（需点击底栏-扩展）】
    - 📝文本：一键剥离网页广告，提取纯净正文，进入【阅读模式】。支持字体放大缩小、上下翻页。
    - 🖼图片：一键提取网页所有高清图片，弹出瀑布流画廊。长按图片可直接保存到手机相册！
    - 🎬视频：智能嗅探网页中的视频流（M3U8/MP4），带封面列表展示，支持一键复制链接或调用系统播放器。

    【收藏与列表管理】
    - 顶栏点击【收藏】可保存当前网页；刚刚浏览过的网页会自动置顶并高亮显示【上次访问】。
    - 支持多分区切换（长按分区可重命名、删除、设为启动随机）。
    - 支持顶部搜索、按类型筛选，以及长按列表进入【批量操作】。
    - 长按单个列表项可进行：编辑标签、移动、删除、复制网址等。

    【安全与隐私】
    - 应用锁：在侧边栏可设置 4 位数字密码。切到后台超过 5 秒再回来，必须输入密码才能解锁！
    - 外部唤起：支持在微信/QQ直接分享网址给本 App，瞬间加载；分享 JSON 文件可直接恢复数据。
    - 广告拦截：底层拦截弹窗与跳转，支持自定义规则和白名单（防误杀）。

    【数据与外观】
    - 网页翻译：顶栏点击翻译；长按可切换引擎。
    - 记词本：侧边栏内置多语种记词本。
    - 外观设置：支持调用相册图片作为本地全屏背景，或修改预览区底色。
    - 数据备份：侧边栏支持导出/导入 JSON 数据及 HTML 书签，建议定期备份。
    - 退出恢复：退出 App 一天内重新打开，自动恢复上次的分屏布局和网页。

    提示：遇到网页黑屏或卡死时，直接点底部的【清空】即可满血复活。
""".trimIndent()

        AlertDialog.Builder(this)
            .setTitle("使用说明与全部功能")
            .setMessage(helpText)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showAddZoneDialog() {
        val input = EditText(this).apply { hint = "新分区名称" }
        AlertDialog.Builder(this).setTitle("新建分区").setView(input).setPositiveButton("创建") { _, _ -> val name = input.text.toString().trim(); if (name.isNotEmpty()) lifecycleScope.launch { db.appDao().insertZone(ZoneEntity(name = name)); loadData() } }.setNegativeButton("取消", null).show()
    }

    private fun showZoneMenu(zone: ZoneEntity) {
        val options = arrayOf("设为启动随机分区", "重命名", if (zone.isDefault) "" else "删除").filter { it.isNotEmpty() }
        AlertDialog.Builder(this).setTitle("分区：${zone.name}").setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                "设为启动随机分区" -> lifecycleScope.launch { db.appDao().clearAllRandomStart(); db.appDao().setRandomStart(zone.name); loadData() }
                "重命名" -> showRenameZoneDialog(zone)
                "删除" -> lifecycleScope.launch { db.appDao().deleteZone(zone.name); if (currentZone == zone.name) currentZone = "默认分区"; loadData() }
            }
        }.show()
    }

    private fun showRenameZoneDialog(zone: ZoneEntity) {
        val input = EditText(this).apply { setText(zone.name) }
        AlertDialog.Builder(this).setTitle("重命名分区").setView(input).setPositiveButton("保存") { _, _ -> val newName = input.text.toString().trim(); if (newName.isNotEmpty() && newName != zone.name) lifecycleScope.launch { db.appDao().insertZone(zone.copy(name = newName)); db.appDao().deleteZone(zone.name); loadData() } }.setNegativeButton("取消", null).show()
    }

    private fun showItemLongClickMenu(view: View, website: WebsiteEntity) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, "打开")
        popup.menu.add(0, 1, 0, "用浏览器打开")
        popup.menu.add(0, 3, 0, "编辑标题/标签/备注")
        popup.menu.add(0, 4, 0, "复制网址")
        popup.menu.add(0, 5, 0, "移动到其他分区")
        popup.menu.add(0, 8, 0, "删除")
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                0 -> { openUrlInPreview(website.url); true }
                1 -> { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(website.url))); true }
                3 -> { showEditDialog(website); true }
                4 -> { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("url", website.url)); Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show(); true }
                5 -> { showMoveToZoneDialog(website); true }
                8 -> { lifecycleScope.launch { db.appDao().deleteWebsite(website); filterAndDisplay() }; true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDialog(website: WebsiteEntity) {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val etTitle = EditText(this).apply { hint = "标题"; setText(website.title) }
        val etTags = EditText(this).apply { hint = "标签(逗号分隔)"; setText(website.tags) }
        val etNote = EditText(this).apply { hint = "备注"; setText(website.note) }
        container.addView(etTitle); container.addView(etTags); container.addView(etNote)
        AlertDialog.Builder(this).setTitle("编辑收藏").setView(container).setPositiveButton("保存") { _, _ -> lifecycleScope.launch { db.appDao().updateWebsite(website.copy(title = etTitle.text.toString(), tags = etTags.text.toString(), note = etNote.text.toString())); filterAndDisplay() } }.setNegativeButton("取消", null).show()
    }

    private fun showMoveToZoneDialog(website: WebsiteEntity) {
        val zoneNames = allZones.map { it.name }.filter { it != website.zoneName }.toTypedArray()
        if (zoneNames.isEmpty()) { Toast.makeText(this, "没有其他分区", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("移动到").setItems(zoneNames) { _, which -> lifecycleScope.launch { db.appDao().moveWebsiteToZone(website.url, zoneNames[which]); filterAndDisplay() } }.show()
    }

    private fun showLinkLongClickMenu(url: String) {
        val options = arrayOf("复制链接", "当前窗打开", "在新预览窗打开", "用浏览器打开")
        AlertDialog.Builder(this).setTitle(url).setItems(options) { _, which ->
            when (which) {
                0 -> { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("url", url)); Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show() }
                1 -> openUrlInPreview(url)
                2 -> { activePane = if (activePane == 1) 2 else 1; openUrlInPreview(url) }
                3 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }.show()
    }

    private fun showImageLongClickMenu(imageUrl: String) {
        val options = arrayOf("保存图片", "复制图片地址", "在新预览窗打开")
        AlertDialog.Builder(this).setItems(options) { _, which ->
            when (which) {
                0 -> { val request = DownloadManager.Request(Uri.parse(imageUrl)); request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "GoonerWebFav/" + System.currentTimeMillis() + ".jpg"); (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request); Toast.makeText(this, "已开始保存图片", Toast.LENGTH_SHORT).show() }
                1 -> { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("image", imageUrl)); Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show() }
                2 -> { activePane = if (activePane == 1) 2 else 1; openUrlInPreview(imageUrl) }
            }
        }.show()
    }

    private fun toggleSelectMode() { isSelectMode = !isSelectMode; selectedUrls.clear(); if (!isSelectMode) hideBatchBar() else showBatchBar(); filterAndDisplay() }

    private fun showBatchBar() {
        if (batchBar != null) return
        batchBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFF0F0A14.toInt()); setPadding(20, 20, 20, 20); layoutParams = FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.BOTTOM } }
        val btnSelectAll = TextView(this).apply { text = "全选"; setTextColor(Color.WHITE); setPadding(20, 0, 20, 0); setOnClickListener { allWebsitesInZone.forEach { selectedUrls.add(it.url) }; filterAndDisplay() } }
        val btnMove = TextView(this).apply { text = "移动"; setTextColor(Color.WHITE); setPadding(20, 0, 20, 0); setOnClickListener { showBatchMoveDialog() } }
        val btnTag = TextView(this).apply {
            text = "打标签"; setTextColor(Color.WHITE); setPadding(20, 0, 20, 0);
            setOnClickListener {
                val input = EditText(this@MainActivity).apply { hint = "输入标签，逗号分隔" }
                AlertDialog.Builder(this@MainActivity).setTitle("批量打标签").setView(input).setPositiveButton("确定") { _, _ ->
                    val tagStr = input.text.toString().trim()
                    if (tagStr.isNotEmpty()) {
                        lifecycleScope.launch {
                            allWebsitesInZone.filter { selectedUrls.contains(it.url) }.forEach {
                                db.appDao().updateWebsite(it.copy(tags = if(it.tags.isBlank()) tagStr else "${it.tags},$tagStr"))
                            }
                            Toast.makeText(this@MainActivity, "已打标签", Toast.LENGTH_SHORT).show(); toggleSelectMode()
                        }
                    }
                }.setNegativeButton("取消", null).show()
            }
        }
        val btnDelete = TextView(this).apply { text = "删除"; setTextColor(Color.WHITE); setPadding(20, 0, 20, 0); setOnClickListener { lifecycleScope.launch { allWebsitesInZone.filter { selectedUrls.contains(it.url) }.forEach { db.appDao().deleteWebsite(it) }; toggleSelectMode() } } }
        batchBar?.addView(btnSelectAll); batchBar?.addView(btnMove); batchBar?.addView(btnTag); batchBar?.addView(btnDelete)
        (previewContainer.parent as? ViewGroup)?.addView(batchBar)
    }

    private fun hideBatchBar() { (previewContainer.parent as? ViewGroup)?.removeView(batchBar); batchBar = null }

    private fun showBatchMoveDialog() {
        val zoneNames = allZones.map { it.name }.filter { it != currentZone }.toTypedArray()
        if (zoneNames.isEmpty()) { Toast.makeText(this, "没有其他分区", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("移动到").setItems(zoneNames) { _, which -> lifecycleScope.launch { selectedUrls.forEach { url -> db.appDao().moveWebsiteToZone(url, zoneNames[which]) }; toggleSelectMode() } }.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingWindow() {
        val currentUrl = webView1?.url
        if (currentUrl == null || currentUrl.startsWith("about:")) { Toast.makeText(this, "当前无网页", Toast.LENGTH_SHORT).show(); return }

        val floatRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F0A14.toInt())
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            layoutParams = RelativeLayout.LayoutParams((screenWidth * 0.85).toInt(), (screenHeight * 0.65).toInt()).apply {
                leftMargin = 100
                topMargin = 200
            }
            background = GradientDrawable().apply { setStroke(4, 0xFFFF66CC.toInt()) }
            tag = floatTag // 【核心】打上标签
        }

        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF301A3E.toInt())
            setPadding(15, 10, 15, 10)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }

        val dragHandle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }

        val titleTv = TextView(this).apply {
            text = try { URI(currentUrl).host } catch (e: Exception) { "浮动窗口" }
            setTextColor(0xFFFF66CC.toInt()); textSize = 12f; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        dragHandle.addView(titleTv, LinearLayout.LayoutParams(-1, -2))

        val changeBtn = TextView(this).apply { text = "✎"; setTextColor(Color.WHITE); textSize = 16f; setPadding(15, 0, 15, 0) }
        val refreshBtn = TextView(this).apply { text = "↻"; setTextColor(Color.WHITE); textSize = 16f; setPadding(15, 0, 15, 0) }
        val zoomOut = TextView(this).apply { text = "−"; setTextColor(Color.WHITE); setPadding(10, 0, 10, 0) }
        val zoomReset = TextView(this).apply { text = "1:1"; setTextColor(Color.WHITE); setPadding(10, 0, 10, 0) }
        val zoomIn = TextView(this).apply { text = "+"; setTextColor(Color.WHITE); setPadding(10, 0, 10, 0) }

        titleBar.addView(dragHandle)
        titleBar.addView(zoomOut)
        titleBar.addView(zoomReset)
        titleBar.addView(zoomIn)
        titleBar.addView(changeBtn)
        titleBar.addView(refreshBtn)
        floatRoot.addView(titleBar)

        val fwWeb = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            settings.builtInZoomControls = true; settings.displayZoomControls = false; settings.setSupportZoom(true)

            // 浮动窗口广告拦截逻辑
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    if (!adBlockEnabled) return super.shouldInterceptRequest(view, request)
                    val url = request?.url?.toString()?.lowercase() ?: return super.shouldInterceptRequest(view, request)

                    // 白名单放行
                    try {
                        val requestHost = request?.url?.host ?: ""
                        for (whiteHost in adWhiteList) {
                            if (requestHost.contains(whiteHost)) {
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                    } catch (e: Exception) {}

                    // 黑名单拦截
                    for (host in defaultAdHosts) {
                        if (url.contains(host)) {
                            return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream("".toByteArray()))
                        }
                    }
                    // 自定义黑名单拦截
                    if (customAdRuleList.isNotEmpty()) {
                        for (rule in customAdRuleList) {
                            if (url.contains(rule)) {
                                return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream("".toByteArray()))
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            webChromeClient = WebChromeClient()
            loadUrl(currentUrl)
        }
        floatRoot.addView(fwWeb)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1A1026.toInt())
            setPadding(15, 10, 15, 10)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }



        val resizeBtn = TextView(this).apply {
            text = "拖拽缩放"
            setTextColor(Color.WHITE); textSize = 14f
            setBackgroundColor(0xFFFF66CC.toInt())
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = 10 }
        }


        bottomBar.addView(resizeBtn)
        floatRoot.addView(bottomBar)

        changeBtn.setOnClickListener {
            val inputUrl = EditText(this@MainActivity).apply { setText(currentUrl) }
            AlertDialog.Builder(this@MainActivity).setTitle("换网址").setView(inputUrl).setPositiveButton("确定") { _, _ ->
                val newUrl = inputUrl.text.toString().trim()
                if (newUrl.isNotEmpty()) fwWeb.loadUrl(if (!newUrl.startsWith("http")) "http://$newUrl" else newUrl)
            }.setNegativeButton("取消", null).show()
        }
        refreshBtn.setOnClickListener { fwWeb.reload() }
        zoomOut.setOnClickListener { fwWeb.settings.textZoom = (fwWeb.settings.textZoom - 10).coerceAtLeast(30) }
        zoomReset.setOnClickListener { fwWeb.settings.textZoom = 100 }
        zoomIn.setOnClickListener { fwWeb.settings.textZoom = (fwWeb.settings.textZoom + 10).coerceAtMost(300) }

        var lastX = 0f; var lastY = 0f
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.rawX; lastY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    val lp = floatRoot.layoutParams as RelativeLayout.LayoutParams
                    lp.leftMargin = (lp.leftMargin + (event.rawX - lastX).toInt()).coerceIn(0, resources.displayMetrics.widthPixels - floatRoot.width)
                    lp.topMargin = (lp.topMargin + (event.rawY - lastY).toInt()).coerceIn(0, resources.displayMetrics.heightPixels - floatRoot.height)
                    floatRoot.layoutParams = lp; lastX = event.rawX; lastY = event.rawY; true
                }
                else -> false
            }
        }

        var rsW = 0; var rsH = 0; var rsX = 0f; var rsY = 0f
        resizeBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { rsX = event.rawX; rsY = event.rawY; rsW = floatRoot.width; rsH = floatRoot.height; true }
                MotionEvent.ACTION_MOVE -> {
                    val lp = floatRoot.layoutParams as RelativeLayout.LayoutParams
                    lp.width = (rsW + (event.rawX - rsX).toInt()).coerceAtLeast(400)
                    lp.height = (rsH + (event.rawY - rsY).toInt()).coerceAtLeast(300)
                    floatRoot.layoutParams = lp; true
                }
                else -> false
            }
        }

        findViewById<ViewGroup>(R.id.mainRoot)?.addView(floatRoot)
    }

    private fun doRandomJump() {
        lifecycleScope.launch {
            // 1. 从数据库实时读取当前分区的所有网址
            val list = db.appDao().getWebsitesInZone(currentZone).toMutableList()
            if (list.isEmpty()) {
                Toast.makeText(this@MainActivity, "当前分区没有可随机的网址", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 2. 应用你在顶部的搜索过滤
            var filtered = list
            if (searchKeyword.isNotEmpty()) {
                filtered = filtered.filter {
                    it.title.contains(searchKeyword, true) || it.url.contains(searchKeyword, true) ||
                            it.note.contains(searchKeyword, true) || it.tags.contains(searchKeyword, true)
                }.toMutableList()
            }

            // 3. 应用你在底部的分类筛选（如：只看图片、只看视频等）
            when (currentFilter) {
                "image" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("image", true) || it.url.contains("pic", true) || it.url.contains("photo", true) || it.url.contains("img", true) || it.url.contains("jpg", true) || it.url.contains("png", true)
                    val matchTitle = it.title.contains("图", true) || it.title.contains("image", true) || it.title.contains("pic", true)
                    matchUrl || matchTitle
                }.toMutableList()
                "video" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("video", true) || it.url.contains("play", true) || it.url.contains("watch", true) || it.url.contains("mp4", true)
                    val matchTitle = it.title.contains("视频", true) || it.title.contains("video", true)
                    matchUrl || matchTitle
                }.toMutableList()
                "audio" -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("audio", true) || it.url.contains("music", true) || it.url.contains("mp3", true)
                    val matchTitle = it.title.contains("音", true) || it.title.contains("audio", true) || it.title.contains("music", true)
                    matchUrl || matchTitle
                }.toMutableList()
                "text"  -> filtered = filtered.filter {
                    val matchUrl = it.url.contains("text", true) || it.url.contains("txt", true) || it.url.contains("read", true)
                    val matchTitle = it.title.contains("文", true) || it.title.contains("read", true)
                    matchUrl || matchTitle
                }.toMutableList()
                "unvisited" -> filtered = filtered.filter { it.visitCount == 0 }.toMutableList()
            }

            if (filtered.isEmpty()) {
                Toast.makeText(this@MainActivity, "当前筛选条件下没有可随机的网页", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 4. 洗牌逻辑（防连续重复）
            val currentPoolUrls = filtered.map { it.url }.toSet()
            val playlistUrls = randomPlaylist.map { it.url }.toSet()

            if (randomPlaylist.isEmpty() || !currentPoolUrls.containsAll(playlistUrls)) {
                randomPlaylist.clear()
                randomPlaylist.addAll(filtered.shuffled())
            }

            // 5. 获取当前正在浏览的网址，防止原地刷新
            val currentWeb = if (activePane == 1) webView1 else webView2
            val currentUrl = currentWeb?.url ?: ""

            var nextItem = randomPlaylist.removeAt(0)
            if (nextItem.url == currentUrl && randomPlaylist.isNotEmpty()) {
                randomPlaylist.add(nextItem)
                nextItem = randomPlaylist.removeAt(0)
            }

            // 6. 打开网页（如果是图片，之前改过的 openUrlInPreview 会自动处理）
            openUrlInPreview(nextItem.url)
        }
    }

    private fun toggleAutoJump(btn: TextView) { if (isAutoRunning) { autoJob?.cancel(); isAutoRunning = false; btn.text = "▶ 自动" } else { isAutoRunning = true; btn.text = "⏹ 停止"; autoJob = lifecycleScope.launch { while (isAutoRunning) { doRandomJump(); delay(Random.nextInt(autoMinSec, autoMaxSec + 1) * 1000L) } } } }

    private fun showNotepadDialog() {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val etSearch = EditText(this).apply { hint = "搜索词条..."; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        val btnAdd = TextView(this).apply { text = " + 新建 "; textSize = 14f; setTextColor(Color.WHITE); setBackgroundColor(0xFFFF66CC.toInt()); setPadding(30, 15, 30, 15) }
        topBar.addView(etSearch); topBar.addView(btnAdd)
        val scrollView = ScrollView(this); val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; scrollView.addView(listContainer); scrollView.layoutParams = LinearLayout.LayoutParams(-1, 600).apply { topMargin = 20 }

        fun refreshList(keyword: String = "") {
            listContainer.removeAllViews()
            lifecycleScope.launch {
                val list = db.appDao().searchNotepad(keyword)
                if (list.isEmpty()) {
                    val emptyTv = TextView(this@MainActivity).apply { text = "暂无词条，点击上方新建"; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 50, 0, 0) }; listContainer.addView(emptyTv)
                } else {
                    list.forEach { item ->
                        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF222222.toInt()); setPadding(20, 20, 20, 20) }
                        var jsonArr: JSONArray? = null; try { jsonArr = JSONArray(item.translations) } catch (e: Exception) {}
                        if (jsonArr != null) {
                            for (j in 0 until jsonArr.length()) {
                                try {
                                    val obj = jsonArr.getJSONObject(j); val lang = obj.getString("lang"); val text = obj.getString("text")
                                    val langTv = TextView(this@MainActivity).apply { this.text = "[$lang] $text"; setTextColor(Color.WHITE); textSize = 14f; setPadding(0, 10, 0, 10) }
                                    langTv.setOnClickListener { val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("text", text)); Toast.makeText(this@MainActivity, "已复制", Toast.LENGTH_SHORT).show() }
                                    row.addView(langTv)
                                } catch (e: Exception) {}
                            }
                        }
                        val btnBar = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 20, 0, 0) }
                        val btnAppend = TextView(this@MainActivity).apply {
                            text = "添加语种"; setTextColor(Color.CYAN); setPadding(20, 10, 20, 10)
                            setOnClickListener {
                                val addContainer = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                                val etLang = EditText(this@MainActivity).apply { hint = "语种名称" }
                                val etText = EditText(this@MainActivity).apply { hint = "词条内容" }
                                addContainer.addView(etLang); addContainer.addView(etText)
                                AlertDialog.Builder(this@MainActivity).setTitle("追加新语种").setView(addContainer).setPositiveButton("保存") { _, _ ->
                                    val newLang = etLang.text.toString().trim().ifBlank { "默认" }; val newText = etText.text.toString().trim()
                                    if (newText.isNotEmpty()) { lifecycleScope.launch { val currentArr = JSONArray(item.translations); val newObj = JSONObject(); newObj.put("lang", newLang); newObj.put("text", newText); currentArr.put(newObj); db.appDao().updateNotepad(item.copy(translations = currentArr.toString())); Toast.makeText(this@MainActivity, "已追加", Toast.LENGTH_SHORT).show(); refreshList(etSearch.text.toString()) } }
                                }.setNegativeButton("取消", null).show()
                            }
                        }
                        val btnDelete = TextView(this@MainActivity).apply { text = "删除词条"; setTextColor(Color.RED); setPadding(20, 10, 20, 10); setOnClickListener { lifecycleScope.launch { db.appDao().deleteNotepad(item); Toast.makeText(this@MainActivity, "已删除", Toast.LENGTH_SHORT).show(); refreshList(etSearch.text.toString()) } } }
                        btnBar.addView(btnAppend); btnBar.addView(btnDelete); row.addView(btnBar)
                        listContainer.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 15 })
                    }
                }
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshList(s.toString()) }; override fun afterTextChanged(s: Editable?) {} })

        btnAdd.setOnClickListener {
            val addContainer = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            val etTitle = EditText(this@MainActivity).apply { hint = "词条名称" }; val etLang = EditText(this@MainActivity).apply { hint = "语种名称" }; val etText = EditText(this@MainActivity).apply { hint = "词条内容" }
            addContainer.addView(etTitle); addContainer.addView(etLang); addContainer.addView(etText)
            AlertDialog.Builder(this@MainActivity).setTitle("新建词条").setView(addContainer).setPositiveButton("保存") { _, _ ->
                val title = etTitle.text.toString().trim().ifBlank { "未命名" }; val lang = etLang.text.toString().trim().ifBlank { "默认" }; val text = etText.text.toString().trim()
                if (text.isNotEmpty()) { lifecycleScope.launch { val jsonArr = JSONArray(); val obj = JSONObject(); obj.put("lang", lang); obj.put("text", text); jsonArr.put(obj); db.appDao().insertNotepad(NotepadEntity(title = title, translations = jsonArr.toString())); Toast.makeText(this@MainActivity, "保存成功", Toast.LENGTH_SHORT).show(); refreshList(etSearch.text.toString()) } }
                else { Toast.makeText(this@MainActivity, "内容不能为空", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("取消", null).show()
        }
        container.addView(topBar); container.addView(scrollView); refreshList(); AlertDialog.Builder(this).setTitle("记词本").setView(container).setPositiveButton("关闭", null).show()
    }

    private fun showAppearanceDialog() {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val btnSelectBg = TextView(this).apply {
            text = "从相册选择背景图"; setTextColor(Color.WHITE); setBackgroundColor(0xFFFF66CC.toInt()); gravity = Gravity.CENTER; setPadding(20, 20, 20, 20)
            setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        }
        container.addView(btnSelectBg)
        val btnClearBg = TextView(this).apply {
            text = "恢复默认背景"; setTextColor(Color.RED); gravity = Gravity.CENTER; setPadding(20, 20, 20, 20)
            setOnClickListener { prefs.edit().remove("custom_bg_uri").apply(); ivBackground.visibility = View.GONE; loadCustomBackground(); Toast.makeText(this@MainActivity, "已恢复默认", Toast.LENGTH_SHORT).show() }
        }
        container.addView(btnClearBg)
        val tvHint = TextView(this).apply { text = "修改预览区底色(HEX):"; setTextColor(Color.GRAY); setPadding(0, 30, 0, 10) }
        container.addView(tvHint)
        val etColor = EditText(this).apply { hint = "输入色值如 #FF0000"; setText("#0a0a0f") }
        container.addView(etColor)
        AlertDialog.Builder(this).setTitle("外观设置").setView(container).setPositiveButton("应用颜色") { _, _ ->
            try { val color = Color.parseColor(etColor.text.toString()); previewContainer.setBackgroundColor(color); Toast.makeText(this, "颜色已应用", Toast.LENGTH_SHORT).show() }
            catch (e: Exception) { Toast.makeText(this, "格式错误", Toast.LENGTH_SHORT).show() }
        }.setNegativeButton("关闭", null).show()
    }

    private fun addWebsite(input: String) {
        if (input.isEmpty()) return
        lifecycleScope.launch {
            var added = 0
            input.split("\\s+".toRegex()).forEach {
                var u = it; if (!u.startsWith("http")) u = "http://$u"
                if (db.appDao().insertWebsite(WebsiteEntity(url = u, zoneName = currentZone)) != -1L) added++
            }
            Toast.makeText(this@MainActivity, if (added > 0) "你已收藏 $added 个" else "已存在", Toast.LENGTH_SHORT).show(); filterAndDisplay()
        }
    }

    private fun updateVisitCountAndTitle(url: String, title: String) {
        lifecycleScope.launch {
            val item = db.appDao().getAllWebsites().find { url.startsWith(it.url) }
            if (item != null) {
                db.appDao().updateWebsite(item.copy(visitCount = item.visitCount + 1, lastVisit = System.currentTimeMillis(), title = if (item.title.isBlank()) title else item.title))
                // 👇 核心：访问后立刻刷新列表，让刚刚点的网页跑上去
                filterAndDisplay()
            }
        }
    }

    private fun clearWebLoginState() {
        AlertDialog.Builder(this).setMessage("确定清除网页登录状态？").setPositiveButton("确定") { _, _ ->
            CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush()
            webView1?.clearCache(true); webView2?.clearCache(true); Toast.makeText(this, "已清除", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("取消", null).show()
    }

    private fun updateBuiltinSites() {
        lifecycleScope.launch {
            sbBuiltinContainer.removeAllViews()
            val defaults = db.appDao().getWebsitesInZone("默认分区").take(6)
            defaults.forEach { site ->
                val btn = TextView(this@MainActivity).apply {
                    text = site.title.ifBlank { site.url }; setTextColor(Color.WHITE); setBackgroundColor(0x33000000); setPadding(20, 10, 20, 10)
                    setOnClickListener { openUrlInPreview(site.url) }
                }
                sbBuiltinContainer.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 5 })
            }
        }
    }
}
// 👇 放在 MainActivity 的大括号外面 👇
class GalleryAdapter(private val imageUrls: List<String>, private val refererUrl: String) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null

    class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val iv = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = true
            setPadding(4, 4, 4, 4)
        }
        return GalleryViewHolder(iv)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val url = imageUrls[position]
        holder.ivImage.setImageDrawable(null)

        holder.ivImage.setOnClickListener {
            onItemClick?.invoke(url)
        }

        holder.ivImage.setOnLongClickListener {
            try {
                val context = holder.ivImage.context
                val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, "GoonerGallery/" + System.currentTimeMillis() + ".jpg")
                val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                dm.enqueue(request)
                android.widget.Toast.makeText(context, "已开始保存图片", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(holder.ivImage.context, "保存失败", android.widget.Toast.LENGTH_SHORT).show()
            }
            true
        }

        // 👇 【核心修复】使用 HttpURLConnection 携带 Referer，破解防盗链 👇
        Thread {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
                if (refererUrl.isNotEmpty()) {
                    conn.setRequestProperty("Referer", refererUrl)
                }
                conn.connect()
                val inputStream = conn.inputStream
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        holder.ivImage.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                // 如果带 Referer 失败，尝试不带 Referer 的普通请求兜底
                try {
                    val inputStream = java.net.URL(url).openStream()
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            holder.ivImage.setImageBitmap(bitmap)
                        }
                    }
                } catch (ex: Exception) {}
            }
        }.start()
    }

    override fun getItemCount(): Int = imageUrls.size
}

class SniffedVideoAdapter(private val videoData: List<Triple<String, String, String>>) : RecyclerView.Adapter<SniffedVideoAdapter.VideoViewHolder>() {

    var onItemClick: ((String) -> Unit)? = null

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivVideoCover)
        val tvName: TextView = view.findViewById(R.id.tvVideoName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sniffed_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        // 👇 这里改成了解构三段数据
        val (url, cover, name) = videoData[position]

        // 直接显示抓到的名字
        holder.tvName.text = if (name.isNotBlank()) name else url

        // 加载封面图
        if (cover.isNotEmpty()) {
            Thread {
                try {
                    val conn = java.net.URL(cover).openConnection() as java.net.HttpURLConnection
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    val bitmap = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    if (bitmap != null) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            holder.ivCover.setImageBitmap(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        holder.ivCover.setImageResource(android.R.drawable.ic_media_play)
                    }
                }
            }.start()
        } else {
            holder.ivCover.setImageResource(android.R.drawable.ic_media_play)
        }

        holder.itemView.setOnClickListener { onItemClick?.invoke(url) }
    }

    override fun getItemCount() = videoData.size
}