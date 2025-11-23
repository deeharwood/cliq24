package com.cliq24.backend.repository;

import com.cliq24.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
}
