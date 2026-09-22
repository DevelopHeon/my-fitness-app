package com.myfitness.dashboard.application.service;

import com.myfitness.dashboard.application.port.out.DashboardDataPort.BodyData;
import com.myfitness.dashboard.application.result.DashboardResult.BodyChange;
import com.myfitness.dashboard.application.result.DashboardResult.BodyPoint;
import com.myfitness.dashboard.application.result.DashboardResult.BodySummary;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

final class DashboardBodySummaryBuilder {

    BodySummary build(
            List<BodyData> records,
            Instant historyStart) {
        if (records.isEmpty()) {
            return new BodySummary(null, null, List.of());
        }

        List<BodyData> orderedByMeasuredAtDesc = records.stream()
                .sorted(Comparator
                        .comparing(BodyData::getMeasuredAt)
                        .thenComparing(BodyData::getId)
                        .reversed())
                .toList();

        BodyData latest = orderedByMeasuredAtDesc.getFirst();
        BodyChange change = orderedByMeasuredAtDesc.size() < 2
                ? null
                : change(
                        latest,
                        orderedByMeasuredAtDesc.get(1));

        List<BodyPoint> history = orderedByMeasuredAtDesc.stream()
                .filter(record ->
                        !record.getMeasuredAt().isBefore(historyStart))
                .sorted(Comparator
                        .comparing(BodyData::getMeasuredAt)
                        .thenComparing(BodyData::getId))
                .map(DashboardBodySummaryBuilder::bodyPoint)
                .toList();

        return new BodySummary(
                bodyPoint(latest),
                change,
                history);
    }

    private static BodyChange change(
            BodyData latest,
            BodyData previous) {
        return new BodyChange(
                latest.getWeightKg().subtract(
                        previous.getWeightKg()),
                latest.getBodyFatPercentage().subtract(
                        previous.getBodyFatPercentage()),
                latest.getSkeletalMuscleKg().subtract(
                        previous.getSkeletalMuscleKg()));
    }

    private static BodyPoint bodyPoint(BodyData record) {
        return new BodyPoint(
                record.getMeasuredAt(),
                record.getWeightKg(),
                record.getBodyFatPercentage(),
                record.getSkeletalMuscleKg());
    }
}
