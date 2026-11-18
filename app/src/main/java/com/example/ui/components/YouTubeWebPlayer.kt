package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.SurfaceLevel2
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private class YouTubePlayerBridge(
    private val mainHandler: Handler,
    private val onErrorCallback: (Int) -> Unit
) {
    @JavascriptInterface
    fun onPlayerError(errorCode: Int) {
        mainHandler.post {
            onErrorCallback(errorCode)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    backgroundPlayEnabled: Boolean = true,
    onSwitchToNativePlayer: (() -> Unit)? = null,
    onEmbedError: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var retryNonce by remember(videoId) { mutableIntStateOf(0) }
    var rendererCrashed by remember(videoId, retryNonce) { mutableStateOf(false) }

    val currentSwitchToNative by rememberUpdatedState(onSwitchToNativePlayer)
    val currentEmbedError by rememberUpdatedState(onEmbedError)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    if (rendererCrashed) {
        Box(
            modifier = modifier
                .background(Color.Black)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Web player renderer stopped on this device",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Switch to Native ExoPlayer for smooth & lock-screen playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onSwitchToNativePlayer != null) {
                        Button(
                            onClick = onSwitchToNativePlayer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonAccent,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Hd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Native ExoPlayer")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            rendererCrashed = false
                            retryNonce++
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceLevel2,
                            contentColor = CyanElectric
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Web")
                    }
                }
            }
        }
        return
    }

    var isDestroyed by remember(retryNonce) { mutableStateOf(false) }

    val webView = remember(context, retryNonce) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            val webViewInstance = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webViewInstance, true)
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                val defaultUa = userAgentString.orEmpty()
                if (defaultUa.contains("; wv")) {
                    userAgentString = defaultUa.replace("; wv", "")
                }
            }
            addJavascriptInterface(
                YouTubePlayerBridge(mainHandler) { errorCode ->
                    val handler = currentEmbedError
                    if (handler != null) {
                        handler(errorCode)
                    } else {
                        currentSwitchToNative?.invoke()
                    }
                },
                "YmnTubeBridge"
            )
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        return true
                    }
                    return !(url.contains("youtube.com/embed") ||
                        url.contains("youtube-nocookie.com/embed") ||
                        url.contains("youtube.com/iframe_api"))
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    if (!isDestroyed) {
                        isDestroyed = true
                        (view?.parent as? ViewGroup)?.removeView(view)
                        runCatching { view?.destroy() }
                    }
                    rendererCrashed = true
                    currentSwitchToNative?.invoke()
                    return true
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            if (!isDestroyed) {
                isDestroyed = true
                runCatching {
                    webView.removeJavascriptInterface("YmnTubeBridge")
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.destroy()
                }
            }
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webView },
            update = { view ->
                if (isDestroyed) return@AndroidView
                val loadKey = "$videoId|$backgroundPlayEnabled"
                if (view.tag != loadKey) {
                    view.tag = loadKey
                    val safeId = videoId.replace(Regex("[^A-Za-z0-9_-]"), "")
                    val visibilityShim = if (backgroundPlayEnabled) {
                        """
                        try {
                          Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                          Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                          window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                          document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                        } catch (_) {}
                        """.trimIndent()
                    } else ""
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                          <style>
                            html, body, #ytplayer { margin: 0; padding: 0; width: 100%; height: 100%; background: #000000; overflow: hidden; }
                            iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
                          </style>
                        </head>
                        <body>
                          <div id="ytplayer"></div>
                          <script>
                            $visibilityShim
                            var errorReported = false;
                            function reportError(code) {
                              if (errorReported) return;
                              errorReported = true;
                              if (window.YmnTubeBridge && window.YmnTubeBridge.onPlayerError) {
                                window.YmnTubeBridge.onPlayerError(code);
                              }
                            }
                            function onYouTubeIframeAPIReady() {
                              try {
                                new YT.Player('ytplayer', {
                                  host: 'https://www.youtube-nocookie.com',
                                  width: '100%',
                                  height: '100%',
                                  videoId: '$safeId',
                                  playerVars: {
                                    autoplay: 1,
                                    controls: 1,
                                    playsinline: 1,
                                    rel: 0,
                                    iv_load_policy: 3,
                                    origin: 'https://www.youtube-nocookie.com'
                                  },
                                  events: {
                                    onReady: function(e) {
                                      try { e.target.playVideo(); } catch (_) {}
                                    },
                                    onError: function(e) {
                                      reportError(Number(e && e.data ? e.data : 150));
                                    }
                                  }
                                });
                              } catch (err) {
                                reportError(-1);
                              }
                            }
                            var tag = document.createElement('script');
                            tag.src = 'https://www.youtube.com/iframe_api';
                            tag.async = true;
                            tag.onerror = function() { reportError(-1); };
                            document.head.appendChild(tag);
                          </script>
                        </body>
                        </html>
                    """.trimIndent()
                    view.loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com/",
                        html,
                        "text/html",
                        "utf-8",
                        "https://www.youtube-nocookie.com/"
                    )
                }
            }
        )
    }
}
