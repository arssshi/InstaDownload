package com.vakarux.instadownload

import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.vakarux.instadownload.ui.AppIcons

class LoginActivity : ComponentActivity() {

    private lateinit var sessionStore: SessionStore

    @OptIn(ExperimentalMaterial3Api::class)
    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionStore = SessionStore(this)

        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    !isInstagramUrl(request.url)

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    !isInstagramUrl(Uri.parse(url))

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    captureSessionIfPresent()
                }
            }
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            removeAllCookies { webView.loadUrl("https://www.instagram.com/accounts/login/") }
            flush()
        }

        val theme = AppSettings(this).theme
        val topBar = ComposeView(this).apply {
            setContent {
                val darkTheme = when (theme) {
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                }
                InstaDownloadTheme(darkTheme = darkTheme) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.login_title)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(AppIcons.Close, contentDescription = stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(topBar)
            addView(webView)
        })
    }

    private fun isInstagramUrl(uri: Uri): Boolean =
        uri.scheme == "https" &&
            (uri.host == "instagram.com" || uri.host?.endsWith(".instagram.com") == true)

    private fun captureSessionIfPresent() {
        val cookies = CookieManager.getInstance()
            .getCookie("https://www.instagram.com") ?: return

        val map = cookies.split(";")
            .mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) null
                else part.substring(0, eq).trim() to part.substring(eq + 1).trim()
            }
            .toMap()

        val sessionId = map["sessionid"]
        if (!sessionId.isNullOrEmpty()) {
            sessionStore.save(
                sessionId = sessionId,
                csrfToken = map["csrftoken"],
                userId = map["ds_user_id"]
            )
            setResult(RESULT_OK)
            finish()
        }
    }
}
