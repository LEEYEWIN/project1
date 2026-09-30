package kr.fast.Jejuro.Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Entity.CommunityComment;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.Repository.CommunityCommentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 검증용 회원·게시글은 테스트 종료 시 모두 롤백한다.
class ProfileCommunityIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired jakarta.persistence.EntityManager entityManager;
 @Autowired UserRepository users;
 @Autowired CommunityCommentRepository comments;
 @Autowired PasswordEncoder encoder;
 @Autowired JdbcTemplate jdbc;
 User user;
 String email;
 String password="Profile-Test-2026!";
 @BeforeEach void setup() {
     email="codex.profile."+UUID.randomUUID()+"@example.com";
     user=users.saveAndFlush(User.register(email,encoder.encode(password),"검증회원",LocalDate.of(2000,1,1),2));
 }
 MockHttpSession login(String pw) throws Exception {
     return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content("{\"email\":\""+email+"\",\"password\":\""+pw+"\"}"))
         .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
 }
 @Test void createsReviewAndCommentUsingImportedDatabaseSchema() throws Exception {
     var session=login(password);
     mvc.perform(post("/api/community/posts").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content("""
             {"postType":"REVIEW","title":"DB 연동 검증 후기","content":"제주 여행 후기 테스트입니다."}
             """)).andExpect(status().isCreated());
     Long postId=jdbc.queryForObject("SELECT MAX(post_id) FROM COMMUNITY_POST WHERE user_id=?",Long.class,user.getUserId());
     comments.saveAndFlush(new CommunityComment(postId,null,user.getUserId(),"검증용 댓글"));
     entityManager.clear(); // 실제 후속 HTTP 요청처럼 DB 기본값을 다시 읽는다.
     mvc.perform(get("/api/community/posts/"+postId).session(session)).andExpect(status().isOk())
         .andExpect(jsonPath("title").value("DB 연동 검증 후기"));
 }
 @Test void editsOwnNicknameAndReportsSocialLinkSeparatelyFromLoginMethod() throws Exception {
     var session=login(password);
     mvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized());
     mvc.perform(get("/api/me/profile").session(session)).andExpect(status().isOk())
         .andExpect(jsonPath("loginMethod").value("EMAIL"))
         .andExpect(jsonPath("socialProviders").isEmpty())
         .andExpect(jsonPath("passwordHash").doesNotExist());
     jdbc.update("INSERT INTO SOCIAL_ACCOUNT(user_id,provider,provider_user_id) VALUES (?,'GOOGLE',?)",user.getUserId(),UUID.randomUUID().toString());
     mvc.perform(get("/api/me/profile").session(session)).andExpect(status().isOk())
         .andExpect(jsonPath("loginMethod").value("EMAIL"))
         .andExpect(jsonPath("socialProviders[0]").value("GOOGLE"));
     mvc.perform(patch("/api/me/profile/nickname").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content("{\"nickname\":\"  새닉네임  \",\"userId\":999}" )).andExpect(status().isNoContent());
     mvc.perform(get("/api/me").session(session)).andExpect(jsonPath("nickname").value("새닉네임"));
     mvc.perform(patch("/api/me/profile/nickname").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content("{\"nickname\":\"  \"}" )).andExpect(status().isBadRequest());
 }
 @Test void passwordChangeRequiresCurrentPasswordAndInvalidatesOtherSessions() throws Exception {
     var first=login(password);var second=login(password);
     String body="{\"currentPassword\":\""+password+"\",\"newPassword\":\"New-Password-2026!\",\"confirmPassword\":\"New-Password-2026!\"}";
     mvc.perform(post("/api/me/profile/password").session(first).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
     mvc.perform(post("/api/me/profile/password").session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content(body.replace(password,"incorrect"))).andExpect(status().isBadRequest());
     assertTrue(encoder.matches(password,users.findById(user.getUserId()).orElseThrow().getPasswordHash()));
     mvc.perform(post("/api/me/profile/password").session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
     assertTrue(first.isInvalid());
     mvc.perform(get("/api/me").session(second)).andExpect(status().isUnauthorized());
     assertTrue(second.isInvalid());
     login("New-Password-2026!");
     mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content("{\"email\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isUnauthorized());
 }
}
