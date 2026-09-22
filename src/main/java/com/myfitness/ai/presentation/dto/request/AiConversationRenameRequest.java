package com.myfitness.ai.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiConversationRenameRequest(
        @NotBlank
        @Size(max = 100)
        String title
) {}
