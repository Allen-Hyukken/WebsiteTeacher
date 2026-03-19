package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findByTeacherId(Long teacherId);

    boolean existsByCode(String code);

    Optional<Classroom> findByCode(String code);
}