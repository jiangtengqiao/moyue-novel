package com.novel.reader.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.novel.reader.Constants
import com.novel.reader.data.api.MoYueApi
import com.novel.reader.data.model.UpdateCheckResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用更新管理器
 * 负责检查更新、下载APK、触发安装
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MoYueApi,
) {
    private val apkDir by lazy {
        File(context.cacheDir, Constants.APK_DIR).apply { mkdirs() }
    }

    /** 检查更新 */
    suspend fun checkUpdate(): UpdateCheckResponse = withContext(Dispatchers.IO) {
        api.checkUpdate(Constants.APP_VERSION)
    }

    /** 下载APK到本地缓存 */
    suspend fun downloadApk(onProgress: (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val response = api.downloadApk()
        val apkFile = File(apkDir, "moyue_update.apk")
        apkFile.outputStream().use { output ->
            val body = response.byteStream()
            val total = response.contentLength()
            var downloaded = 0L
            val buffer = ByteArray(8192)
            var lastReport = 0L

            body.use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastReport > 200) {
                            onProgress(downloaded.toFloat() / total)
                            lastReport = now
                        }
                    }
                }
            }
            onProgress(1f)
        }
        apkFile
    }

    /** 触发APK安装 */
    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** 清理旧APK */
    fun cleanOldApk() {
        apkDir.listFiles()?.forEach { it.delete() }
    }
}
