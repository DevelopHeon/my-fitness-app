package com.myfitness.routine.application.port.out;

import com.myfitness.routine.application.usecase.response.RoutineResult;
import java.util.List;

public interface RoutineReadPort {
    List<RoutineResult> findAllByUserId(Long userId);
}
