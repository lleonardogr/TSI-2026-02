package com.senac.tsi.MinhaPrimeiraApi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Tag(name="tag_at_class_level", description = "Employees Management")
public class EmployeeController {

    private final EmployeeRepository repository;

    private final EmployeeModelAssembler assembler;

    EmployeeController(EmployeeRepository repository, EmployeeModelAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    @Operation(summary = "Get all employees")
    @ApiResponse(responseCode = "200", description = "Returned a list with all employees visible")
    @GetMapping("/employees")
    CollectionModel<EntityModel<Employee>> getAllEmployees() {

        List<EntityModel<Employee>> employees = repository.findAll().stream() //
                .map(assembler::toModel) //
                .collect(Collectors.toList());

        return CollectionModel.of(employees, linkTo(methodOn(EmployeeController.class).getAllEmployees()).withSelfRel());
    }


    @Tag(name = "tag_at_method_level")
    @Tag(name = "create" , description = "create a new employee")
    @Operation(summary = "Creates a new employee")
    @ApiResponse(responseCode = "201", description = "Returns a new employee created")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PostMapping("/employees")
    public ResponseEntity<?> newEmployee(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New employee",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema= @Schema(implementation = Employee.class),
                            examples = @ExampleObject(value = "{ \"firstName\": \"Bilbo\", \"lastName\": \"Baggins\", \"role\": \"Burglar\" }")
                    )

            )
            @RequestBody @Valid Employee newEmployee){

        EntityModel<Employee> entityModel =
                assembler.toModel(repository.save(newEmployee));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(newEmployee);
    }

    @Operation(summary = "Get a employee by your id")
    @ApiResponse(responseCode = "200", description = "Returns a valid employee"
    , content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = Employee.class))})
    @ApiResponse(responseCode = "404", description = "Not find it a employee with the id", content = @Content)
    @ApiResponse(responseCode = "400", description = "Tried to search a employee with invalid id", content = @Content)
    @GetMapping("/employees/{id}")
    EntityModel<Employee> getEmployeeById(@PathVariable Long id) {

        Employee employee = repository.findById(id) //
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        return assembler.toModel(employee);
    }

    @PutMapping("/employees/{id}")
    public Employee updateOrCreateEmployee(@RequestBody Employee newEmployee, @PathVariable Long id){
        return repository.findById(id)
                .map(employee -> {
                    employee.setName(newEmployee.getName());
                    employee.setRole(newEmployee.getRole());
                    return repository.save(employee);
                })
                .orElseGet(() -> {
                    return repository.save(newEmployee);
                });
    }

    @Operation(summary = "Deletes a employee")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a employee", content = {@Content})
    @ApiResponse(responseCode = "404", description = "Not found a employee, maybe it's already deleted", content = {@Content})
    @DeleteMapping("/employee/{id}")
    public ResponseEntity<?> deleteEmployee(@Parameter(description = "Id of employee") @PathVariable Long id)
    {
        var employee = repository.findById(id);
        if(employee.isEmpty())
            return ResponseEntity.notFound().build();

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
