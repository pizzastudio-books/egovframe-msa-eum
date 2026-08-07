package com.pizzastudio.eum.code.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeRepository extends JpaRepository<Code, String> {

    List<Code> findByParentCodeIdAndUseAtTrueOrderBySortSeq(String parentCodeId);
}
