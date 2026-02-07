package com.pesexpo.productservice.domain.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateProduct(

        @Pattern(regexp = ".*\\S.*", message = "Product Name can't be blank")
        String productName,

        @Positive(message = "Product Price must be positive")
        BigDecimal price

) {
    @AssertTrue(message = "At least one field must be provided")
    public boolean isAtLeastOneFieldPresent() {
        return (productName != null && !productName.isBlank()) || price != null;
    }
}
