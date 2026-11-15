package com.devlab.taskboard.repository;

import com.devlab.taskboard.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {

    Optional<Label> findByIdAndOwnerId(Long id, Long ownerId);

    List<Label> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    List<Label> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);
}
