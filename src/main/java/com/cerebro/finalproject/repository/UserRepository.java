package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Fetch user with teacher classes eagerly loaded
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.teacherClasses WHERE u.id = :id")
    Optional<User> findByIdWithTeacherClasses(@Param("id") Long id);
}