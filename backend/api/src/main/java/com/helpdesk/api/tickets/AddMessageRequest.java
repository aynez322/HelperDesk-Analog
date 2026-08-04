package com.helpdesk.api.tickets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddMessageRequest(
        @NotBlank @Size(max = 5000) String body
) {
}