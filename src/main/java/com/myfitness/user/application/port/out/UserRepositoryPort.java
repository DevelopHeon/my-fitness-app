package com.myfitness.user.application.port.out;

import com.myfitness.user.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByGoogleSubject(String googleSubject);
}
