package com.senac.tsi.MinhaPrimeiraApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class EmployeeController {

    private final EmployeeRepository repository;

    public EmployeeController(EmployeeRepository repository){
        this.repository = repository;
    }

    @GetMapping("/employee")
    public CollectionModel<EntityModel<Employee>> GetAll(){
        var employees = repository.findAll()
                .stream().map(employee -> EntityModel.of(employee,
                        linkTo(methodOn(EmployeeController.class)
                                .getEmployeeById(employee.getId()))
                                .withSelfRel(),
                        linkTo(methodOn(EmployeeController.class)
                                .GetAll()).withRel("employees")))
                .toList();

        return CollectionModel.of(employees, linkTo(methodOn(EmployeeController.class)
                .GetAll()).withSelfRel());
    }

    @PostMapping("/employee")
    public Employee createEmployee(@RequestBody Employee newEmployee){
        return repository.save(newEmployee);
    }

    @GetMapping("/employee/{id}")
    public EntityModel<Employee> getEmployeeById (
            @PathVariable long id){
        var employee =  repository
                .findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(id));

        return EntityModel.of(employee,
                linkTo(methodOn(EmployeeController.class)
                        .getEmployeeById(id)).withSelfRel(),
                linkTo(methodOn(EmployeeController.class)
                        .GetAll()).withRel("employees")
        );
    }

    @PutMapping("/employee/{id}")
    public Employee updateOrCreateEmployee(@RequestBody Employee newEmployee, @PathVariable long id) {
        return repository.findById(id)
                .map(employee -> {
                    employee.setName(newEmployee.getName());
                    employee.setRole(newEmployee.getRole());
                    return repository.save(employee);
                }).orElseGet(() ->
                        repository.save(newEmployee));
    }

    @DeleteMapping("/employee/{id}")
    public ResponseEntity deleteEmployeeById(@PathVariable long id){
        return repository.findById(id).map(
                employee -> {
                    repository.deleteById(id);
                    return ResponseEntity.status(204).build();
                })
                .orElseGet(() -> ResponseEntity.status(404).build());

    }
}
