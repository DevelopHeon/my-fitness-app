package com.myfitness.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.nutrition.application.port.in.NutritionPort.MealItemInput;
import com.myfitness.nutrition.application.port.in.NutritionPort;
import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import com.myfitness.nutrition.application.port.in.NutritionPort.MealType;
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
    @Autowired NutritionPort nutritionPort;
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
        List<MealItemInput> items = List.of(
                new MealItemInput("밥", new BigDecimal("300"), null, null, null),
                new MealItemInput("잘못된 음식", new BigDecimal("-1"), null, null, null));
        assertThatThrownBy(() -> nutritionPort.addMealItems(9001L, date, MealType.LUNCH, items))
                .isInstanceOf(NutritionRuleException.class);
        assertThat(jdbc.queryForObject("select count(*) from meals where user_id = 9001", Integer.class))
                .isZero();
        assertThat(nutritionPort.daily(9001L, date).consumed().calories()).isEqualByComparingTo("0");
    }
}
