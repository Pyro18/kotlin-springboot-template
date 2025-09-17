package com.example.template.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email", unique = true),
        Index(name = "idx_user_username", columnList = "username", unique = true),
        Index(name = "idx_user_active", columnList = "active"),
        Index(name = "idx_user_created", columnList = "created_at")
    ]
)
@EntityListeners(AuditingEntityListener::class)
@SequenceGenerator(
    name = "user_seq",
    sequenceName = "user_sequence",
    allocationSize = 50
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    var firstName: String,

    @Column(nullable = false, length = 100)
    var lastName: String,

    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var active: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Column(length = 500)
    var bio: String? = null,

    @Column(name = "profile_picture_url", length = 500)
    var profilePictureUrl: String? = null,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,

    @Column(name = "locked_until")
    var lockedUntil: LocalDateTime? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_permissions",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "permission")
    var permissions: MutableSet<String> = mutableSetOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var sessions: MutableSet<UserSession> = mutableSetOf(),

    @Version
    var version: Long = 0,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    fun isAccountLocked(): Boolean =
        lockedUntil?.isAfter(LocalDateTime.now()) ?: false

    fun lock(duration: Long = 30) {
        lockedUntil = LocalDateTime.now().plusMinutes(duration)
    }

    fun unlock() {
        lockedUntil = null
        failedLoginAttempts = 0
    }

    fun recordFailedLogin() {
        failedLoginAttempts++
        if (failedLoginAttempts >= 5) {
            lock()
        }
    }

    fun recordSuccessfulLogin() {
        lastLoginAt = LocalDateTime.now()
        failedLoginAttempts = 0
        lockedUntil = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        id?.hashCode() ?: 0

    override fun toString(): String =
        "User(id=$id, username='$username', email='$email')"
}

enum class UserRole {
    ADMIN, MODERATOR, USER, GUEST
}

@Entity
@Table(
    name = "user_sessions",
    indexes = [
        Index(name = "idx_session_token", columnList = "token", unique = true),
        Index(name = "idx_session_expires", columnList = "expires_at")
    ]
)
data class UserSession(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val token: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", length = 500)
    val userAgent: String? = null,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
) {
    fun isExpired(): Boolean =
        expiresAt.isBefore(LocalDateTime.now())
}