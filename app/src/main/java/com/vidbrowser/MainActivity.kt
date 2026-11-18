package com.vidbrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var url: EditText
    private lateinit var prog: ProgressBar
    private lateinit var vidBtn: Button
    private val found = LinkedHashMap<String, String>() // url -> label
    private val exts = Regex("""\.(mp4|m3u8|webm|mkv|mov|3gp)(\?|$)""", RegexOption.IGNORE_CASE)
    private val adHosts = listOf("doubleclick.net", "googlesyndication.com", "adservice.google", "exoclick", "juicyads", "popads", "propellerads", "adsterra", "trafficjunky")
    private val home = "https://www.google.com"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        url = EditText(this).apply {
            hint = "Search or enter URL"; setSingleLine(); imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, _, _ -> go(text.toString()); true }
        }
        prog = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        web = WebView(this)
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFFEEEEEE.toInt()) }
        fun b(t: String, f: () -> Unit) = Button(this).apply { text = t; setBackgroundColor(Color.TRANSPARENT); setOnClickListener { f() } }
            .also { bottom.addView(it, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)) }
        b("◀") { if (web.canGoBack()) web.goBack() }
        b("▶") { if (web.canGoForward()) web.goForward() }
        b("⌂") { web.loadUrl(home) }
        vidBtn = b("🎬 0") { showVideos() }
        b("⬇") { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }

        root.addView(url, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(prog, LinearLayout.LayoutParams(MATCH_PARENT, 8))
        root.addView(web, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(bottom, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)

        web.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = true
            userAgentString = Dl.UA; setSupportMultipleWindows(false); builtInZoomControls = true; displayZoomControls = false
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, p: Int) { prog.progress = p; prog.visibility = if (p == 100) android.view.View.GONE else android.view.View.VISIBLE }
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView, u: String, f: Bitmap?) { url.setText(u); found.clear(); updateBadge() }
            override fun onPageFinished(v: WebView, u: String) { scanDom() }
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
                val u = r.url.toString()
                if (adHosts.any { r.url.host?.contains(it) == true }) return WebResourceResponse("text/plain", "utf-8", null)
                if (exts.containsMatchIn(u)) runOnUiThread { addFound(u) }
                return null
            }
        }
        // Rescan DOM periodically for dynamically injected <video> tags
        web.postDelayed(object : Runnable { override fun run() { scanDom(); web.postDelayed(this, 3000) } }, 3000)
        web.setDownloadListener { u, _, _, _, _ -> Dl.start(this, u, web.url) }
        web.loadUrl(intent?.dataString ?: home)
    }

    private fun go(q: String) {
        val t = q.trim()
        web.loadUrl(if (t.startsWith("http")) t else if (t.contains('.') && !t.contains(' ')) "https://$t" else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(t, "UTF-8"))
    }

    private fun scanDom() {
        web.evaluateJavascript("(function(){var o=[];document.querySelectorAll('video,video source,audio').forEach(function(v){var s=v.currentSrc||v.src;if(s&&s.indexOf('blob:')!==0)o.push(s)});return o.join('\\n')})()") { r ->
            r?.trim('"')?.replace("\\u003C", "<")?.split("\\n")?.filter { it.startsWith("http") }?.forEach { addFound(it) }
        }
    }

    private fun addFound(u: String) { if (found.putIfAbsent(u, Uri2.label(u)) == null) updateBadge() }
    private fun updateBadge() { vidBtn.text = "🎬 ${found.size}" }

    private fun showVideos() {
        if (found.isEmpty()) { Toast.makeText(this, "No videos detected yet — play a video on the page", Toast.LENGTH_LONG).show(); return }
        val keys = found.keys.toList()
        AlertDialog.Builder(this).setTitle("Detected videos")
            .setItems(keys.map { found[it] }.toTypedArray()) { _, i ->
                AlertDialog.Builder(this).setItems(arrayOf("▶ Play in built-in player", "⬇ Download")) { _, w ->
                    if (w == 0) startActivity(Intent(this, PlayerActivity::class.java).putExtra("url", keys[i]).putExtra("referer", web.url))
                    else Dl.start(this, keys[i], web.url)
                }.show()
            }.show()
    }

    override fun onKeyDown(k: Int, e: KeyEvent): Boolean {
        if (k == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true }
        return super.onKeyDown(k, e)
    }
}

object Uri2 { fun label(u: String): String { val p = android.net.Uri.parse(u); return (p.lastPathSegment ?: "video") + "  (" + (p.host ?: "") + ")" } }
