package com.example.goals

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    wv.addJavascriptInterface(PathBridge(context), "Android")
    wv.loadUrl("https://appassets.androidplatform.net/assets/path/index.html" + if (demo) "?demo=1" else "")
    return wv
}

private const val JS_SAVE = "window.pathSave&&window.pathSave()"
private const val JS_RESUME = "window.pathResume&&window.pathResume()"

/** Сцена живёт, пока открыто приложение: при смене вкладки она только приостанавливается. */
@Composable
fun PathHost(visible: Boolean) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val wv = remember { createPathWebView(ctx, false) }

    DisposableEffect(lifecycle, visible) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE) {
                wv.evaluateJavascript(JS_SAVE, null)
                wv.onPause()
            } else if (e == Lifecycle.Event.ON_RESUME && visible) {
                wv.onResume()
                wv.evaluateJavascript(JS_RESUME, null)
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(visible) {
        if (visible) {
            wv.onResume()
            wv.evaluateJavascript(JS_RESUME, null)
        } else {
            wv.evaluateJavascript(JS_SAVE, null)
            wv.onPause()
        }
    }
    DisposableEffect(Unit) {
        onDispose { wv.destroy() }
    }
    AndroidView(
        factory = { wv },
        modifier = Modifier.fillMaxSize().alpha(if (visible) 1f else 0f),
        update = { it.visibility = if (visible) View.VISIBLE else View.INVISIBLE }
    )
}
