package kr.fast.Jejuro.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// [공통] React(다른 주소)에서 스프링 API를 호출할 수 있게 허용하는 CORS 설정
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")                          // API 주소만 허용
                .allowedOrigins(
                        "http://localhost:5173",                // Vite (지금 React)
                        "http://localhost:3000",                // create-react-app을 쓸 경우
                        // CRA 개발 프록시(package.json "proxy")는 Origin 헤더를 대상 주소(8080)로 바꿔 보낸다.
                        // application.properties 의 server.forward-headers-strategy=native 때문에
                        // 서버는 요청 주소를 3000으로 보므로, 8080 Origin 도 허용해야 로그인 등 POST가 막히지 않는다.
                        "http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
