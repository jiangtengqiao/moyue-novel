package com.novel.reader.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户认证相关模型
 */
@Serializable
data class User(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    @SerialName("is_creator") val isCreator: Boolean = false,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: User,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
)

/**
 * 小说模型
 */
@Serializable
data class Novel(
    val id: String,
    val title: String,
    val author: String,
    @SerialName("creator_id") val creatorId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val status: String = "ongoing",
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("chapter_count") val chapterCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("collect_count") val collectCount: Int = 0,
    val rating: Double = 0.0,
    val featured: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class NovelBrief(
    val id: String,
    val title: String,
    val author: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    val description: String? = null,
    val status: String = "ongoing",
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("chapter_count") val chapterCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    val rating: Double = 0.0,
)

@Serializable
data class NovelListResponse(
    val total: Int,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val pages: Int = 0,
    val items: List<NovelBrief>,
)

/**
 * 章节模型
 */
@Serializable
data class Chapter(
    val id: String,
    @SerialName("novel_id") val novelId: String,
    val title: String,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_free") val isFree: Boolean = true,
    val status: String = "published",
    val level: Int = 2,
    val volume: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ChapterContent(
    val id: String,
    @SerialName("novel_id") val novelId: String,
    val title: String,
    val content: String,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_free") val isFree: Boolean = true,
)

/**
 * 创作者模型
 */
@Serializable
data class Creator(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("pen_name") val penName: String,
    @SerialName("real_name") val realName: String? = null,
    val introduction: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    val verified: Boolean = false,
    @SerialName("total_words") val totalWords: Int = 0,
    @SerialName("total_novels") val totalNovels: Int = 0,
    @SerialName("total_readers") val totalReaders: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CreatorRegisterRequest(
    @SerialName("pen_name") val penName: String,
    @SerialName("real_name") val realName: String? = null,
    val introduction: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("social_accounts") val socialAccounts: Map<String, String>? = null,
)

/**
 * 分类
 */
@Serializable
data class Category(
    val id: String,
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

/**
 * 公告
 */
@Serializable
data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val type: String = "info",
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

/**
 * 应用更新
 */
@Serializable
data class AppVersion(
    val id: String,
    @SerialName("version_name") val versionName: String,
    @SerialName("version_code") val versionCode: Int,
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("file_size") val fileSize: Long = 0,
    val md5: String? = null,
    @SerialName("update_title") val updateTitle: String? = null,
    @SerialName("update_log") val updateLog: String = "",
    @SerialName("force_update") val forceUpdate: Boolean = false,
    @SerialName("min_supported_version") val minSupportedVersion: String = "1.0.0",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class UpdateCheckResponse(
    @SerialName("has_update") val hasUpdate: Boolean,
    @SerialName("latest_version") val latestVersion: AppVersion? = null,
    val message: String = "",
)

/**
 * 上传任务
 */
@Serializable
data class UploadTask(
    val id: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("novel_id") val novelId: String? = null,
    val status: String = "pending",
    @SerialName("total_files") val totalFiles: Int = 0,
    @SerialName("processed_files") val processedFiles: Int = 0,
    @SerialName("failed_files") val failedFiles: Int = 0,
    @SerialName("current_file") val currentFile: String? = null,
    val progress: Double = 0.0,
    val message: String = "",
    @SerialName("file_list") val fileList: List<UploadFileItem> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UploadFileItem(
    val filename: String = "",
    val status: String = "pending",
    val chapters: Int = 0,
    val words: Int = 0,
    val error: String? = null,
)

/**
 * 通用响应
 */
@Serializable
data class MessageResponse(
    val message: String,
    val success: Boolean = true,
)

@Serializable
data class DashboardData(
    @SerialName("total_novels") val totalNovels: Int,
    @SerialName("total_words") val totalWords: Int,
    @SerialName("total_views") val totalViews: Int,
    @SerialName("total_likes") val totalLikes: Int,
    @SerialName("total_collects") val totalCollects: Int,
    @SerialName("total_chapters") val totalChapters: Int,
    @SerialName("recent_novels") val recentNovels: List<NovelBrief> = emptyList(),
)

@Serializable
data class UploadSingleResponse(
    val success: Boolean = true,
    val filename: String = "",
    @SerialName("chapters_added") val chaptersAdded: Int = 0,
    @SerialName("words_added") val wordsAdded: Int = 0,
    val message: String = "",
)

@Serializable
data class NovelUpdateBody(
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val tags: List<String>? = null,
    val status: String? = null,
)

@Serializable
data class ReadingHistoryItem(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("novel_id") val novelId: String,
    @SerialName("novel_title") val novelTitle: String = "",
    @SerialName("novel_author") val novelAuthor: String = "",
    @SerialName("chapter_index") val chapterIndex: Int = 0,
    @SerialName("chapter_title") val chapterTitle: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
