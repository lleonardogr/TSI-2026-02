package com.senac.tsi.MinhaPrimeiraApi.v2;

import com.senac.tsi.MinhaPrimeiraApi.Employee;

public record EmployeeResponse(long id, Name name, String role) {

    static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                new Name(employee.getFirstName(), employee.getLastName()),
                employee.getRole());
    }
}
