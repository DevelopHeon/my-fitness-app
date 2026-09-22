package com.myfitness.ai.application.context;

import java.util.List;

public record AiContextBundle(
        String text,
        List<AiContextType> types
) {
    public static AiContextBundle empty() {
        return new AiContextBundle("", List.of());
    }

    public String typeNames() {
        return types.stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }
}
