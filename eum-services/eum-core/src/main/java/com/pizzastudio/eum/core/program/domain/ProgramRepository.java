package com.pizzastudio.eum.core.program.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long>, ProgramRepositoryCustom {
}
