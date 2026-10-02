package kr.fast.Jejuro.Config;



//[공통]

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** 모든 에러를 { "message": "..." } 형태로 통일해서 React가 같은 방식으로 처리하게 한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

 private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

 @ExceptionHandler(ApiException.class)
 public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
     return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
 }

 /** @Valid 검사 실패 (빈 제목 등) */
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String, String>> handleValid(MethodArgumentNotValidException e) {
     String msg = e.getBindingResult().getFieldErrors().stream()
             .findFirst()
             .map(f -> f.getField() + ": " + f.getDefaultMessage())
             .orElse("입력값을 확인하세요.");
     return ResponseEntity.badRequest().body(Map.of("message", msg));
 }

 /** 사진이 application.properties의 최대 크기(5MB)를 넘음 */
 @ExceptionHandler(MaxUploadSizeExceededException.class)
 public ResponseEntity<Map<String, String>> handleUploadSize(MaxUploadSizeExceededException e) {
     return ResponseEntity.badRequest().body(Map.of("message", "사진은 5MB 이하만 올릴 수 있습니다."));
 }

 /** DB 제약(UNIQUE, CHECK, FK, 트리거) 위반 */
 @ExceptionHandler(DataIntegrityViolationException.class)
 public ResponseEntity<Map<String, String>> handleDb(DataIntegrityViolationException e) {
     String cause = String.valueOf(e.getMostSpecificCause().getMessage());
     // 같은 닉네임 동시 가입·변경을 DB UNIQUE(uk_user_nickname)가 막은 경우
     if (cause.contains("uk_user_nickname")) {
         return ResponseEntity.status(HttpStatus.CONFLICT)
                 .body(Map.of("message", "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요."));
     }
     return ResponseEntity.status(HttpStatus.CONFLICT)
             .body(Map.of("message", "저장할 수 없는 값입니다. 중복이나 허용 범위를 확인하세요."));
 }

 /** 그 밖의 DB 오류 (없는 표·칼럼 등) → "요청 중 문제" 대신 DB 문제라고 알려 주고 원인은 서버 로그에 */
 @ExceptionHandler(DataAccessException.class)
 public ResponseEntity<Map<String, String>> handleDbAccess(DataAccessException e) {
     log.error("DB 오류: {}", e.getMostSpecificCause().getMessage(), e);
     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body(Map.of("message", "DB 조회 중 문제가 발생했어요. DB 표·칼럼이 최신인지 확인해 주세요."));
 }
}