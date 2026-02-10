package com.api.pethotelgo.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "stay_histories")
class StayHistory(
    @Id
    var id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    var pet: Pet? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    var reservation: Reservation? = null,

    var checkIn: Instant = Instant.now(),

    var checkOut: Instant? = null,

    @Column(columnDefinition = "text")
    var behavior: String = "",

    @Column(columnDefinition = "text")
    var notes: String = ""
)