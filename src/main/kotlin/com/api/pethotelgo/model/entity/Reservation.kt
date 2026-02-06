package com.api.pethotelgo.model.entity

import com.api.pethotelgo.model.entity.Owner
import com.api.pethotelgo.model.enums.ReservationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reservations")
class Reservation(
    @Id
    var id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    var pet: Pet? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Owner? = null,

    var checkIn: Instant = Instant.now(),

    var checkOut: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    var status: ReservationStatus = ReservationStatus.pending,

    @Column(columnDefinition = "text")
    var notes: String = "",

    var createdAt: Instant = Instant.now()
)