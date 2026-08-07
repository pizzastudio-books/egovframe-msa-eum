package com.pizzastudio.eum.application.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository
    extends JpaRepository<Application, String>, ApplicationRepositoryCustom {
}
