package com.eimsound.eimusic

/**
 * 当前运行平台名称，用于更新检查时选择正确的 GitHub Release 资产。
 *
 * 返回值与 [com.eimsound.eimusic.network.GitHubUpdateApi] 的 os 参数约定一致：
 * "linux" / "windows" / "osx"。
 */
expect fun currentOsName(): String
