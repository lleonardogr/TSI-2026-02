package com.senac.tsi.MinhaPrimeiraApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class EmployeeController {

    private final EmployeeRepository repository;

    public EmployeeController(EmployeeRepository repository){
        this.repository = repository;
    }

    @GetMapping("/employee")
    public List<Employee> GetAll(){
        return repository.findAll();
    }

    @PostMapping("/employee")
    public Employee createEmployee(@RequestBody Employee newEmployee){
        return repository.save(newEmployee);
    }

    @GetMapping("/employee/{id}")
    public Employee getEmployeeById (
            @PathVariable long id){
        return repository
                .findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(id));
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
