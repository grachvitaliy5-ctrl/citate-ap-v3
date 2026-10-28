package com.example.goals

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/** Мост между сценой (JavaScript) и приложением. Сцена хранит своё состояние здесь, а не в браузере. */
class PathBridge(context: Context) {
    private val ctx = context.applicationContext

    @JavascriptInterface
    fun load(): String = Store(ctx).pathState()

    @JavascriptInterface
    fun save(json: String) {
        Store(ctx).setPathState(json)
        try {
            Scheduler.scheduleAll(ctx)
        } catch (e: Exception) {
        }
    }

    @JavascriptInterface
    fun today(): Int = Store(ctx).today().toInt()

    @JavascriptInterface
    fun pending(): String = Store(ctx).pendingJson()

    @JavascriptInterface
    fun ack(id: String, day: Int) {
        val rid = id.toLongOrNull() ?: return
        Store(ctx).ack(rid, day.toLong())
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun createPathWebView(context: Context, demo: Boolean): WebView {
    val wv = WebView(context)
    wv.setBackgroundColor(0xFF0B0A1F.toInt())
    wv.overScrollMode = View.OVER_SCROLL_NEVER
    val s = wv.settings
    s.javaScriptEnabled = true
    s.domStorageEnabled = true
    s.mediaPlaybackRequiresUserGesture = false
    s.allowContentAccess = false
    s.setSupportZoom(false)
    s.textZoom = 100
    val loader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    wv.webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            loader.shouldInterceptRequest(request.url)
    }
    WebView.setWebContentsDebuggingEnabled(true)
    wv.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(m: ConsoleMessage): Boolean {
            Log.i("PathJS", "${m.messageLevel()} ${m.message()} @${m.sourceId()}:${m.lineNumber()}")
            return true
        }
    }
    wv.addJavascriptInterface(PathBridge(context), "Android")
    wv.loadUrl("https://appassets.androidplatform.net/assets/path/index.html" + if (demo) "?demo=1" else "")
    return wv
}
