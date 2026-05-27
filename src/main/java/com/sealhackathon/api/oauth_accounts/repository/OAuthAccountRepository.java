package com.sealhackathon.api.oauth_accounts.repository;

import com.sealhackathon.api.oauth_accounts.entity.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Integer> {

    Optional<OAuthAccount> findByProviderAndProviderUid(String provider, String providerUid);

    Optional<OAuthAccount> findByUserIdAndProvider(Integer userId, String provider);

    long countByUserId(Integer userId);
}
