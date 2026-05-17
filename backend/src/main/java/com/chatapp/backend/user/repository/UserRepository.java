package com.chatapp.backend.user.repository;
import com.chatapp.backend.user.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
            select u from User u
            where u.deletedAt is null
              and lower(u.username) like lower(concat(:q, '%'))
              and u.id <> :excludeId
            order by u.username asc
            """)
    List<User> searchByUsernamePrefix(@Param("q") String q,
                                       @Param("excludeId") UUID excludeId,
                                       Pageable pageable);

    @Query("""
            select u from User u
            where u.deletedAt is null
              and (:q is null or :q = ''
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.email)    like lower(concat('%', :q, '%')))
            order by u.createdAt desc
            """)
    Page<User> adminSearch(@Param("q") String q, Pageable pageable);
}
