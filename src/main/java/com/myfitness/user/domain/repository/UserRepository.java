package com.myfitness.user.domain.repository;

import com.myfitness.user.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByGoogleSubject(String googleSubject);
}
