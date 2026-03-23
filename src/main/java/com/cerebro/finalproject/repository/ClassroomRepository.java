package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findByTeacherId(Long teacherId);

    boolean existsByCode(String code);

    Optional<Classroom> findByCode(String code);

    /** Fetch classroom + students in a single query to avoid lazy-load issues in the view. */
    @Query("SELECT c FROM Classroom c LEFT JOIN FETCH c.students WHERE c.id = :id")
    Optional<Classroom> findByIdWithStudents(@Param("id") Long id);
}