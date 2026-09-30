package kr.fast.Jejuro.Repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.fast.Jejuro.Entity.PreferenceOption;

public interface PreferenceOptionRepository extends JpaRepository<PreferenceOption, Long> {
    List<PreferenceOption> findAllByOrderByPreferenceIdAscOptionValueAsc();
}