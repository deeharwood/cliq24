package com.cliq24.backend.repository;

import com.cliq24.backend.model.SocialAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends MongoRepository<SocialAccount, String> {
    List<SocialAccount> findByUserId(String userId);
    Optional<SocialAccount> findByUserIdAndPlatform(String userId, String platform);
    void deleteByUserIdAndPlatform(String userId, String platform);
    long countByUserId(String userId);
}
