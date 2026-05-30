package com.taskflow.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndIsDeletedFalse(Long id);

    List<Project> findByOwner_IdAndIsDeletedFalse(Long ownerId);

    boolean existsByIdAndIsDeletedFalse(Long id);

}
