package kr.fast.Jejuro.Repository;


import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.Companion;

public interface CompanionRepository extends JpaRepository<Companion, Long> {

    List<Companion> findByTravelIdOrderByCompanionSeq(Long travelId);

    List<Companion> findByTravelIdIn(Collection<Long> travelIds);
}