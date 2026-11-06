package com.siva.assistant.aa;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByConsentIdOrderByIdAsc(Long consentId);

    List<BankAccount> findByConsentIdInOrderByIdAsc(Collection<Long> consentIds);
}
