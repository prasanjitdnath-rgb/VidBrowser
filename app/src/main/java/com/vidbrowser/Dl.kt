package com.vidbrowser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.widget.Toast

object Dl {
    const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    fun start(c: Context, url: String, referer: String?) {
        if (url.contains(".m3u8")) {
            Toast.makeText(c, "HLS (.m3u8) streams can be played but not saved as a single file", Toast.LENGTH_LONG).show(); return
        }
        val name = Uri.parse(url).lastPathSegment?.takeIf { it.contains('.') } ?: "video_${System.currentTimeMillis()}.mp4"
        val r = DownloadManager.Request(Uri.parse(url))
            .setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VidBrowser/$name")
            .addRequestHeader("User-Agent", UA)
        referer?.let { r.addRequestHeader("Referer", it) }
        CookieManager.getInstance().getCookie(url)?.let { r.addRequestHeader("Cookie", it) }
        (c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(r)
        Toast.makeText(c, "Download started: $name", Toast.LENGTH_SHORT).show()
    }
}
