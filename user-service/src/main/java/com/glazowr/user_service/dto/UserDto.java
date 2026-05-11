package com.glazowr.user_service.dto;

import com.glazowr.user_service.validation.OnCreate;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    /**
     * Create API:
     * client should NOT send ID
     *
     * Update API:
     * ignore body ID completely
     * use path variable as source of truth
     */
    @Null(
            groups = OnCreate.class,
            message = "ID must not be provided during creation"
    )
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name cannot exceed 50 characters")
    private String name;

    @Size(max = 50, message = "Surname cannot exceed 50 characters")
    private String surname;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    private boolean alerting;

    @PositiveOrZero(message = "Energy alerting threshold must be positive or zero")
    private double energyAlertingThreshold;

    /**
     * Business validation:
     * if alerting=true,
     * threshold must be > 0
     */
    @AssertTrue(message = "Threshold must be greater than 0 when alerting is enabled")
    public boolean isThresholdValid() {
        return !alerting || energyAlertingThreshold > 0;
    }
}