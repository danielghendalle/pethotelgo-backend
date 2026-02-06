package com.api.pethotelgo.service

import com.api.pethotelgo.model.entity.Owner

interface OwnerService {
    fun getAllOwners(): List<Owner>
    fun getOwnerById(id: String): Owner
    fun createOwner(owner: Owner): Owner
    fun updateOwner(id: String, data: Owner): Owner
    fun deleteOwner(id: String)
    fun validateOwnerData(owner: Owner)
}

