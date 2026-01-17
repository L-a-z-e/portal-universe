package com.portal.universe.authservice.repository;

import com.portal.universe.authservice.domain.SocialAccount;
import com.portal.universe.authservice.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
