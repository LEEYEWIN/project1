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
                        "http://localhost:3000")                // create-react-app을 쓸 경우
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")                            // X-User-Id(테스트 회원) 헤더 포함
                .allowCredentials(true);
    }
}