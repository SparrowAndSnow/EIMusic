package com.eimsound.eimusic

/** 设置进程环境变量；libcurl/Ktor 在创建客户端前读取代理配置。 */
expect fun setEnv(name: String, value: String)
