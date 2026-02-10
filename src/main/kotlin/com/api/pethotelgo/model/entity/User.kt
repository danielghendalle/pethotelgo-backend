package com.api.pethotelgo.model.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import com.api.pethotelgo.model.enums.UserRole

@Entity
@Table(name = "users")
class User(
    @Id
    var id: String = UUID.randomUUID().toString(),

    @Column(unique = true)
    var email: String = "",

    var name: String = "",

    @Column(name = "password_hash")
    var passwordHash: String = "", // will be hashed

    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    var isActive: Boolean = true,

    var createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var refreshTokens: MutableList<RefreshToken> = mutableListOf()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = isActive

    override fun isAccountNonLocked(): Boolean = isActive

    override fun isCredentialsNonExpired(): Boolean = isActive

    override fun isEnabled(): Boolean = isActive
}

