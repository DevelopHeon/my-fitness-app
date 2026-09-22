package com.myfitness.ai.application.context;

import java.util.List;

record AiContextSection(
        String text,
        List<AiContextType> types
) {
    static AiContextSection of(
            String text,
            AiContextType... types) {
        return new AiContextSection(
                text,
                List.of(types));
    }
}
