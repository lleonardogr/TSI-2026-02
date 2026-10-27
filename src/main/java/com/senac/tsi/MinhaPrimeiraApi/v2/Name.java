package com.senac.tsi.MinhaPrimeiraApi.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Breaking change of v2: the name is an object instead of the flat firstName / lastName / name fields of v1
public record Name(
        @NotBlank @Size(min = 3, max = 100) String first,
        @Size(min = 3, max = 100) String last) {
}
