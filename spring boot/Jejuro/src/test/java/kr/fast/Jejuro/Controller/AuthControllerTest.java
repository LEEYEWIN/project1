package kr.fast.Jejuro.Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import kr.fast.Jejuro.Config.CurrentUser;
import kr.fast.Jejuro.Config.SecurityConfig;
import kr.fast.Jejuro.Entity.User;
import kr.fast.Jejuro.Repository.UserRepository;
import kr.fast.Jejuro.Service.AuthService;

@WebMvcTest(controllers = {AuthController.class, MeController.class})
@Import({SecurityConfig.class, AuthService.class, CurrentUser.class})
class AuthControllerTest {
 @Autowired MockMvc mvc;
 @Autowired PasswordEncoder encoder;
 @MockitoBean UserRepository users;
 private final AtomicReference<User> stored = new AtomicReference<>();
 private static final String SIGNUP = """
     {"email":"hello@example.com","password":"correct-horse-123","nickname":"제주여행자","birthDate":"2000-05-14","genderCode":2}
     """;
 @BeforeEach void setup() {
     stored.set(null);
     when(users.existsByEmailIgnoreCase(anyString())).thenAnswer(call -> stored.get() != null);
     when(users.findByEmailIgnoreCase(anyString())).thenAnswer(call -> Optional.ofNullable(stored.get()));
     when(users.findByIdForUpdate(anyLong())).thenAnswer(call -> Optional.ofNullable(stored.get()));
     when(users.findById(anyLong())).thenAnswer(call -> Optional.ofNullable(stored.get()));
     when(users.saveAndFlush(any(User.class))).thenAnswer(call -> {
         User user = call.getArgument(0); ReflectionTestUtils.setField(user, "userId", 42L);
         stored.set(user); return user;
     });
 }
 private MockHttpSession signup() throws Exception {
     return (MockHttpSession) mvc.perform(post("/api/auth/signup").with(csrf())
             .contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
         .andExpect(status().isCreated()).andExpect(jsonPath("userId").value(42))
         .andExpect(jsonPath("role").value("USER"))
         .andExpect(jsonPath("passwordHash").doesNotExist())
         .andReturn().getRequest().getSession(false);
 }
 @Test void signupStoresHashAndCreatesSessionThenLogoutRevokesIt() throws Exception {
     MockHttpSession session = signup();
     assertNotEquals("correct-horse-123", stored.get().getPasswordHash());
     assertTrue(encoder.matches("correct-horse-123", stored.get().getPasswordHash()));
     mvc.perform(get("/api/me").session(session).header("X-User-Id", "999"))
         .andExpect(status().isOk()).andExpect(jsonPath("userId").value(42));
     mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
     mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
     assertTrue(session.isInvalid());
     mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
 }
 @Test void loginVerifiesPasswordAndRotatesSession() throws Exception {
     signup();
     mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
             .content("{\"email\":\"hello@example.com\",\"password\":\"incorrect\"}"))
         .andExpect(status().isUnauthorized());
     MockHttpSession session = new MockHttpSession();
     String oldId = session.getId();
     mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
             .content("{\"email\":\"HELLO@example.com\",\"password\":\"correct-horse-123\"}"))
         .andExpect(status().isOk());
     assertNotEquals(oldId, session.getId());
     mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());
 }
 @Test void rejectsDuplicateEmailInvalidSignupAndMissingCsrf() throws Exception {
     mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
         .andExpect(status().isForbidden());
     mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
             .content(SIGNUP.replace("correct-horse-123", "short"))).andExpect(status().isBadRequest());
     verify(users, never()).saveAndFlush(any());
     signup();
     mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
         .andExpect(status().isConflict());
 }
 @Test void headersCannotImpersonateUsersAndMembersCannotAccessAdmin() throws Exception {
     mvc.perform(get("/api/me").header("X-User-Id", "1")).andExpect(status().isUnauthorized());
     MockHttpSession session = signup();
     mvc.perform(get("/api/admin/kpi").session(session)).andExpect(status().isForbidden());
     mvc.perform(get("/api/test-users").session(session)).andExpect(status().isForbidden());
 }
 @Test void csrfEndpointIssuesTokenForBrowserClient() throws Exception {
     mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
         .andExpect(jsonPath("token").isNotEmpty()).andExpect(jsonPath("headerName").value("X-CSRF-TOKEN"));
 }

 @Test void withdrawalRequiresPasswordConsentAndCsrf() throws Exception {
     MockHttpSession session = signup();
     String body = """
         {"password":"correct-horse-123","confirmed":true}
         """;
     mvc.perform(post("/api/auth/withdraw").session(session).contentType(MediaType.APPLICATION_JSON)
         .content(body)).andExpect(status().isForbidden());
     mvc.perform(post("/api/auth/withdraw").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content(body.replace("correct-horse-123", "wrong"))).andExpect(status().isBadRequest());
     mvc.perform(post("/api/auth/withdraw").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content(body.replace("true", "false"))).andExpect(status().isBadRequest());
     assertEquals("ACTIVE", stored.get().getStatus());
     assertNull(stored.get().getWithdrawnAt());
     mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());
 }
 @Test void withdrawalDisablesLoginAndAllExistingSessions() throws Exception {
     MockHttpSession first = signup();
     String login = """
         {"email":"hello@example.com","password":"correct-horse-123"}
         """;
     MockHttpSession second = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
         .contentType(MediaType.APPLICATION_JSON).content(login))
         .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
     mvc.perform(post("/api/auth/withdraw").session(first).with(csrf()).contentType(MediaType.APPLICATION_JSON)
         .content("""
             {"password":"correct-horse-123","confirmed":true,"userId":999}
             """))
         .andExpect(status().isNoContent());
     verify(users).findByIdForUpdate(42L);
     assertEquals("WITHDRAWAL_PENDING", stored.get().getStatus());
     assertNotNull(stored.get().getWithdrawnAt());
     assertTrue(first.isInvalid());
     mvc.perform(get("/api/me").session(second)).andExpect(status().isUnauthorized());
     assertTrue(second.isInvalid());
     mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login))
         .andExpect(status().isUnauthorized());
 }
}
