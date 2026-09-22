package com.myfitness.exercise.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "exercises", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exercise_name", columnNames = "name")
})
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExerciseCategory category;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Exercise() {}

    private Exercise(String name, ExerciseCategory category, int sortOrder) {
        this.name = name;
        this.category = category;
        this.sortOrder = sortOrder;
    }

    public static Exercise create(String name, ExerciseCategory category, int sortOrder) {
        return new Exercise(name, category, sortOrder);
    }

    public ExerciseReference toReference() {
        return new ExerciseReference(ExerciseType.DEFAULT, id, name, category, null);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ExerciseCategory getCategory() { return category; }
    public int getSortOrder() { return sortOrder; }
}
