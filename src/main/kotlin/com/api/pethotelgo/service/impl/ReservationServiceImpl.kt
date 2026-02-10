package com.api.pethotelgo.service.impl

import com.api.pethotelgo.exception.*
import com.api.pethotelgo.model.entity.Reservation
import com.api.pethotelgo.model.dto.DayCapacity
import com.api.pethotelgo.model.enums.ReservationStatus
import com.api.pethotelgo.repository.ReservationRepository
import com.api.pethotelgo.repository.PetRepository
import com.api.pethotelgo.repository.OwnerRepository
import com.api.pethotelgo.service.ReservationService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class ReservationServiceImpl(
    private val reservationRepository: ReservationRepository,
    private val petRepository: PetRepository,
    private val ownerRepository: OwnerRepository
) : ReservationService {

    companion object {
        private const val MAX_PETS_PER_DAY = 10 // Business rule constant
        private const val MIN_RESERVATION_DAYS = 1
    }

    override fun getAllReservations(): List<Reservation> = reservationRepository.findAll()

    override fun getReservationById(id: String): Reservation = reservationRepository.findById(id)
        .orElseThrow { ReservationNotFoundException() }

    override fun getReservationsByDate(date: LocalDate): List<Reservation> {
        val zone = ZoneId.systemDefault()
        return reservationRepository.findAll().filter { r ->
            val start = r.checkIn.atZone(zone).toLocalDate()
            val end = r.checkOut.atZone(zone).toLocalDate()
            !date.isBefore(start) && !date.isAfter(end)
        }
    }

    override fun getReservationsByPetId(petId: String): List<Reservation> {
        // Business Rule: Verify pet exists
        petRepository.findById(petId)
            .orElseThrow { PetNotFoundException() }
        return reservationRepository.findByPetId(petId)
    }

    override fun createReservation(reservation: Reservation): Reservation {
        validateReservationData(reservation)

        // Business Rule: Verify pet exists
        reservation.pet?.let { petRef ->
            petRepository.findById(petRef.id)
                .orElseThrow { PetNotFoundException() }
        }

        // Business Rule: Verify owner exists
        reservation.owner?.let { ownerRef ->
            ownerRepository.findById(ownerRef.id)
                .orElseThrow { OwnerNotFoundException() }
        }

        // Business Rule: Check for overlapping reservations for the same pet
        val petId = reservation.pet?.id ?: throw ValidationException("Pet is required")
        val existingReservations = reservationRepository.findByPetId(petId)
            .filter { it.status in listOf(ReservationStatus.confirmed, ReservationStatus.pending) }

        if (hasConflict(reservation, existingReservations)) {
            throw ConflictException(
                message = "Pet already has a reservation during this period",
                code = ErrorCode.RESERVATION_CONFLICT
            )
        }

        // Business Rule: Check daily capacity
        val zone = ZoneId.systemDefault()
        var currentDate = reservation.checkIn.atZone(zone).toLocalDate()
        val endDate = reservation.checkOut.atZone(zone).toLocalDate()

        while (!currentDate.isAfter(endDate)) {
            val dayCapacity = checkDayCapacity(currentDate)
            if (dayCapacity.isFull) {
                throw ApiException(ErrorCode.CAPACITY_FULL, "Hotel is at full capacity on $currentDate")
            }
            currentDate = currentDate.plusDays(1)
        }

        return reservationRepository.save(reservation)
    }

    override fun updateReservation(id: String, data: Reservation): Reservation {
        val existing = getReservationById(id)
        validateReservationData(data)

        // Business Rule: Cannot update dates if reservation is completed or cancelled
        if (existing.status in listOf(ReservationStatus.completed, ReservationStatus.cancelled)) {
            throw BusinessRuleException("Cannot update a ${existing.status} reservation")
        }

        // Business Rule: Verify pet exists if changing
        data.pet?.let { petRef ->
            petRepository.findById(petRef.id)
                .orElseThrow { PetNotFoundException() }
        }

        // Business Rule: Verify owner exists if changing
        data.owner?.let { ownerRef ->
            ownerRepository.findById(ownerRef.id)
                .orElseThrow { OwnerNotFoundException() }
        }

        existing.checkIn = data.checkIn
        existing.checkOut = data.checkOut
        existing.notes = data.notes
        data.pet?.let { existing.pet = it }
        data.owner?.let { existing.owner = it }

        return reservationRepository.save(existing)
    }

    override fun deleteReservation(id: String) {
        val reservation = getReservationById(id)

        // Business Rule: Can only delete pending reservations
        if (reservation.status != ReservationStatus.pending) {
            throw BusinessRuleException("Can only delete pending reservations")
        }

        reservationRepository.delete(reservation)
    }

    override fun updateReservationStatus(id: String, newStatus: String): Reservation {
        val reservation = getReservationById(id)

        val status = try {
            ReservationStatus.valueOf(newStatus)
        } catch (_: Exception) {
            throw ValidationException("Invalid reservation status")
        }

        // Business Rule: Validate status transitions
        if (!isValidStatusTransition(reservation.status, status)) {
            throw BusinessRuleException("Cannot transition from ${reservation.status} to $status")
        }

        reservation.status = status
        return reservationRepository.save(reservation)
    }

    override fun checkDayCapacity(date: LocalDate): DayCapacity {
        val zone = ZoneId.systemDefault()
        val reservations = reservationRepository.findAll().filter { r ->
            if (r.status !in listOf(ReservationStatus.confirmed, ReservationStatus.pending)) {
                return@filter false
            }
            val start = r.checkIn.atZone(zone).toLocalDate()
            val end = r.checkOut.atZone(zone).toLocalDate()
            !date.isBefore(start) && !date.isAfter(end)
        }

        val totalPets = reservations.size
        val isFull = totalPets >= MAX_PETS_PER_DAY

        return DayCapacity(
            date = date,
            totalPets = totalPets,
            isFull = isFull,
            reservations = reservations
        )
    }

    override fun validateReservationData(reservation: Reservation) {
        // Business Rule 1: Pet is required
        if (reservation.pet == null || reservation.pet!!.id.isBlank()) {
            throw ValidationException("Pet is required")
        }

        // Business Rule 2: Owner is required
        if (reservation.owner == null || reservation.owner!!.id.isBlank()) {
            throw ValidationException("Owner is required")
        }

        // Business Rule 3: Check-in must be before check-out
        if (!reservation.checkIn.isBefore(reservation.checkOut)) {
            throw ValidationException("Check-in must be before check-out")
        }

        // Business Rule 4: Minimum reservation duration (at least 1 day)
        val daysDifference = java.time.temporal.ChronoUnit.DAYS.between(
            reservation.checkIn.atZone(ZoneId.systemDefault()).toLocalDate(),
            reservation.checkOut.atZone(ZoneId.systemDefault()).toLocalDate()
        )
        if (daysDifference < MIN_RESERVATION_DAYS) {
            throw ValidationException("Reservation must be at least $MIN_RESERVATION_DAYS day(s)")
        }

        // Business Rule 5: Cannot reserve in the past
        if (reservation.checkIn.isBefore(java.time.Instant.now())) {
            throw ValidationException("Cannot create reservation in the past")
        }

        // Business Rule 6: Maximum reservation length (e.g., 30 days)
        if (daysDifference > 30) {
            throw ValidationException("Reservation cannot exceed 30 days")
        }
}

    private fun hasConflict(newReservation: Reservation, existingReservations: List<Reservation>): Boolean {
        return existingReservations.any { existing ->
            // Check if dates overlap
            val startsBeforeEnd = newReservation.checkIn.isBefore(existing.checkOut)
            val endsAfterStart = newReservation.checkOut.isAfter(existing.checkIn)
            startsBeforeEnd && endsAfterStart
        }
    }

    private fun isValidStatusTransition(from: ReservationStatus, to: ReservationStatus): Boolean {
        return when {
            from == to -> true
            from == ReservationStatus.pending && to in listOf(ReservationStatus.confirmed, ReservationStatus.cancelled) -> true
            from == ReservationStatus.confirmed && to in listOf(ReservationStatus.completed, ReservationStatus.cancelled) -> true
            from == ReservationStatus.completed -> false // No transitions from completed
            from == ReservationStatus.cancelled -> false // No transitions from cancelled
            else -> false
        }
    }
}
