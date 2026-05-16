package com.chatapp.backend.dm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, UUID> {

    Optional<DirectConversation> findByUserLowAndUserHigh(UUID userLow, UUID userHigh);

    @Query("""
            select c from DirectConversation c
            where c.userLow = :userId or c.userHigh = :userId
            order by coalesce(c.lastMessageAt, c.createdAt) desc
            """)
    List<DirectConversation> findAllForUser(@Param("userId") UUID userId);
}
