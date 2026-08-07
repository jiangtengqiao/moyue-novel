package com.novel.reader.data.repository

import com.novel.reader.data.api.MoYueApi
import com.novel.reader.data.api.NovelCreateBody
import com.novel.reader.data.api.ReadingProgressBody
import com.novel.reader.data.model.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 小说数据仓库
 */
@Singleton
class NovelRepository @Inject constructor(
    private val api: MoYueApi,
    private val sessionManager: SessionManager,
) {
    private suspend fun token(): String {
        val t = sessionManager.tokenFlow.first() ?: ""
        return "Bearer $t"
    }

    // ==================== 认证 ====================

    suspend fun login(username: String, password: String): TokenResponse {
        val resp = api.login(LoginRequest(username, password))
        sessionManager.saveToken(resp.accessToken)
        sessionManager.saveUser(resp.user)
        return resp
    }

    suspend fun register(username: String, password: String, nickname: String?): TokenResponse {
        val resp = api.register(RegisterRequest(username, password, nickname))
        sessionManager.saveToken(resp.accessToken)
        sessionManager.saveUser(resp.user)
        return resp
    }

    suspend fun logout() = sessionManager.clear()

    // ==================== 小说 ====================

    suspend fun getCategories() = api.getCategories()

    suspend fun getNovels(page: Int, pageSize: Int, category: String? = null, sort: String = "latest") =
        api.getNovels(page, pageSize, category, sort)

    suspend fun searchNovels(keyword: String, page: Int = 1) = api.searchNovels(keyword, page)

    suspend fun getFeatured() = api.getFeatured()

    suspend fun getNovel(id: String) = api.getNovel(id)

    suspend fun createNovel(title: String, author: String, description: String, tags: List<String>): Novel {
        return api.createNovel(token(), NovelCreateBody(title, author, description = description, tags = tags))
    }

    suspend fun toggleBookmark(novelId: String) = api.toggleBookmark(token(), novelId)

    suspend fun getBookmarks() = api.getBookmarks(token())

    // ==================== 章节 ====================

    suspend fun getChapters(novelId: String) = api.getChapters(novelId)

    suspend fun getChapterContent(novelId: String, chapterIndex: Int) = api.getChapterByIndex(novelId, chapterIndex)

    // ==================== 创作者 ====================

    suspend fun registerCreator(request: CreatorRegisterRequest): Creator {
        return api.registerCreator(token(), request)
    }

    suspend fun getCreatorProfile() = api.getCreatorProfile(token())

    suspend fun getCreatorNovels(page: Int = 1) = api.getCreatorNovels(token(), page)

    suspend fun getDashboard() = api.getDashboard(token())

    // ==================== 上传 ====================

    suspend fun uploadSingleFile(novelId: String, file: File): UploadSingleResponse {
        val reqBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, reqBody)
        return api.uploadSingleFile(token(), novelId, part)
    }

    suspend fun uploadFolder(novelId: String, files: List<File>): UploadTask {
        val parts = files.map { file ->
            val reqBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, reqBody)
        }
        return api.uploadFolder(token(), novelId, parts)
    }

    suspend fun getUploadTask(taskId: String) = api.getUploadTask(token(), taskId)

    // ==================== 公告与更新 ====================

    suspend fun getAnnouncements() = api.getAnnouncements()

    suspend fun getLatestAnnouncements() = api.getLatestAnnouncements()

    suspend fun checkUpdate(currentVersion: String) = api.checkUpdate(currentVersion)

    // ==================== 阅读历史 ====================

    suspend fun saveReadingProgress(novelId: String, chapterIndex: Int, chapterTitle: String) =
        api.saveReadingProgress(token(), novelId, ReadingProgressBody(chapterIndex, chapterTitle))

    suspend fun getReadingHistory() = api.getReadingHistory(token())
}
