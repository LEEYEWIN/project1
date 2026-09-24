package kr.fast.Jejuro.Config;


//[공통]

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 모든 에러를 { "message": "..." } 형태로 통일해서 React가 같은 방식으로 처리하게 한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

 /** DB 제약(UNIQUE, CHECK, FK, 트리거) 위반 */
 @ExceptionHandler(DataIntegrityViolationException.class)
 public ResponseEntity<Map<String, String>> handleDb(DataIntegrityViolationException e) {
     return ResponseEntity.status(HttpStatus.CONFLICT)
             .body(Map.of("message", "저장할 수 없는 값입니다. 중복이나 허용 범위를 확인하세요."));
 }
}