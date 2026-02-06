package com.api.pethotelgo.service.impl

import com.api.pethotelgo.exception.OwnerNotFoundException
import com.api.pethotelgo.exception.ValidationException
import com.api.pethotelgo.model.entity.Owner
import com.api.pethotelgo.repository.OwnerRepository
import com.api.pethotelgo.service.OwnerService
import org.springframework.stereotype.Service
 

@Service
class OwnerServiceImpl(private val ownerRepository: OwnerRepository) : OwnerService {

    override fun getAllOwners(): List<Owner> = ownerRepository.findAll()

    override fun getOwnerById(id: String): Owner = ownerRepository.findById(id)
        .orElseThrow { OwnerNotFoundException() }

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
            throw ValidationException("Owner name is required")
        }

        // Business Rule 2: Phone must be provided and non-empty
        if (owner.phone.isBlank()) {
            throw ValidationException("Owner phone is required")
        }

        // Business Rule 3: Phone must be at least 8 digits
        if (owner.phone.replace(Regex("[^0-9]"), "").length < 8) {
            throw ValidationException("Phone must have at least 8 digits")
        }

        // Business Rule 4: Name length validation
        if (owner.name.length > 100) {
            throw ValidationException("Owner name cannot exceed 100 characters")
    }
}
}
