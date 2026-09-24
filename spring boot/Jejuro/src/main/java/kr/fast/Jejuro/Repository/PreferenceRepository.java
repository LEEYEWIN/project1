package kr.fast.Jejuro.Repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import kr.fast.Jejuro.Entity.Preference;

public interface PreferenceRepository extends JpaRepository<Preference, Long> {

    /** 질문과 그룹 규칙을 한 번에 가져온다(fetch join으로 N+1 방지). */
    @Query("select p from Preference p join fetch p.group order by p.preferenceId")
    List<Preference> findAllWithGroup();
}