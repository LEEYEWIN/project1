package kr.fast.Jejuro.Repository;


//[공통]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import kr.fast.Jejuro.Entity.Code;

public interface CodeRepository extends JpaRepository<Code, Long> {
 List<Code> findByGroupCodeOrderByCodeId(String groupCode);
}