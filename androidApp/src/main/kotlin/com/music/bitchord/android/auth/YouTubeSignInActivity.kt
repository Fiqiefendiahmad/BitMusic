package com.music.bitchord.android.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.music.bitchord.android.ui.theme.BitMusicCompactTheme

class YouTubeSignInActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)

        setContent {
            BitMusicCompactTheme(darkTheme = true) {
                var canFinish by remember { mutableStateOf(false) }
                var currentUrl by remember { mutableStateOf(START_URL) }
                fun refreshState() {
                    val cookie = manager.getCookie(MUSIC_URL).orEmpty()
                    canFinish = AndroidYouTubeAuth.hasSigningSecret(cookie)
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Sign in") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                Button(
                                    onClick = {
                                        val cookie = manager.getCookie(MUSIC_URL).orEmpty()
                                        if (AndroidYouTubeAuth.hasSigningSecret(cookie)) {
                                            AndroidYouTubeAuth.adopt(cookie, persist = true, context = this@YouTubeSignInActivity)
                                            Toast.makeText(this@YouTubeSignInActivity, "Signed in", Toast.LENGTH_SHORT).show()
                                            setResult(Activity.RESULT_OK)
                                            finish()
                                        } else {
                                            Toast.makeText(this@YouTubeSignInActivity, "Open YouTube Music and complete sign-in first", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = canFinish,
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Done") }
                            },
                        )
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.userAgentString = settings.userAgentString.replace("; wv", "")
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            currentUrl = url.orEmpty()
                                            refreshState()
                                        }
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                        ): Boolean = false
                                    }
                                    loadUrl(START_URL)
                                }
                            },
                            update = { refreshState() },
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val MUSIC_URL = "https://music.youtube.com/"
        const val START_URL =
            "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true" +
                "&continue=https%3A%2F%2Fmusic.youtube.com%2F"
    }
}
