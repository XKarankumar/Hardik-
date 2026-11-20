package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.domain.model.UpdateCheckState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class AppUpdateRepository(private val context: Context) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)

    suspend fun checkForUpdates(owner: String, repo: String): UpdateCheckState =
        withContext(Dispatchers.IO) {
            val cleanOwner = owner.trim()
            val cleanRepo = repo.trim()
            if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
                return@withContext UpdateCheckState.Error("Repository owner and name are required.")
            }
            try {
                val release = api.getLatestRelease(cleanOwner, cleanRepo)
                val current = BuildConfig.VERSION_NAME
                val latest = release.tagName.removePrefix("v").removePrefix("V").trim()
                if (isRemoteNewer(current = current, remote = latest)) {
                    val apkAsset = release.assets.firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true)
                    }
                    UpdateCheckState.UpdateAvailable(
                        currentVersion = current,
                        latestVersion = latest,
                        releaseName = release.name?.takeIf { it.isNotBlank() } ?: "Release $latest",
                        changelog = release.body?.takeIf { it.isNotBlank() }
                            ?: "No release notes provided.",
                        apkDownloadUrl = apkAsset?.browserDownloadUrl,
                        apkFileName = apkAsset?.name,
                        apkSizeBytes = apkAsset?.size,
                        htmlUrl = release.htmlUrl,
                    )
                } else {
                    UpdateCheckState.UpToDate(currentVersion = current)
                }
            } catch (http: HttpException) {
                val msg = when (http.code()) {
                    404 -> "No published releases found at $cleanOwner/$cleanRepo."
                    403 -> "GitHub API rate limit reached. Try again later."
                    else -> "GitHub API error (${http.code()})."
                }
                UpdateCheckState.Error(msg)
            } catch (e: Exception) {
                UpdateCheckState.Error(e.localizedMessage ?: "Network error while checking updates.")
            }
        }

    fun downloadReleaseApk(
        downloadUrl: String,
        latestVersion: String,
        fileName: String?,
    ): Flow<UpdateCheckState> = flow {
        emit(UpdateCheckState.Downloading(latestVersion, 0))
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val safeName = (fileName ?: "hardik-buds-$latestVersion.apk")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val targetFile = File(updatesDir, safeName)

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "HardikBuds-Android")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(UpdateCheckState.Error("Download failed (HTTP ${response.code})."))
                return@flow
            }
            val body = response.body ?: run {
                emit(UpdateCheckState.Error("Empty APK download response."))
                return@flow
            }
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastEmittedPct = -1

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0L) {
                            val pct = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastEmittedPct) {
                                lastEmittedPct = pct
                                emit(UpdateCheckState.Downloading(latestVersion, pct))
                            }
                        }
                    }
                }
            }
        }

        val needsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            !context.packageManager.canRequestPackageInstalls()
        } else {
            false
        }
        emit(
            UpdateCheckState.ReadyToInstall(
                latestVersion = latestVersion,
                apkFile = targetFile,
                requiresPermissionGrant = needsPermission,
            )
        )
    }.flowOn(Dispatchers.IO)

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildManageUnknownSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildInstallApkIntent(apkFile: File): Intent {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    companion object {
        fun isRemoteNewer(current: String, remote: String): Boolean {
            val cParts = current.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
            val rParts = remote.removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(cParts.size, rParts.size)
            for (i in 0 until maxLen) {
                val c = cParts.getOrElse(i) { 0 }
                val r = rParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
