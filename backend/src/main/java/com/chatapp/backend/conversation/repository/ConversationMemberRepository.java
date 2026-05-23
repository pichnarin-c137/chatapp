package com.chatapp.backend.conversation.repository;
import com.chatapp.backend.conversation.entity.ConversationMemberId;
import com.chatapp.backend.conversation.entity.ConversationMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationMember> findByUserIdAndLeftAtIsNull(UUID userId);

    List<ConversationMember> findByConversationId(UUID conversationId);

    @Query("""
            select count(m) from ConversationMember m
             where m.id.conversationId = :conversationId
               and m.leftAt is null
            """)
    long countActive(@Param("conversationId") UUID conversationId);

    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(UUID conversationId, UUID userId);
}
