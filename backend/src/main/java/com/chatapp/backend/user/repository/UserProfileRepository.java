package com.chatapp.backend.user.repository;
import com.chatapp.backend.user.entity.UserProfile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
