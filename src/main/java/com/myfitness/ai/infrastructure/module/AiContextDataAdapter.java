package com.myfitness.ai.infrastructure.module;

import com.myfitness.ai.application.port.out.BodyContextPort;
import com.myfitness.ai.application.port.out.NutritionContextPort;
import com.myfitness.ai.application.port.out.WorkoutContextPort;
import com.myfitness.body.application.port.in.insight.BodyInsightQuery;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery;
import com.myfitness.workout.application.port.in.insight.WorkoutInsightQuery;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiContextDataAdapter implements BodyContextPort, WorkoutContextPort, NutritionContextPort {
    private final BodyInsightQuery body;
    private final WorkoutInsightQuery workout;
    private final NutritionInsightQuery nutrition;

    public AiContextDataAdapter(BodyInsightQuery body, WorkoutInsightQuery workout, NutritionInsightQuery nutrition) {
        this.body = body;
        this.workout = workout;
        this.nutrition = nutrition;
    }

    @Override
    public List<BodyInsight> findRecent(Long userId, int limit) {
        return body.findRecent(userId, limit).stream().map(record -> new BodyInsight(
                record.id(), record.measuredAt(), record.weightKg(),
                record.bodyFatPercentage(), record.skeletalMuscleKg())).toList();
    }

    @Override
    public List<WorkoutInsight> findCompletedSince(Long userId, LocalDate from) {
        return workout.findCompletedSince(userId, from).stream().map(record -> new WorkoutInsight(
                record.workoutDate(), record.startedAt(),
                record.exercises().stream().map(AiContextDataAdapter::toExercise).toList())).toList();
    }

    @Override
    public NutritionDayInsight getDay(Long userId, LocalDate date) {
        NutritionInsightQuery.NutritionDayInsight day = nutrition.getDay(userId, date);
        return new NutritionDayInsight(
                day.date(), toMacro(day.goal()), toMacro(day.consumed()), toMacro(day.remaining()),
                day.recentFoods(), day.frequentFoods());
    }

    private static ExerciseInsight toExercise(WorkoutInsightQuery.ExerciseInsight exercise) {
        return new ExerciseInsight(
                exercise.exerciseType(), exercise.exerciseId(), exercise.exerciseName(), exercise.category(),
                exercise.sets().stream().map(set -> new SetInsight(set.weightKg(), set.reps(), set.completed())).toList());
    }

    private static MacroInsight toMacro(NutritionInsightQuery.MacroInsight macro) {
        return macro == null ? null : new MacroInsight(
                macro.calories(), macro.carbohydrateGrams(), macro.proteinGrams(), macro.fatGrams());
    }
}
