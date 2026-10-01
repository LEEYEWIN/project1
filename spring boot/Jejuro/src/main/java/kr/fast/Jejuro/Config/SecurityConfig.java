package kr.fast.Jejuro.Config;


import java.util.List;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.Entity.User;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

 @Bean UserDetailsService userDetailsService(UserRepository users) {
     return email -> {
         var user = users.findByEmailIgnoreCase(email)
                 .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 확인할 수 없습니다."));
         return org.springframework.security.core.userdetails.User.withUsername(user.getUserId().toString())
                 .password(user.getPasswordHash()).roles(user.getRole())
                 .disabled(!"ACTIVE".equals(user.getStatus())).build();
     };
 }

 @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
     var provider = new DaoAuthenticationProvider(users);
     provider.setPasswordEncoder(encoder);
     return new ProviderManager(provider);
 }

 @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
 @Bean CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
 @Bean SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository tokens) {
     return new CompositeSessionAuthenticationStrategy(List.of(
             new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(tokens)));
 }

 @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository contexts,
                                     CsrfTokenRepository tokens, UserRepository users,
                                     SocialProviderRegistry socialProviders, SocialLoginHandler socialLogin) throws Exception {
     http.addFilterBefore(new ActiveAccountFilter(users), org.springframework.security.web.access.intercept.AuthorizationFilter.class);
     http.securityContext(context -> context.securityContextRepository(contexts))
         .csrf(csrf -> csrf.csrfTokenRepository(tokens))
         .authorizeHttpRequests(auth -> auth
             .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
             .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/signup", "/api/auth/email/**",
                     "/api/auth/social/providers", "/oauth2/**", "/login/oauth2/**").permitAll()
             .requestMatchers(HttpMethod.GET, "/api/pois/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
             .requestMatchers("/api/admin/**").access((authentication, context) -> {
                 var principal = authentication.get();
                 if (principal == null || !principal.isAuthenticated()
                         || principal instanceof AnonymousAuthenticationToken) {
                     return new AuthorizationDecision(false);
                 }
                 try {
                     boolean admin = users.findById(Long.valueOf(principal.getName()))
                             .map(User::isAdmin).orElse(false);
                     return new AuthorizationDecision(admin);
                 } catch (NumberFormatException ex) {
                     return new AuthorizationDecision(false);
                 }
             })
             .anyRequest().authenticated())
         .exceptionHandling(errors -> errors
             .authenticationEntryPoint((req, res, ex) -> {
                 res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                 res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
             })
             .accessDeniedHandler((req, res, ex) -> {
                 res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                 res.getWriter().write("{\"message\":\"요청 권한이 없거나 인증 정보가 만료되었습니다. 새로고침 후 다시 시도하세요.\"}");
             }))
         .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
         .logout(logout -> logout.logoutUrl("/api/auth/logout")
             .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
             .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
     // 소셜 로그인: application-local.properties 에 키를 넣은 제공자가 있을 때만 켠다.
     if (socialProviders.anyEnabled()) {
         http.oauth2Login(oauth -> oauth.successHandler(socialLogin).failureHandler(socialLogin));
     }
     return http.build();
 }
}