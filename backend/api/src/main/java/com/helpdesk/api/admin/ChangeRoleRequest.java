package com.helpdesk.api.admin;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
        @NotBlank String role
) {
}