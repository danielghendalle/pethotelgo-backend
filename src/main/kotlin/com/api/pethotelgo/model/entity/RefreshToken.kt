package com.api.pethotelgo.model.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens", indexes = [Index(name = "idx_token_hash", columnList = "token_hash")])
class RefreshToken(
    @Id
    var id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(unique = true)
    var token: String? = null,

    @Column(name = "token_hash", unique = true)
    var tokenHash: String? = null,

    var expiresAt: Instant = Instant.now(),

    var createdAt: Instant = Instant.now(),

    var revokedAt: Instant? = null
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    fun isRevoked(): Boolean = revokedAt != null

    fun isValid(): Boolean = !isExpired() && !isRevoked()
}
