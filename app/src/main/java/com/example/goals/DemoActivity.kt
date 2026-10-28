package com.example.goals

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity

/** Просмотр сцены без сохранения: кнопки «Демо» и «Новый день» открыты, настоящий прогресс не трогается. */
class DemoActivity : ComponentActivity() {
    private lateinit var wv: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wv = createPathWebView(this, true)
        setContentView(wv)
    }

    override fun onPause() {
        super.onPause()
        wv.onPause()
    }

    override fun onResume() {
        super.onResume()
        wv.onResume()
    }

    override fun onDestroy() {
        wv.destroy()
        super.onDestroy()
    }
}
