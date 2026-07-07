package com.example.data.repo

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.Roles
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Local authentication with salted SHA-256 password hashes and a persisted
 * session (survives process death). Mirrors the Flask roles model:
 * admin/manager are privileged; attendance users are scoped helpers.
 */
class AuthRepository(private val db: AppDatabase, context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    suspend fun restoreSession() {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        if (id > 0) {
            val user = db.userDao().findById(id)
            if (user != null && user.status == "active") _currentUser.value = user
        }
    }

    sealed interface LoginResult {
        data class Success(val user: User) : LoginResult
        data class Error(val message: String) : LoginResult
    }

    suspend fun login(username: String, password: String): LoginResult {
        val user = db.userDao().findByUsername(username.trim())
            ?: return LoginResult.Error("Invalid username or password")
        if (!verifyPassword(password, user.passwordHash)) {
            return LoginResult.Error("Invalid username or password")
        }
        if (user.status != "active") {
            return LoginResult.Error("This account has been disabled. Please contact the Administrator.")
        }
        prefs.edit().putLong(KEY_USER_ID, user.id).apply()
        _currentUser.value = user
        return LoginResult.Success(user)
    }

    fun logout() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _currentUser.value = null
    }

    suspend fun refreshCurrentUser() {
        val id = _currentUser.value?.id ?: return
        _currentUser.value = db.userDao().findById(id)
    }

    /** Create or update an attendance user (admin action). */
    suspend fun saveAttendanceUser(
        existingId: Long?,
        username: String,
        fullName: String,
        phone: String?,
        password: String?,
        assignedSiteIds: List<Long>,
        assignedProjectIds: List<Long>,
    ): String? {
        val trimmed = username.trim()
        if (trimmed.isBlank() || fullName.isBlank()) return "Name and username are required."
        val clash = db.userDao().findByUsername(trimmed)
        if (clash != null && clash.id != existingId) return "Username already exists."
        val existing = existingId?.let { db.userDao().findById(it) }
        if (existing == null && password.isNullOrBlank()) return "Password is required for a new user."

        val user = (existing ?: User(
            username = trimmed,
            passwordHash = hashPassword(password!!),
            fullName = fullName,
            role = Roles.ATTENDANCE,
        )).copy(
            username = trimmed,
            fullName = fullName,
            phone = phone,
            assignedSiteIds = assignedSiteIds.joinToString(","),
            assignedProjectIds = assignedProjectIds.joinToString(","),
            passwordHash = if (!password.isNullOrBlank()) hashPassword(password) else (existing?.passwordHash ?: return "Password is required."),
        )
        db.userDao().upsert(user)
        return null
    }

    suspend fun toggleUserStatus(userId: Long) {
        val user = db.userDao().findById(userId) ?: return
        db.userDao().upsert(user.copy(status = if (user.status == "active") "disabled" else "active"))
    }

    suspend fun deleteUser(userId: Long) {
        db.userDao().findById(userId)?.let { db.userDao().delete(it) }
    }

    companion object {
        private const val KEY_USER_ID = "user_id"

        fun hashPassword(password: String): String {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val digest = sha256(salt + password.toByteArray(Charsets.UTF_8))
            return "${salt.toHex()}:${digest.toHex()}"
        }

        fun verifyPassword(password: String, stored: String): Boolean {
            val parts = stored.split(':')
            if (parts.size != 2) return false
            val salt = parts[0].hexToBytes() ?: return false
            val digest = sha256(salt + password.toByteArray(Charsets.UTF_8))
            return MessageDigest.isEqual(digest, parts[1].hexToBytes())
        }

        private fun sha256(input: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(input)

        private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

        private fun String.hexToBytes(): ByteArray? {
            if (length % 2 != 0) return null
            return try {
                ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}
