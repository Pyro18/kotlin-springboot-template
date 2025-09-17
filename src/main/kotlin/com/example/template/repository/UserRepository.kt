package com.example.template.repository

import com.example.template.entity.User
import com.example.template.entity.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Basic queries
    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun findByEmailIgnoreCase(email: String): User?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun existsByEmailIgnoreCase(email: String): Boolean

    // Paginated queries
    fun findByActive(active: Boolean, pageable: Pageable): Page<User>

    fun findByRole(role: UserRole, pageable: Pageable): Page<User>

    fun findByActiveAndRole(active: Boolean, role: UserRole, pageable: Pageable): Page<User>

    @Query("""
        SELECT u FROM User u 
        WHERE u.active = :active 
        AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun findByActiveAndSearchTerm(
        @Param("active") active: Boolean,
        @Param("search") search: String,
        pageable: Pageable
    ): Page<User>

    @Query("""
        SELECT u FROM User u 
        WHERE u.role = :role
        AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun findByRoleAndSearchTerm(
        @Param("role") role: UserRole,
        @Param("search") search: String,
        pageable: Pageable
    ): Page<User>

    @Query("""
        SELECT u FROM User u 
        WHERE u.active = :active 
        AND u.role = :role
        AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    fun findByActiveAndRoleAndSearchTerm(
        @Param("active") active: Boolean,
        @Param("role") role: UserRole,
        @Param("search") search: String,
        pageable: Pageable
    ): Page<User>

    @Query("""
        SELECT u FROM User u 
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    fun findBySearchTerm(@Param("search") search: String, pageable: Pageable): Page<User>

    // Advanced search
    @Query("""
        SELECT u FROM User u 
        WHERE (:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
        AND (:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
        AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
    """)
    fun searchUsers(
        @Param("firstName") firstName: String?,
        @Param("lastName") lastName: String?,
        @Param("email") email: String?,
        pageable: Pageable
    ): Page<User>

    // Statistics queries
    fun countByActive(active: Boolean): Long

    fun countByRole(role: UserRole): Long

    fun countByEmailVerified(verified: Boolean): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    fun countRecentRegistrations(@Param("date") date: LocalDateTime): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :date")
    fun countActiveUsersSince(@Param("date") date: LocalDateTime): Long

    // Batch operations
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.id IN :ids")
    fun deactivateUsersByIds(@Param("ids") ids: List<Long>): Int

    @Modifying
    @Query("UPDATE User u SET u.active = true WHERE u.id IN :ids")
    fun activateUsersByIds(@Param("ids") ids: List<Long>): Int

    @Modifying
    @Query("DELETE FROM User u WHERE u.id IN :ids")
    fun deleteAllByIdInBatch(@Param("ids") ids: List<Long?>)

    // Session and login related
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.sessions WHERE u.username = :username")
    fun findByUsernameWithSessions(@Param("username") username: String): User?

    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil > CURRENT_TIMESTAMP")
    fun findLockedUsers(): List<User>

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = NULL, u.failedLoginAttempts = 0 WHERE u.lockedUntil <= CURRENT_TIMESTAMP")
    fun unlockExpiredAccounts(): Int

    // Native queries for complex operations
    @Query(
        value = """
            SELECT u.role, COUNT(*) as count, 
                   AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - u.created_at))/86400) as avg_days_since_registration
            FROM users u
            GROUP BY u.role
        """,
        nativeQuery = true
    )
    fun getRoleStatistics(): List<Array<Any>>

    @Query(
        value = """
            SELECT DATE(created_at) as date, COUNT(*) as registrations
            FROM users
            WHERE created_at >= :startDate
            GROUP BY DATE(created_at)
            ORDER BY date DESC
        """,
        nativeQuery = true
    )
    fun getRegistrationTrend(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>

    // Custom projections
    @Query("""
        SELECT new map(
            u.id as id,
            u.username as username,
            u.email as email,
            u.firstName as firstName,
            u.lastName as lastName,
            u.role as role
        ) FROM User u WHERE u.active = true
    """)
    fun findActiveUsersProjection(): List<Map<String, Any>>

    // Full-text search (PostgreSQL specific)
    @Query(
        value = """
            SELECT * FROM users u
            WHERE to_tsvector('english', 
                concat_ws(' ', u.username, u.email, u.first_name, u.last_name, u.bio)
            ) @@ plainto_tsquery('english', :searchQuery)
        """,
        nativeQuery = true
    )
    fun fullTextSearch(@Param("searchQuery") searchQuery: String, pageable: Pageable): Page<User>
}