package com.api.pethotelgo.service.impl

import com.api.pethotelgo.exception.OwnerNotFoundException
import com.api.pethotelgo.exception.PetNotFoundException
import com.api.pethotelgo.exception.ValidationException
import com.api.pethotelgo.model.entity.Pet
import com.api.pethotelgo.model.enums.SociabilityLevel
import com.api.pethotelgo.repository.PetRepository
import com.api.pethotelgo.repository.OwnerRepository
import com.api.pethotelgo.service.PetService
import org.springframework.stereotype.Service
 

@Service
class PetServiceImpl(
    private val petRepository: PetRepository,
    private val ownerRepository: OwnerRepository
) : PetService {

    override fun getAllPets(): List<Pet> = petRepository.findAll()

    override fun getPetById(id: String): Pet = petRepository.findById(id)
        .orElseThrow { PetNotFoundException() }

    override fun getPetsByOwnerId(ownerId: String): List<Pet> {
        // Business Rule: Verify owner exists
        ownerRepository.findById(ownerId)
            .orElseThrow { OwnerNotFoundException() }
        return petRepository.findByOwnerId(ownerId)
    }

    override fun createPet(pet: Pet): Pet {
        validatePetData(pet)

        // Business Rule: Verify owner exists before associating
        pet.owner?.let { ownerRef ->
            ownerRepository.findById(ownerRef.id)
                .orElseThrow { OwnerNotFoundException() }
        }

        return petRepository.save(pet)
    }

    override fun updatePet(id: String, data: Pet): Pet {
        val existing = getPetById(id)
        validatePetData(data)

        // Business Rule: Verify owner exists if changing owner
        data.owner?.let { ownerRef ->
            ownerRepository.findById(ownerRef.id)
                .orElseThrow { OwnerNotFoundException() }
        }

        existing.name = data.name
        existing.breed = data.breed
        existing.size = data.size
        existing.needsSeparateSpace = data.needsSeparateSpace
        existing.sociability = data.sociability
        existing.allergies = data.allergies
        existing.specialCare = data.specialCare
        existing.feedingSchedule = data.feedingSchedule
        existing.feedingAmount = data.feedingAmount
        existing.vaccinationCardUrl = data.vaccinationCardUrl
        data.owner?.let { existing.owner = it }

        return petRepository.save(existing)
    }

    override fun deletePet(id: String) {
        val pet = getPetById(id)
        petRepository.delete(pet)
    }

    override fun validatePetData(pet: Pet) {
        // Business Rule 1: Pet name is required
        if (pet.name.isBlank()) {
            throw ValidationException("Pet name is required")
        }

        // Business Rule 2: Pet name length validation
        if (pet.name.length > 50) {
            throw ValidationException("Pet name cannot exceed 50 characters")
        }

        // Business Rule 3: Breed is required
        if (pet.breed.isBlank()) {
            throw ValidationException("Pet breed is required")
        }

        // Business Rule 4: Breed length validation
        if (pet.breed.length > 50) {
            throw ValidationException("Pet breed cannot exceed 50 characters")
        }

        // Business Rule 5: Feeding schedule is required
        if (pet.feedingSchedule.isBlank()) {
            throw ValidationException("Pet feeding schedule is required")
        }

        // Business Rule 6: Feeding amount is required
        if (pet.feedingAmount.isBlank()) {
            throw ValidationException("Pet feeding amount is required")
        }

        // Business Rule 7: Low sociability pets MUST have separate space
        if (pet.sociability == SociabilityLevel.baixa && !pet.needsSeparateSpace) {
            throw ValidationException("Low sociability pets must have separate space")
        }

        // Business Rule 8: High sociability pets should NOT require separate space (warning but allowed)
        // This could be logged but we allow it for exceptional cases

        // Business Rule 9: Vaccination card URL format validation if provided
        if (pet.vaccinationCardUrl != null && pet.vaccinationCardUrl!!.isNotBlank()) {
            if (!isValidUrl(pet.vaccinationCardUrl!!)) {
                throw ValidationException("Invalid vaccination card URL format")
            }
        }

        // Business Rule 10: Owner must be associated
        if (pet.owner == null || pet.owner!!.id.isBlank()) {
            throw ValidationException("Pet must be associated with an owner")
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URI(url).toURL()
            true
        } catch (_: Exception) {
            false
        }
    }
}

