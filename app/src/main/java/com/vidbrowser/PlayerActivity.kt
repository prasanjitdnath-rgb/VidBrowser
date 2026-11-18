package com.vidbrowser

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private val speeds = floatArrayOf(0.75f, 1f, 1.25f, 1.5f, 2f)
    private var si = 1

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val url = intent.getStringExtra("url") ?: return finish()
        val ref = intent.getStringExtra("referer")
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val pv = PlayerView(this)
        root.addView(pv, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0x88000000.toInt()); gravity = Gravity.END }
        fun btn(t: String, f: (Button) -> Unit) = Button(this).apply { text = t; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { f(this) } }
        bar.addView(btn("1.0x") { it.text = "${speeds[++si % speeds.size].also { s -> player?.playbackParameters = PlaybackParameters(s) }}x" })
        bar.addView(btn("⬇ Download") { Dl.start(this, url, ref) })
        bar.addView(btn("✕") { finish() })
        root.addView(bar, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
        setContentView(root)

        val ds = DefaultHttpDataSource.Factory().setUserAgent(Dl.UA).setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(buildMap { ref?.let { put("Referer", it) }; android.webkit.CookieManager.getInstance().getCookie(url)?.let { put("Cookie", it) } })
        player = ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(ds)).build().also {
            pv.player = it; it.setMediaItem(MediaItem.fromUri(url)); it.prepare(); it.playWhenReady = true
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
    override fun onPause() { super.onPause(); player?.pause() }
    override fun onDestroy() { player?.release(); super.onDestroy() }
}
