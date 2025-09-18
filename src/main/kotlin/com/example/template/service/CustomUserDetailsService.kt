package com.example.template.service

import com.example.template.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service("userDetailsService")
@Transactional(readOnly = true)
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsernameOrEmail(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")

        return UserPrincipal(
            id = user.id!!,
            username = user.username,
            email = user.email,
            password = user.password,
            authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
            enabled = user.active,
            accountNonExpired = true,
            accountNonLocked = !user.isAccountLocked(),
            credentialsNonExpired = true
        )
    }
}

/**
 * Custom UserDetails implementation for JWT authentication
 */
data class UserPrincipal(
    val id: Long,
    private val username: String,
    val email: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val enabled: Boolean,
    private val accountNonExpired: Boolean,
    private val accountNonLocked: Boolean,
    private val credentialsNonExpired: Boolean
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities
    override fun getPassword(): String = password
    override fun getUsername(): String = username
    override fun isAccountNonExpired(): Boolean = accountNonExpired
    override fun isAccountNonLocked(): Boolean = accountNonLocked
    override fun isCredentialsNonExpired(): Boolean = credentialsNonExpired
    override fun isEnabled(): Boolean = enabled

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserPrincipal) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}