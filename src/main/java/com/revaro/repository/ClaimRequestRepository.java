package com.revaro.repository;

import com.revaro.entity.ClaimRequest;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRequestRepository extends JpaRepository<ClaimRequest, Long> {

    List<ClaimRequest> findByStatusOrderByCreatedAtAsc(ClaimStatus status);

    List<ClaimRequest> findAllByOrderByCreatedAtDesc();

    List<ClaimRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    boolean existsByRequesterAndEventAndStatus(User requester, Event event, ClaimStatus status);

    long countByStatus(ClaimStatus status);

    @Modifying
    @Query("UPDATE ClaimRequest c SET c.reviewedBy = null WHERE c.reviewedBy = :user")
    void clearReviewer(@Param("user") User user);
}
