package com.eimsound.eimusic.util

/**
 * Desktop平台的代理工具类实现
 */
actual object ProxyUtils {
    /**
     * 配置系统代理设置
     * @param host 代理主机地址，如果为null则清除代理设置
     * @param port 代理端口
     */
    actual fun configureSystemProxy(enabled: Boolean, host: String?, port: Int) {
        if (enabled) {
            System.setProperty("http.proxyHost", host)
            System.setProperty("http.proxyPort", port.toString())
            System.setProperty("https.proxyHost", host)
            System.setProperty("https.proxyPort", port.toString())
        } else {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
        }
    }
}