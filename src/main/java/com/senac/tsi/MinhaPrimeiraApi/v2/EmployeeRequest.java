package com.senac.tsi.MinhaPrimeiraApi.v2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// v2 receives a DTO instead of the JPA entity, so the database model can change without breaking the API again
public record EmployeeRequest(
        @NotNull @Valid Name name,
        @Size(min = 3, max = 100) String role) {
}
