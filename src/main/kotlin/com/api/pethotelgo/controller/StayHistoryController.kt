package com.api.pethotelgo.controller

import com.api.pethotelgo.model.entity.StayHistory
import com.api.pethotelgo.service.StayHistoryService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
@RequestMapping("/stay-history")
@Tag(name = "Stay History", description = "Pet stay history and behavior tracking endpoints")
@SecurityRequirement(name = "bearer-jwt")
class StayHistoryController(private val stayHistoryService: StayHistoryService) {

    @GetMapping
    @Operation(summary = "List all stay histories", description = "Get all pet stay history records")
    fun getAll(): List<StayHistory> = stayHistoryService.getAllStayHistories()

    @GetMapping("/pet/{petId}")
    @Operation(summary = "Get pet's stay history", description = "Get all stay history records for a specific pet")
    fun getByPetPath(@PathVariable petId: String): List<StayHistory> =
        stayHistoryService.getStayHistoriesByPetId(petId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create stay history", description = "Record pet behavior and stay information")
    fun create(@RequestBody stayHistory: StayHistory): StayHistory =
        stayHistoryService.createStayHistory(stayHistory)

    @PutMapping("/{id}")
    @Operation(summary = "Update stay history", description = "Update stay history information")
    fun update(@PathVariable id: String, @RequestBody data: StayHistory): StayHistory =
        stayHistoryService.updateStayHistory(id, data)
}
