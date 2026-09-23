package com.myfitness.user.infrastructure.persistence;

import com.myfitness.user.domain.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleSubject(String googleSubject);
}
