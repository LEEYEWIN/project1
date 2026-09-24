package kr.fast.Jejuro.Repository;


//[공통]

import org.springframework.data.jpa.repository.JpaRepository;
import kr.fast.Jejuro.Entity.Region;

public interface RegionRepository extends JpaRepository<Region, Integer> {
}