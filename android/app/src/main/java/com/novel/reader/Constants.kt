package com.novel.reader

/**
 * 全局常量
 */
object Constants {
    /** 后端服务地址 - 部署时替换为实际服务器地址 */
    const val BASE_URL = "http://10.0.2.2:8000/"

    /** 当前应用版本名 */
    const val APP_VERSION = "1.1.0"

    /** 当前应用版本号 */
    const val APP_VERSION_CODE = 2

    /** 分页大小 */
    const val PAGE_SIZE = 20

    /** DataStore 名称 */
    const val DATASTORE_NAME = "moyue_prefs"

    /** 本地缓存目录 */
    const val APK_DIR = "apk_updates"
}
