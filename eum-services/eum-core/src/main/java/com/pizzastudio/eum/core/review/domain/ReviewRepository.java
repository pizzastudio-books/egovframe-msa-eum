package com.pizzastudio.eum.core.review.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByApplicationIdOrderByReviewIdDesc(String applicationId);
}
