package com.pizzastudio.eum.core.application.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository
    extends JpaRepository<Application, String>, ApplicationRepositoryCustom {
}
