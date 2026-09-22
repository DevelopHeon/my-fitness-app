package com.myfitness.routine.infrastructure.persistence;

import com.myfitness.routine.domain.model.Routine;
import com.myfitness.routine.application.port.out.RoutineRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RoutineRepositoryAdapter implements RoutineRepositoryPort {
    private final SpringDataRoutineRepository repository;

    public RoutineRepositoryAdapter(SpringDataRoutineRepository repository) {
        this.repository = repository;
    }

    @Override
    public Routine save(Routine routine) {
        return repository.saveAndFlush(routine);
    }

    @Override
    public Optional<Routine> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Routine> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCase(Long userId, String name) {
        return repository.existsByUserIdAndNameIgnoreCase(userId, name);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCaseAndIdNot(
            Long userId, String name, Long id) {
        return repository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id);
    }

    @Override
    public void delete(Routine routine) {
        repository.delete(routine);
        repository.flush();
    }
}
