package com.myfitness.exercise.infrastructure.bootstrap;

import com.myfitness.exercise.domain.model.ExerciseCategory;
import java.util.List;

final class DefaultExerciseCatalog {
    private DefaultExerciseCatalog() {}

    static List<Item> items() {
        return List.of(
                new Item("벤치프레스", ExerciseCategory.CHEST, 10),
                new Item("인클라인 벤치프레스", ExerciseCategory.CHEST, 20),
                new Item("덤벨 벤치프레스", ExerciseCategory.CHEST, 30),
                new Item("체스트 프레스", ExerciseCategory.CHEST, 40),
                new Item("케이블 플라이", ExerciseCategory.CHEST, 50),
                new Item("펙덱 플라이", ExerciseCategory.CHEST, 60),
                new Item("딥스", ExerciseCategory.CHEST, 70),
                new Item("오버헤드 프레스", ExerciseCategory.SHOULDER, 10),
                new Item("덤벨 숄더 프레스", ExerciseCategory.SHOULDER, 20),
                new Item("사이드 레터럴 레이즈", ExerciseCategory.SHOULDER, 30),
                new Item("프론트 레이즈", ExerciseCategory.SHOULDER, 40),
                new Item("리어 델트 플라이", ExerciseCategory.SHOULDER, 50),
                new Item("페이스 풀", ExerciseCategory.SHOULDER, 60),
                new Item("랫풀다운", ExerciseCategory.BACK, 10),
                new Item("풀업", ExerciseCategory.BACK, 20),
                new Item("바벨 로우", ExerciseCategory.BACK, 30),
                new Item("시티드 케이블 로우", ExerciseCategory.BACK, 40),
                new Item("원암 덤벨 로우", ExerciseCategory.BACK, 50),
                new Item("티바 로우", ExerciseCategory.BACK, 60),
                new Item("데드리프트", ExerciseCategory.BACK, 70),
                new Item("바벨 컬", ExerciseCategory.ARM, 10),
                new Item("덤벨 컬", ExerciseCategory.ARM, 20),
                new Item("해머 컬", ExerciseCategory.ARM, 30),
                new Item("케이블 푸시다운", ExerciseCategory.ARM, 40),
                new Item("스컬 크러셔", ExerciseCategory.ARM, 50),
                new Item("오버헤드 트라이셉스 익스텐션", ExerciseCategory.ARM, 60),
                new Item("크런치", ExerciseCategory.ABS, 10),
                new Item("레그 레이즈", ExerciseCategory.ABS, 20),
                new Item("행잉 레그 레이즈", ExerciseCategory.ABS, 30),
                new Item("케이블 크런치", ExerciseCategory.ABS, 40),
                new Item("플랭크", ExerciseCategory.ABS, 50),
                new Item("앱 롤아웃", ExerciseCategory.ABS, 60),
                new Item("스쿼트", ExerciseCategory.LEGS, 10),
                new Item("레그 프레스", ExerciseCategory.LEGS, 20),
                new Item("런지", ExerciseCategory.LEGS, 30),
                new Item("레그 익스텐션", ExerciseCategory.LEGS, 40),
                new Item("레그 컬", ExerciseCategory.LEGS, 50),
                new Item("루마니안 데드리프트", ExerciseCategory.LEGS, 60),
                new Item("힙 쓰러스트", ExerciseCategory.LEGS, 70),
                new Item("카프 레이즈", ExerciseCategory.LEGS, 80));
    }

    record Item(String name, ExerciseCategory category, int sortOrder) {}
}
