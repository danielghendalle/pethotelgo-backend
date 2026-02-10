package com.api.pethotelgo.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "owners")
class Owner(
    @Id
    var id: String = UUID.randomUUID().toString(),

    var name: String = "",

    var phone: String = "",

    var createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var pets: MutableList<Pet> = mutableListOf(),

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var reservations: MutableList<Reservation> = mutableListOf()
)