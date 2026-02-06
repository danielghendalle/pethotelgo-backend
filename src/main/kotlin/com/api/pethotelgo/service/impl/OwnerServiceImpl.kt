package com.api.pethotelgo.service.impl

import com.api.pethotelgo.model.entity.Owner
import com.api.pethotelgo.repository.OwnerRepository
import com.api.pethotelgo.service.OwnerService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class OwnerServiceImpl(private val ownerRepository: OwnerRepository) : OwnerService {

    override fun getAllOwners(): List<Owner> = ownerRepository.findAll()

    override fun getOwnerById(id: String): Owner = ownerRepository.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found") }

    override fun createOwner(owner: Owner): Owner {
        validateOwnerData(owner)
        return ownerRepository.save(owner)
    }

    override fun updateOwner(id: String, data: Owner): Owner {
        val existing = getOwnerById(id)
        validateOwnerData(data)
        existing.name = data.name
        existing.phone = data.phone
        return ownerRepository.save(existing)
    }

    override fun deleteOwner(id: String) {
        val owner = getOwnerById(id)
        ownerRepository.delete(owner)
    }

    override fun validateOwnerData(owner: Owner) {
        // Business Rule 1: Name cannot be empty or blank
        if (owner.name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner name is required")
        }

        // Business Rule 2: Phone must be provided and non-empty
        if (owner.phone.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner phone is required")
        }

        // Business Rule 3: Phone must be at least 8 digits
        if (owner.phone.replace(Regex("[^0-9]"), "").length < 8) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone must have at least 8 digits")
        }

        // Business Rule 4: Name length validation
        if (owner.name.length > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner name cannot exceed 100 characters")
        }
    }
}

