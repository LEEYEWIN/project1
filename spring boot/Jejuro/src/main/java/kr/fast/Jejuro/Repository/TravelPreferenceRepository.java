package kr.fast.Jejuro.Repository;


import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.TravelPreference;

public interface TravelPreferenceRepository extends JpaRepository<TravelPreference, Long> {
}