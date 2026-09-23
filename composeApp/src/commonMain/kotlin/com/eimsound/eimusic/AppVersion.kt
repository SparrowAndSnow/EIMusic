package com.eimsound.eimusic

/**
 * 版本信息工具类
 * 提供应用版本信息的读取和比较功能
 * 版本号通过 Gradle BuildConfig 自动注入
 */
object AppVersion {

    /**
     * 当前应用版本
     * 从 BuildConfig 自动获取，格式："1.0.0-SNAPSHOT"
     */
    const val VERSION_NAME: String = BuildConfig.VERSION_NAME

    /**
     * 版本代码 (用于比较)
     * 从 BuildConfig 自动获取
     */
    const val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    const val APP_NAME: String = BuildConfig.APP_NAME

    val combineName get() = "${BuildConfig.APP_NAME}-${BuildConfig.VERSION_NAME}-${BuildConfig.OS}"

    /**
     * 解析版本号字符串为可比较的列表
     * @param version 版本号字符串 (例如："1.0.1")
     * @return 整数列表 (例如：[1, 0, 1])
     */
    fun parseVersion(version: String): List<Int> {
        return version
            .removePrefix("v")
            .removePrefix("V")
            .split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    /**
     * 比较两个版本号
     * @param version1 版本号 1
     * @param version2 版本号 2
     * @return 负数表示 version1 < version2, 0 表示相等，正数表示 version1 > version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = parseVersion(version1)
        val v2Parts = parseVersion(version2)

        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrElse(i) { 0 }
            val part2 = v2Parts.getOrElse(i) { 0 }

            if (part1 != part2) {
                return part1 - part2
            }
        }

        return 0
    }

    /**
     * 检查是否有新版本可用
     * @param currentVersion 当前版本
     * @param latestVersion 最新版本
     * @return true 如果有新版本
     */
    fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(currentVersion, latestVersion) < 0
    }

    /**
     * 格式化版本显示文本
     * @param version 版本号
     * @return 格式化后的文本 (例如："v1.0.1")
     */
    fun formatVersion(version: String): String {
        return if (version.startsWith("v") || version.startsWith("V")) {
            version
        } else {
            "v$version"
        }
    }
}