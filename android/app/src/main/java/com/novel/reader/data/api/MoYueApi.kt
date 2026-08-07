package com.novel.reader.data.api

import com.novel.reader.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * 墨阅后端 API 接口
 */
interface MoYueApi {

    // ==================== 认证 ====================

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): User

    // ==================== 小说 ====================

    @GET("api/novels/categories")
    suspend fun getCategories(): List<Category>

    @GET("api/novels/")
    suspend fun getNovels(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("category") category: String? = null,
        @Query("sort") sort: String = "latest",
        @Query("keyword") keyword: String? = null,
    ): NovelListResponse

    @GET("api/novels/search")
    suspend fun searchNovels(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
    ): NovelListResponse

    @GET("api/novels/featured")
    suspend fun getFeatured(@Query("limit") limit: Int = 6): List<NovelBrief>

    @GET("api/novels/{id}")
    suspend fun getNovel(@Path("id") id: String): Novel

    @POST("api/novels/")
    suspend fun createNovel(
        @Header("Authorization") token: String,
        @Body body: NovelCreateBody,
    ): Novel

    @PUT("api/novels/{id}")
    suspend fun updateNovel(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body body: NovelUpdateBody,
    ): Novel

    @POST("api/novels/{id}/bookmark")
    suspend fun toggleBookmark(
        @Header("Authorization") token: String,
        @Path("id") id: String,
    ): MessageResponse

    @GET("api/novels/bookmarks/list")
    suspend fun getBookmarks(@Header("Authorization") token: String): List<NovelBrief>

    @POST("api/novels/{id}/history")
    suspend fun saveReadingProgress(
        @Header("Authorization") token: String,
        @Path("id") novelId: String,
        @Body body: ReadingProgressBody,
    ): MessageResponse

    @GET("api/novels/reading-history/list")
    suspend fun getReadingHistory(@Header("Authorization") token: String): List<ReadingHistoryItem>

    // ==================== 章节 ====================

    @GET("api/novels/{novelId}/chapters/")
    suspend fun getChapters(@Path("novelId") novelId: String): List<Chapter>

    @GET("api/novels/{novelId}/chapters/index/{index}")
    suspend fun getChapterByIndex(
        @Path("novelId") novelId: String,
        @Path("index") index: Int,
    ): ChapterContent

    @GET("api/novels/{novelId}/chapters/{chapterId}")
    suspend fun getChapter(
        @Path("novelId") novelId: String,
        @Path("chapterId") chapterId: String,
    ): ChapterContent

    @POST("api/novels/{novelId}/chapters/")
    suspend fun createChapter(
        @Header("Authorization") token: String,
        @Path("novelId") novelId: String,
        @Body body: ChapterCreateBody,
    ): ChapterContent

    @PUT("api/novels/{novelId}/chapters/{chapterId}")
    suspend fun updateChapter(
        @Header("Authorization") token: String,
        @Path("novelId") novelId: String,
        @Path("chapterId") chapterId: String,
        @Body body: ChapterCreateBody,
    ): ChapterContent

    @DELETE("api/novels/{novelId}/chapters/{chapterId}")
    suspend fun deleteChapter(
        @Header("Authorization") token: String,
        @Path("novelId") novelId: String,
        @Path("chapterId") chapterId: String,
    ): MessageResponse

    // ==================== 创作者 ====================

    @POST("api/creator/register")
    suspend fun registerCreator(
        @Header("Authorization") token: String,
        @Body body: CreatorRegisterRequest,
    ): Creator

    @GET("api/creator/profile")
    suspend fun getCreatorProfile(@Header("Authorization") token: String): Creator

    @GET("api/creator/novels")
    suspend fun getCreatorNovels(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
    ): NovelListResponse

    @GET("api/creator/dashboard")
    suspend fun getDashboard(@Header("Authorization") token: String): DashboardData

    // ==================== 上传 ====================

    @Multipart
    @POST("api/upload/single/{novelId}")
    suspend fun uploadSingleFile(
        @Header("Authorization") token: String,
        @Path("novelId") novelId: String,
        @Part file: MultipartBody.Part,
    ): UploadSingleResponse

    @Multipart
    @POST("api/upload/folder/{novelId}")
    suspend fun uploadFolder(
        @Header("Authorization") token: String,
        @Path("novelId") novelId: String,
        @Part files: List<MultipartBody.Part>,
    ): UploadTask

    @GET("api/upload/task/{taskId}")
    suspend fun getUploadTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: String,
    ): UploadTask

    // ==================== 公告 ====================

    @GET("api/announcements/")
    suspend fun getAnnouncements(@Query("limit") limit: Int = 20): List<Announcement>

    @GET("api/announcements/latest")
    suspend fun getLatestAnnouncements(): List<Announcement>

    // ==================== 更新 ====================

    @GET("api/update/check")
    suspend fun checkUpdate(@Query("current_version") currentVersion: String): UpdateCheckResponse

    @Streaming
    @GET("api/update/download")
    suspend fun downloadApk(): okhttp3.ResponseBody
}

@kotlinx.serialization.Serializable
data class NovelCreateBody(
    val title: String,
    val author: String,
    @kotlinx.serialization.SerialName("category_id") val categoryId: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "ongoing",
)

@kotlinx.serialization.Serializable
data class ReadingProgressBody(
    @kotlinx.serialization.SerialName("chapter_index") val chapterIndex: Int,
    @kotlinx.serialization.SerialName("chapter_title") val chapterTitle: String = "",
)

@kotlinx.serialization.Serializable
data class ChapterCreateBody(
    val title: String,
    val content: String = "",
    val level: Int = 2,
    val volume: String = "",
)
