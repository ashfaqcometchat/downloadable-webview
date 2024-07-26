package com.example.afrozkeliyewebview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupWebView()
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url ?: return true)
                return true
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            defaultTextEncodingName = "utf-8";
        }

        webView.addJavascriptInterface(JavaScriptInterface(this), "Android")

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (url.startsWith("blob:")) {
                webView.evaluateJavascript(JavaScriptInterface.getBase64StringFromBlobUrl(url, mimetype)) {
                    // No-op
                }
            } else {
                try {
                    // Use DownloadManager for handling downloads
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setTitle("Downloading file...")
                        .setDescription("Downloading file from web")
                        .setMimeType(mimetype)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, getFileName(contentDisposition))

                    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)

                    Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to handle download", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "setupWebView: ${e.message}")
                }
            }
        }

        webView.loadUrl("https://66a0cbf3d54a9cede2938219--funny-sable-737e96.netlify.app/login") // Your URL here
    }


    private fun getFileName(contentDisposition: String?): String {
        if (contentDisposition == null) return "downloadedfile"
        val regex = "filename=\"([^\"]*)\"".toRegex()
        val match = regex.find(contentDisposition)
        return match?.groupValues?.get(1) ?: "downloadedfile"
    }
}