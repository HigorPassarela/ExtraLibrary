package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.SoldRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.SoldResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.SoldMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.services.SoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sold")
@Tag(name = "Sold Manager", description = "Manager for register Solds")
public class SoldController {

    private final SoldService service;
    private final SoldMapper mapper;

    public SoldController(SoldService service, SoldMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create/Register sold", description = "Endpoint for register customer solds")
    public ResponseEntity<Object> save(@RequestBody @Valid SoldRequest soldRequest) {
        try {
            Sold sold = SoldMapper.toEntity(soldRequest);
            Sold savedSold = service.save(sold);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(savedSold.getId())
                    .toUri();

            return ResponseEntity.created(location).build();
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sold by Id", description = "Endpoint for get sold details by Id")
    public ResponseEntity<SoldResponse> getDetailsFromId(@Parameter(description = "Sold id", required = true) @PathVariable String id) {
        try {
            Long soldId = Long.parseLong(id);

            return service
                    .getForId(soldId)
                    .map(sold -> {
                        SoldResponse soldResponse = mapper.toDTO(sold);
                        return ResponseEntity.ok(soldResponse);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all solds", description = "Endpoint for get all solds")
    public ResponseEntity<List<SoldResponse>> getAll() {
        List<Sold> solds = service.getAll();
        List<SoldResponse> soldResponses = solds.stream()
                .map(mapper::toDTO)
                .toList();

        return ResponseEntity.ok(soldResponses);
    }
}
