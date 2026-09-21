package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.NewsPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsPostRepository extends JpaRepository<NewsPost, Long> {
    List<NewsPost> findByPublishedTrueOrderByCreatedAtDesc();
    Optional<NewsPost> findBySlug(String slug);
}
