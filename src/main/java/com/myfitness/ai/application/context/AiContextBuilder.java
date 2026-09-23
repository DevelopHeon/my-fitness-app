package com.myfitness.ai.application.context;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.application.context.AiContextSelector.AiContextArea;
import com.myfitness.ai.domain.model.AiQueryType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiContextBuilder {
    private static final String NL = System.lineSeparator();

    private final AiContextSelector contextSelector;
    private final AiWorkoutContextBuilder workoutContextBuilder;
    private final AiBodyContextBuilder bodyContextBuilder;
    private final AiNutritionContextBuilder nutritionContextBuilder;

    AiContextBuilder(
            AiContextSelector contextSelector,
            AiWorkoutContextBuilder workoutContextBuilder,
            AiBodyContextBuilder bodyContextBuilder,
            AiNutritionContextBuilder nutritionContextBuilder) {
        this.contextSelector = contextSelector;
        this.workoutContextBuilder = workoutContextBuilder;
        this.bodyContextBuilder = bodyContextBuilder;
        this.nutritionContextBuilder = nutritionContextBuilder;
    }

    public AiContextBundle build(
            Long userId,
            AiQueryType queryType,
            AiClientContext clientContext,
            String question) {
        Set<AiContextArea> areas = contextSelector.select(
                queryType,
                clientContext,
                question);
        if (areas.isEmpty()) {
            return AiContextBundle.empty();
        }

        List<AiContextSection> sections = buildSections(
                userId,
                clientContext,
                question,
                areas);

        return new AiContextBundle(
                joinText(sections),
                collectTypes(sections));
    }

    private static String joinText(
            List<AiContextSection> sections) {
        return sections.stream()
                .map(AiContextSection::text)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(NL + NL));
    }

    private static List<AiContextType> collectTypes(
            List<AiContextSection> sections) {
        return sections.stream()
                .flatMap(section -> section.types().stream())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private List<AiContextSection> buildSections(
            Long userId,
            AiClientContext clientContext,
            String question,
            Set<AiContextArea> areas) {
        List<AiContextSection> sections = new ArrayList<>();

        if (areas.contains(AiContextArea.WORKOUT)) {
            sections.add(workoutContextBuilder.build(
                    userId,
                    question));
        }
        if (areas.contains(AiContextArea.BODY)) {
            sections.add(bodyContextBuilder.build(userId));
        }
        if (areas.contains(AiContextArea.NUTRITION)) {
            sections.add(nutritionContextBuilder.build(
                    userId,
                    clientContext));
        }
        return sections;
    }
}
