package com.myfitness.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.nutrition.application.dto.request.MealItemCommand;
import com.myfitness.nutrition.application.port.in.NutritionUseCase;
import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import com.myfitness.nutrition.domain.model.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MealBatchTransactionIntegrationTest {
    @Autowired NutritionUseCase nutritionUseCase;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void cleanRecords() {
        jdbc.update("delete from meal_foods where meal_id in (select id from meals where user_id = 9001)");
        jdbc.update("delete from meals where user_id = 9001");
    }

    @Test
    void rollsBackEarlierFoodAndMealWhenLaterDomainValidationFails() {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<MealItemCommand> items = List.of(
                new MealItemCommand("밥", new BigDecimal("300"), null, null, null),
                new MealItemCommand("잘못된 음식", new BigDecimal("-1"), null, null, null));
        assertThatThrownBy(() -> nutritionUseCase.addMealItems(9001L, date, MealType.LUNCH, items))
                .isInstanceOf(NutritionRuleException.class);
        assertThat(jdbc.queryForObject("select count(*) from meals where user_id = 9001", Integer.class))
                .isZero();
        assertThat(nutritionUseCase.daily(9001L, date).consumed().calories()).isEqualByComparingTo("0");
    }
}
