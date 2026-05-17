package com.chatapp.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
            select u from User u
            where (:q is null or :q = ''
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.email)    like lower(concat('%', :q, '%')))
            order by u.createdAt desc
            """)
    Page<User> adminSearch(@Param("q") String q, Pageable pageable);

    @Query("""
            select u from User u
            where lower(u.username) like lower(concat(:q, '%'))
              and u.id <> :excludeId
            order by u.username asc
            """)
    List<User> searchByUsernamePrefix(@Param("q") String q,
                                       @Param("excludeId") UUID excludeId,
                                       Pageable pageable);
}
