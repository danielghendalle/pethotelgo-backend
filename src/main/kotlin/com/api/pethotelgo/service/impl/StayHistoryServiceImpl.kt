package com.api.pethotelgo.service.impl

import com.api.pethotelgo.model.entity.StayHistory
import com.api.pethotelgo.repository.StayHistoryRepository
import com.api.pethotelgo.repository.PetRepository
import com.api.pethotelgo.service.StayHistoryService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class StayHistoryServiceImpl(
    private val stayHistoryRepository: StayHistoryRepository,
    private val petRepository: PetRepository
) : StayHistoryService {

    override fun getAllStayHistories(): List<StayHistory> = stayHistoryRepository.findAll()

    override fun getStayHistoriesByPetId(petId: String): List<StayHistory> {
        // Business Rule: Verify pet exists
        petRepository.findById(petId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found") }
        return stayHistoryRepository.findByPetId(petId)
    }

    override fun createStayHistory(stayHistory: StayHistory): StayHistory {
        validateStayHistoryData(stayHistory)

        // Business Rule: Verify pet exists
        stayHistory.pet?.let { petRef ->
            petRepository.findById(petRef.id)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found") }
        }

        return stayHistoryRepository.save(stayHistory)
    }

    override fun updateStayHistory(id: String, data: StayHistory): StayHistory {
        val existing = stayHistoryRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "StayHistory not found") }

        validateStayHistoryData(data)

        // Business Rule: Verify pet exists if changing
        data.pet?.let { petRef ->
            petRepository.findById(petRef.id)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found") }
        }

        existing.checkIn = data.checkIn
        existing.checkOut = data.checkOut
        existing.behavior = data.behavior
        existing.notes = data.notes
        data.pet?.let { existing.pet = it }
        data.reservation?.let { existing.reservation = it }

        return stayHistoryRepository.save(existing)
    }

    override fun validateStayHistoryData(stayHistory: StayHistory) {
        // Business Rule 1: Pet is required
        if (stayHistory.pet == null || stayHistory.pet!!.id.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet is required for stay history")
        }

        // Business Rule 2: If check-out is provided, it must be after check-in
        if (stayHistory.checkOut != null && !stayHistory.checkIn.isBefore(stayHistory.checkOut)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out must be after check-in")
        }

        // Business Rule 4: Behavior notes should be provided
        if (stayHistory.behavior.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Behavior description is required")
        }

        // Business Rule 5: Behavior length validation
        if (stayHistory.behavior.length > 500) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Behavior notes cannot exceed 500 characters")
        }

        // Business Rule 6: Notes length validation
        if (stayHistory.notes.length > 500) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Notes cannot exceed 500 characters")
        }

        // Business Rule 7: Stay duration should not exceed 30 days
        stayHistory.checkOut?.let { checkOut ->
            val daysDuration = java.time.temporal.ChronoUnit.DAYS.between(stayHistory.checkIn, checkOut)
            if (daysDuration > 30) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Stay duration cannot exceed 30 days")
            }
        }
    }
}



