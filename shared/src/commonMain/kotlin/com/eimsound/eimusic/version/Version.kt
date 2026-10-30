package com.eimsound.eimusic.version

/**
 * 解析版本号字符串为可比较的整数列表。
 *
 * 非数字段会被忽略（例如 "1.0.0-beta" -> [1, 0, 0]），
 * 这与现有客户端/服务端的比较语义保持一致。
 */
fun parseVersion(version: String): List<Int> =
    version
        .removePrefix("v")
        .removePrefix("V")
        .split(".")
        .mapNotNull { it.toIntOrNull() }

/**
 * 比较两个版本号。
 *
 * @return 负数表示 [a] 小于 [b]，0 表示相等，正数表示 [a] 大于 [b]。
 */
fun compareVersions(a: String, b: String): Int {
    val left = parseVersion(a)
    val right = parseVersion(b)
    val size = maxOf(left.size, right.size)
    for (i in 0 until size) {
        val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
        if (diff != 0) return diff
    }
    return 0
}
