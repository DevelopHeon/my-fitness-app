package com.myfitness.user.infrastructure.persistence;

import com.myfitness.user.application.port.out.UserRepositoryPort;
import com.myfitness.user.domain.model.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository repository;

    public UserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return repository.findByGoogleSubject(googleSubject);
    }
}
