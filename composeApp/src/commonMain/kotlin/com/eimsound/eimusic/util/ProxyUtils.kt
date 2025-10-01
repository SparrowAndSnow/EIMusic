package com.eimsound.eimusic.util

/**
 * 代理工具类，用于处理跨平台的代理配置
 */
expect object ProxyUtils {
    /**
     * 配置系统代理设置
     * @param host 代理主机地址，如果为null则清除代理设置
     * @param port 代理端口
     */
    fun configureSystemProxy(enabled: Boolean, host: String?, port: Int)

}