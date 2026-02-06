package com.api.pethotelgo.controller

import com.api.pethotelgo.model.entity.Pet
import com.api.pethotelgo.service.PetService
import com.api.pethotelgo.service.ReservationService
import com.api.pethotelgo.service.StayHistoryService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/pets")
@Tag(name = "Pets", description = "Pet management endpoints")
@SecurityRequirement(name = "bearer-jwt")
class PetController(
    private val petService: PetService,
    private val reservationService: ReservationService,
    private val stayHistoryService: StayHistoryService
) {

    @GetMapping
    @Operation(summary = "List all pets", description = "Get all pets in the system")
    fun getAll(): List<Pet> = petService.getAllPets()

    @GetMapping("/{id}")
    @Operation(summary = "Get pet by ID", description = "Get specific pet information")
    fun getById(@PathVariable id: String): Pet = petService.getPetById(id)

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "List pets by owner", description = "Get all pets of a specific owner")
    fun getByOwnerPath(@PathVariable ownerId: String): List<Pet> = petService.getPetsByOwnerId(ownerId)

    @GetMapping("/{petId}/reservations")
    @Operation(summary = "Get pet's reservations", description = "Get all reservations for a pet")
    fun getReservationsByPet(@PathVariable petId: String) = reservationService.getReservationsByPetId(petId)

    @GetMapping("/{petId}/stay-history")
    @Operation(summary = "Get pet's stay history", description = "Get all stay history records for a pet")
    fun getStayHistoryByPet(@PathVariable petId: String) = stayHistoryService.getStayHistoriesByPetId(petId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create pet", description = "Register a new pet")
    fun create(@RequestBody pet: Pet): Pet = petService.createPet(pet)

    @PutMapping("/{id}")
    @Operation(summary = "Update pet", description = "Update pet information")
    fun update(@PathVariable id: String, @RequestBody data: Pet): Pet =
        petService.updatePet(id, data)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete pet", description = "Delete a pet")
    fun delete(@PathVariable id: String) = petService.deletePet(id)
}
