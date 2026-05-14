package com.shorturl.service.repository;

import com.shorturl.service.model.IdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdSequenceRepository extends JpaRepository<IdSequence, String> {
}
