-dontwarn org.jaudiotagger.logging.**

# 保留所有 SPI 服务提供者的实现类
-keep class javax.sound.sampled.spi.AudioFileReader { *; }
-keep class * extends javax.sound.sampled.spi.AudioFileReader

# 保留第三方音频库中的具体解码器实现
-keep class org.tritonus.sampled.file.** { *; }
-keep class com.googlecode.mp3spi.** { *; }
-keep class org.jflac.** { *; }
-keep class com.github.trilarion.vorbis.** { *; }
-keep class com.jcraft.** { *; }
-keep class org.jaudiotagger.** { *; }
-keep class javazoom.** { *; }

# 保留 META-INF/services 目录及内容（防止被删除）
-keepdirectories META-INF/services
-keep class META-INF.services.** { *; }

# 不优化 javax.sound 包下的任何类
-keep class javax.sound.sampled.** { *; }

-keep class org.jflac.** { *; }
-keep interface org.jflac.** { *; }
-keep enum org.jflac.** { *; }

-keep interface org.jaudiotagger.** { *; }
-keep enum org.jaudiotagger.** { *; }


