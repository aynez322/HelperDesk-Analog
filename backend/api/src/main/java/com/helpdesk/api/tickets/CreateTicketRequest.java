package com.helpdesk.api.tickets;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 150) String subject,
        @Size(max = 5000) String description,
        Long categoryId,
        @Email @Size(max = 255) String requesterEmail
) {
}