package com.eimsound.eimusic.server.admin

import com.eimsound.eimusic.server.db.ServerDatabase
import com.eimsound.eimusic.server.db.UserRepository
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminAccountServiceTest {
    @Test
    fun bootstrapsAndAuthenticatesAdmin() {
        val file = Files.createTempFile("eimusic-admin-auth-test", ".db")
        try {
            val database = ServerDatabase.connect(file)
            val repository = UserRepository(database)
            val service = AdminAccountService(repository)

            val initialized = service.initializeFirstAdmin("admin", "test-password")
            assertNotNull(initialized)
            assertTrue(service.hasAdmin())

            val admin = service.authenticate("admin", "test-password")
            assertNotNull(admin)
            assertEquals(AdminAccountService.ROLE_ADMIN, admin.role)
            assertNull(service.authenticate("admin", "wrong-password"))

            repository.create("listener", PasswordHasher.hash("listener-password"), AdminAccountService.ROLE_USER)
            assertNull(service.authenticate("listener", "listener-password"))

            repository.setDisabled(admin.id, true)
            assertNull(service.authenticate("admin", "test-password"))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun initializesOnlyOnce() {
        val file = Files.createTempFile("eimusic-admin-bootstrap-test", ".db")
        try {
            val database = ServerDatabase.connect(file)
            val repository = UserRepository(database)
            val service = AdminAccountService(repository)

            assertNotNull(service.initializeFirstAdmin("admin", "first-password"))
            assertNull(service.initializeFirstAdmin("admin2", "second-password"))

            assertNotNull(service.authenticate("admin", "first-password"))
            assertNull(service.authenticate("admin2", "second-password"))
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
