package com.api.pethotelgo.model.entity

import com.api.pethotelgo.model.entity.Owner
import com.api.pethotelgo.model.enums.PetSize
import com.api.pethotelgo.model.enums.SociabilityLevel
import com.api.pethotelgo.model.entity.StayHistory
import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "pets")
class Pet(
    @Id
    var id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Owner? = null,

    var name: String = "",

    var breed: String = "",

    @Enumerated(EnumType.STRING)
    var size: PetSize = PetSize.medio,

    var needsSeparateSpace: Boolean = false,

    @Enumerated(EnumType.STRING)
    var sociability: SociabilityLevel = SociabilityLevel.media,

    @Column(columnDefinition = "text")
    var allergies: String = "",

    @Column(columnDefinition = "text")
    var specialCare: String = "",

    var feedingSchedule: String = "",

    var feedingAmount: String = "",

    @Column(columnDefinition = "text")
    var vaccinationCardUrl: String? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "pet", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var stayHistories: MutableList<StayHistory> = mutableListOf<StayHistory>()
)