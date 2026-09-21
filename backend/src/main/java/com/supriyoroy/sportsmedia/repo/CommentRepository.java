package com.supriyoroy.sportsmedia.repo;

import com.supriyoroy.sportsmedia.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByApprovedTrueAndMatchIdOrderByPinnedDescCreatedAtDesc(Long matchId);
    List<Comment> findTop50ByApprovedTrueOrderByPinnedDescCreatedAtDesc();
    List<Comment> findByApprovedFalseOrderByCreatedAtDesc();
    long countByApprovedFalse();
}
