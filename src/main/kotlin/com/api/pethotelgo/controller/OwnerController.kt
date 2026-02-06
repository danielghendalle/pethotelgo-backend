package com.api.pethotelgo.controller

import com.api.pethotelgo.controller.api.OwnerApi
import com.api.pethotelgo.model.entity.Owner
import com.api.pethotelgo.model.entity.Pet
import com.api.pethotelgo.service.OwnerService
import org.springframework.web.bind.annotation.*

@RestController
class OwnerController(private val ownerService: OwnerService) : OwnerApi {

    override fun getAll(): List<Owner> = ownerService.getAllOwners()

    override fun getById(id: String): Owner = ownerService.getOwnerById(id)

    override fun getPetsByOwner(id: String): List<Pet> = ownerService.getOwnerById(id).pets

    override fun create(owner: Owner): Owner = ownerService.createOwner(owner)

    override fun update(id: String, data: Owner): Owner =
        ownerService.updateOwner(id, data)

    override fun delete(id: String) = ownerService.deleteOwner(id)
}
