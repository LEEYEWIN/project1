# 이메일 인증 실행 설정

회원가입 화면에서 인증번호를 발송하고 확인한 후 가입합니다.
인증번호와 인증 상태는 서버 메모리에 5분간 유지됩니다. 서버를 재시작하면 다시 인증해야 합니다.

각자의 `spring boot/Jejuro/src/main/resources/application-local.properties`에 다음 설정을 넣습니다.
실제 계정과 앱 비밀번호는 Git에 올리지 않습니다.

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

Spring 실행 환경에 `MAIL_USERNAME`(발신 Gmail 전체 주소), `MAIL_PASSWORD`(Gmail 앱 비밀번호)를 설정하고 서버를 재시작합니다.
Eclipse에서는 Run Configurations → 실행할 Spring 애플리케이션 → Environment에서 설정합니다.

`application-local.properties`는 현재 저장소에서 이미 추적 중이므로 `.gitignore`만으로 변경 사항이 제외되지 않습니다. 커밋 대상으로 선택하지 마세요.

확인 순서: 회원가입 화면 → 인증번호 발송 → 수신 메일의 인증번호 입력 → 인증 확인 → 회원가입.
