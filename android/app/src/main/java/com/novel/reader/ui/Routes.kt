package com.novel.reader.ui

/**
 * 路由定义
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // 主页面 (底部导航)
    const val HOME = "home"
    const val LIBRARY = "library"
    const val CREATOR = "creator"
    const val PROFILE = "profile"

    // 详情页
    const val NOVEL_DETAIL = "novel/{novelId}"
    const val READER = "reader/{novelId}/{chapterIndex}"
    const val CHAPTER_LIST = "chapters/{novelId}"
    const val SEARCH = "search"
    const val ANNOUNCEMENTS = "announcements"
    const val ANNOUNCEMENT_DETAIL = "announcement/{id}"

    // 创作者页面
    const val CREATOR_REGISTER = "creator_register"
    const val CREATOR_DASHBOARD = "creator_dashboard"
    const val CREATE_NOVEL = "create_novel"
    const val UPLOAD_NOVEL = "upload_novel/{novelId}"
    const val UPLOAD_PROGRESS = "upload_progress/{taskId}"
    const val MY_NOVELS = "my_novels"
    const val READING_HISTORY = "reading_history"
    const val SETTINGS = "settings"
    const val CHAPTER_EDIT = "chapter_edit/{novelId}?chapterId={chapterId}"
    const val AGREEMENT = "agreement/{type}"

    fun novelDetail(id: String) = "novel/$id"
    fun reader(novelId: String, index: Int) = "reader/$novelId/$index"
    fun chapterList(novelId: String) = "chapters/$novelId"
    fun announcementDetail(id: String) = "announcement/$id"
    fun uploadNovel(novelId: String) = "upload_novel/$novelId"
    fun uploadProgress(taskId: String) = "upload_progress/$taskId"
    fun chapterEdit(novelId: String, chapterId: String? = null) =
        if (chapterId == null) "chapter_edit/$novelId" else "chapter_edit/$novelId?chapterId=$chapterId"

    fun agreement(type: String) = "agreement/$type"
}
