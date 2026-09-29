package com.myfitness.user.application.service;

import com.myfitness.user.application.dto.request.GoogleLoginCommand;
import com.myfitness.user.application.dto.response.UserProfileResult;
import com.myfitness.user.application.exception.UserNotFoundException;
import com.myfitness.user.application.port.in.CurrentUserQuery;
import com.myfitness.user.application.port.in.GoogleLoginUseCase;
import com.myfitness.user.application.port.out.UserRepositoryPort;
import com.myfitness.user.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserApplicationService
        implements GoogleLoginUseCase, CurrentUserQuery {
    private final UserRepositoryPort userRepository;
    private final Clock clock;

    @Autowired
    public UserApplicationService(UserRepositoryPort userRepository) {
        this(userRepository, Clock.systemUTC());
    }

    UserApplicationService(
            UserRepositoryPort userRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserProfileResult login(GoogleLoginCommand command) {
        Instant now = clock.instant();
        User user = userRepository
                .findByGoogleSubject(command.googleSubject())
                .map(existing -> updateProfile(existing, command, now))
                .orElseGet(() -> createUser(command, now));

        return UserProfileResult.from(userRepository.save(user));
    }

    @Override
    public UserProfileResult getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserProfileResult::from)
                .orElseThrow(UserNotFoundException::new);
    }

    private static User createUser(
            GoogleLoginCommand command,
            Instant now) {
        return User.createFromGoogle(
                command.googleSubject(),
                command.email(),
                command.displayName(),
                command.profileImageUrl(),
                now);
    }

    private static User updateProfile(
            User user,
            GoogleLoginCommand command,
            Instant now) {
        user.recordGoogleLogin(
                command.email(),
                command.displayName(),
                command.profileImageUrl(),
                now);
        return user;
    }
}
