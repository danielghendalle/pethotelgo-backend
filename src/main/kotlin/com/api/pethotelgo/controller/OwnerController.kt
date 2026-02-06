package com.api.pethotelgo.controller

import com.api.pethotelgo.model.entity.Owner
import com.api.pethotelgo.service.OwnerService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/owners")
@Tag(name = "Owners", description = "Owner management endpoints")
@SecurityRequirement(name = "bearer-jwt")
class OwnerController(private val ownerService: OwnerService) {

    @GetMapping
    @Operation(summary = "List all owners", description = "Get all pet owners")
    fun getAll(): List<Owner> = ownerService.getAllOwners()

    @GetMapping("/{id}")
    @Operation(summary = "Get owner by ID", description = "Get specific owner information")
    fun getById(@PathVariable id: String): Owner = ownerService.getOwnerById(id)

    @GetMapping("/{id}/pets")
    @Operation(summary = "Get owner's pets", description = "Get all pets belonging to an owner")
    fun getPetsByOwner(@PathVariable id: String) = ownerService.getOwnerById(id).pets

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create owner", description = "Create a new pet owner")
    fun create(@RequestBody owner: Owner): Owner = ownerService.createOwner(owner)

    @PutMapping("/{id}")
    @Operation(summary = "Update owner", description = "Update owner information")
    fun update(@PathVariable id: String, @RequestBody data: Owner): Owner =
        ownerService.updateOwner(id, data)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete owner", description = "Delete an owner and associated pets")
    fun delete(@PathVariable id: String) = ownerService.deleteOwner(id)
}
