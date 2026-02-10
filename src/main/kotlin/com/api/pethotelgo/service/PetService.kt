package com.api.pethotelgo.service

import com.api.pethotelgo.model.entity.Pet

interface PetService {
    fun getAllPets(): List<Pet>
    fun getPetById(id: String): Pet
    fun getPetsByOwnerId(ownerId: String): List<Pet>
    fun createPet(pet: Pet): Pet
    fun updatePet(id: String, data: Pet): Pet
    fun deletePet(id: String)
    fun validatePetData(pet: Pet)
}

