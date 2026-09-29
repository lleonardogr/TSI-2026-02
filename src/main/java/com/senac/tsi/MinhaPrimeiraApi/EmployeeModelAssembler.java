package com.senac.tsi.MinhaPrimeiraApi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import com.senac.tsi.MinhaPrimeiraApi.Employee;
import com.senac.tsi.MinhaPrimeiraApi.EmployeeController;

@Component
class EmployeeModelAssembler implements RepresentationModelAssembler<Employee, EntityModel<Employee>> {

    @Override
    public EntityModel<Employee> toModel(Employee employee) {
        return EntityModel.of(employee,
                linkTo(methodOn(EmployeeController.class).getEmployeeById(employee.getId())).withSelfRel(),
                // Use Pageable.unpaged() em vez de null
                linkTo(methodOn(EmployeeController.class).getAllEmployees(Pageable.unpaged())).withRel("employees")
        );
    }
}