package com.eimsound.eimusic.version

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 版本更新信息
 */
@Serializable
data class UpdateInfo(
    /**
     * 版本号 (例如：1.0.1)
     */
    val version: String,
    
    /**
     * 版本代码 (用于比较的整数)
     */
    @SerialName("version_code")
    val versionCode: Int,
    
    /**
     * 更新标题
     */
    val title: String,
    
    /**
     * 更新描述
     */
    val description: String,
    
    /**
     * 发布日期
     */
    @SerialName("release_date")
    val releaseDate: String,
    
    /**
     * 下载 URL
     */
    @SerialName("download_url")
    val downloadUrl: String? = null,
    
    /**
     * 是否强制更新
     */
    @SerialName("is_critical")
    val isCritical: Boolean = false,
    
    /**
     * 更新日志
     */
    @SerialName("release_notes")
    val releaseNotes: String? = null
)

/**
 * 更新检查响应
 */
@Serializable
data class UpdateResponse(
    /**
     * 是否有更新
     */
    @SerialName("has_update")
    val hasUpdate: Boolean,
    
    /**
     * 更新信息
     */
    val update: UpdateInfo? = null,
    
    /**
     * 当前最新版本
     */
    @SerialName("latest_version")
    val latestVersion: String? = null
)
