package app.ranking.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val versionCode: Int) : UpdateState
    data class Downloading(val versionCode: Int) : UpdateState
    data class Ready(val versionCode: Int) : UpdateState
    data class Failed(val versionCode: Int) : UpdateState
}

/**
 * Atualização pelo próprio app: consulta a release mais recente no GitHub,
 * baixa o APK e abre o instalador do Android (que sempre pede a confirmação do usuário).
 */
object Updater {
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/RubemCezar/app-ranking/releases/latest"

    data class Release(val versionCode: Int, val apkUrl: String)

    fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info)
    }

    /** A tag da release é "v<número do build>", o mesmo número usado como versionCode. */
    fun fetchLatest(): Release? {
        val conn = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val code = json.getString("tag_name").removePrefix("v").toIntOrNull() ?: return null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    return Release(code, asset.getString("browser_download_url"))
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    fun download(context: Context, url: String): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "app-ranking.apk")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            conn.inputStream.use { input -> file.outputStream().use { out -> input.copyTo(out) } }
        } finally {
            conn.disconnect()
        }
        return file
    }

    /** Abre o instalador. Se o app ainda não tem permissão, abre a tela para liberar. */
    fun install(context: Context, file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
