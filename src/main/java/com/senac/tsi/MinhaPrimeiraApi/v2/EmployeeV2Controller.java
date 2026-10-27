package com.senac.tsi.MinhaPrimeiraApi.v2;

import com.senac.tsi.MinhaPrimeiraApi.Employee;
import com.senac.tsi.MinhaPrimeiraApi.EmployeeNotFoundException;
import com.senac.tsi.MinhaPrimeiraApi.EmployeeRepository;
import com.senac.tsi.MinhaPrimeiraApi.IdempotencyFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Version 2 of the employees API. Same URLs as v1, chosen with "API-Version: 2" (or ?api-version=2).
 *
 * What changed compared to v1 (all breaking changes, that's why it is a new version):
 *   - name is an object { "first": ..., "last": ... } instead of firstName / lastName / name
 *   - requests and responses are DTOs (records), not the JPA entity
 *   - plain JSON instead of HATEOAS (_links / _embedded)
 *   - PUT only updates; it returns 404 when the employee doesn't exist instead of creating one
 *
 * DELETE didn't change, so it is not here: the v1 mapping has no version and serves every version.
 */
@RestController
@RequestMapping("/employees")
@Tag(name = "employees-v2", description = "Employees Management - version 2 (send API-Version: 2)")
public class EmployeeV2Controller {

    private final EmployeeRepository repository;

    public EmployeeV2Controller(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Get all employees (v2)")
    @ApiResponse(responseCode = "200", description = "Returned a page of employees")
    @GetMapping(version = "2")
    public PagedModel<EmployeeResponse> getAllEmployees(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return new PagedModel<>(repository.findAll(pageable).map(EmployeeResponse::from));
    }

    @Operation(summary = "Get a employee by id (v2)")
    @ApiResponse(responseCode = "200", description = "Returns the employee")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @GetMapping(path = "/{id}", version = "2")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return repository.findById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Operation(summary = "Creates a new employee (v2)")
    @ApiResponse(responseCode = "201", description = "Returns the new employee")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload")
    @ApiResponse(responseCode = "409", description = "A request with the same Idempotency-Key is still being processed", content = @Content)
    @ApiResponse(responseCode = "422", description = "Idempotency-Key was already used with a different payload", content = @Content)
    @Parameter(in = ParameterIn.HEADER, name = IdempotencyFilter.HEADER, required = false,
            description = "Unique key (e.g. a UUID) that makes retries safe: repeating it returns the original response instead of creating a new employee",
            schema = @Schema(type = "string"))
    @PostMapping(version = "2")
    public ResponseEntity<EmployeeResponse> newEmployee(@RequestBody @Valid EmployeeRequest request) {
        Employee saved = repository.save(new Employee(request.name().first(), request.name().last(), request.role()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(EmployeeResponse.from(saved));
    }

    @Operation(summary = "Updates an existing employee (v2)")
    @ApiResponse(responseCode = "200", description = "Returns the updated employee")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @PutMapping(path = "/{id}", version = "2")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @RequestBody @Valid EmployeeRequest request) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        employee.setFirstName(request.name().first());
        employee.setLastName(request.name().last());
        employee.setRole(request.role());
        return EmployeeResponse.from(repository.save(employee));
    }
}
