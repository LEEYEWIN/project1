package kr.fast.Jejuro.Config;


//[공통]

import org.springframework.http.HttpStatus;

/** 서비스에서 던지는 예외. 상태 코드와 화면에 보여줄 메시지를 담는다. */
public class ApiException extends RuntimeException {
 private final HttpStatus status;

 public ApiException(HttpStatus status, String message) {
     super(message);
     this.status = status;
 }

 public static ApiException badRequest(String message) {
     return new ApiException(HttpStatus.BAD_REQUEST, message);
 }

 public static ApiException notFound(String message) {
     return new ApiException(HttpStatus.NOT_FOUND, message);
 }

 public HttpStatus getStatus() {
     return status;
 }
}
