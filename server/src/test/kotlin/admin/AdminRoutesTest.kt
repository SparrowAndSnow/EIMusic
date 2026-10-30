package com.eimsound.eimusic.server.admin

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.network.RequestUrlProvider
import com.eimsound.eimusic.server.db.AuditRepository
import com.eimsound.eimusic.server.db.ServerDatabase
import com.eimsound.eimusic.server.db.SourceRepository
import com.eimsound.eimusic.server.db.UserRepository
import com.eimsound.eimusic.server.manager.ChannelManager
import com.eimsound.eimusic.server.manager.UserSessionManager
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminRoutesTest {
    @BeforeTest
    fun configureAdmin() {
        System.setProperty("EIMUSIC_ADMIN_SESSION_SECRET", "test-session-secret")
    }

    @AfterTest
    fun clearAdmin() {
        System.clearProperty("EIMUSIC_ADMIN_SESSION_SECRET")
    }

    @Test
    fun loginAndManageSources() {
        val file = Files.createTempFile("eimusic-admin-test", ".db")
        try {
            val database = ServerDatabase.connect(file)
            val sourceRepository = SourceRepository(database).also { it.ensureDefaults() }
            val userRepository = UserRepository(database)
            val auditRepository = AuditRepository(database)
            val requestUrlProvider = RequestUrlProvider(listOf("https://netease-source.com"))
            val adminAccounts = AdminAccountService(userRepository)

            testApplication {
                application {
                    configureAdminSessions()
                    routing {
                        adminRoutes(
                            sourceRepository = sourceRepository,
                            userRepository = userRepository,
                            auditRepository = auditRepository,
                            requestUrlProvider = requestUrlProvider,
                            adminAccounts = adminAccounts,
                            sessionManager = UserSessionManager(),
                            channelManager = ChannelManager(),
                        )
                    }
                }

                val client = createClient {
                    install(HttpCookies)
                    followRedirects = false
                }

                val redirect = client.get("/admin")
                assertEquals(HttpStatusCode.Found, redirect.status)
                assertEquals("/admin/login", redirect.headers[HttpHeaders.Location])

                val setupPage = client.get("/admin/login")
                assertEquals(HttpStatusCode.OK, setupPage.status)
                assertTrue(setupPage.bodyAsText().contains("初始化管理员"))

                val setup = client.post("/admin/setup") {
                    setBody("username=admin&password=test-password&confirm=test-password")
                    contentType(io.ktor.http.ContentType.Application.FormUrlEncoded)
                }
                assertEquals(HttpStatusCode.Found, setup.status)
                assertEquals("/admin", setup.headers[HttpHeaders.Location])

                val dashboard = client.get("/admin")
                assertEquals(HttpStatusCode.OK, dashboard.status)
                assertTrue(dashboard.bodyAsText().contains("仪表盘"))
                assertTrue(
                    auditRepository.recent().any {
                        it.action == "admin.setup"
                    }
                )

                client.get("/admin/logout")
                val login = client.post("/admin/login") {
                    setBody("username=admin&password=test-password")
                    contentType(io.ktor.http.ContentType.Application.FormUrlEncoded)
                }
                assertEquals(HttpStatusCode.Found, login.status)
                assertEquals("/admin", login.headers[HttpHeaders.Location])
                assertTrue(
                    auditRepository.recent().any {
                        it.action == "login" && it.detail == "success"
                    }
                )

                val sourcesPage = client.get("/admin/sources").bodyAsText()
                val csrf = Regex("""name="csrf" value="([^"]+)"""")
                    .find(sourcesPage)?.groupValues?.get(1)
                    ?: error("CSRF token missing")

                val badCsrf = client.post("/admin/sources") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("csrf", "wrong")
                                append("source", TrackSource.NETEASE.name)
                                append("urls", "https://evil.example")
                            }
                        )
                    )
                }
                assertEquals(HttpStatusCode.Forbidden, badCsrf.status)

                val save = client.post("/admin/sources") {
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("csrf", csrf)
                                append("source", TrackSource.NETEASE.name)
                                append("urls", "https://new-source.example")
                            }
                        )
                    )
                }
                assertEquals(HttpStatusCode.Found, save.status)
                assertEquals(
                    listOf("https://new-source.example"),
                    sourceRepository.load()[TrackSource.NETEASE],
                )
                assertEquals("new-source.example", requestUrlProvider.url.value.host)
                assertTrue(
                    auditRepository.recent().any {
                        it.action == "source.update" && it.detail.contains("new-source.example")
                    }
                )
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
