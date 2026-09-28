package kr.fast.Jejuro.Service;


//[커뮤니티 게시판 - 첨부 사진]

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kr.fast.Jejuro.Config.ApiException;

/**
* 게시글 첨부 사진 1장을 서버 폴더에 저장하고 꺼내 준다.
* - 저장 폴더: application.properties 의 app.upload-dir (기본 uploads/community, Spring 실행 폴더 기준)
* - 파일 이름: 무작위 UUID + 확장자 (사용자가 올린 파일 이름은 쓰지 않음 → 경로 조작 방지)
* - 허용: JPG·PNG·GIF·WEBP, 5MB 이하. 확장자가 아니라 파일 앞부분(시그니처)으로 진짜 사진인지 확인
* - 화면에서 쓰는 주소: /api/community/images/{파일이름}
*/
@Service
public class CommunityImageService {

 public static final String URL_PREFIX = "/api/community/images/";
 private static final long MAX_BYTES = 5L * 1024 * 1024;
 private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f\\-]{36}\\.(jpg|png|gif|webp)$");
 private static final Map<String, String> CONTENT_TYPE = Map.of(
         "jpg", "image/jpeg", "png", "image/png", "gif", "image/gif", "webp", "image/webp");

 private final Path dir;

 public CommunityImageService(@Value("${app.upload-dir:uploads/community}") String uploadDir) {
     this.dir = Paths.get(uploadDir).toAbsolutePath().normalize();
 }

 /** 사진 저장 → 화면에서 쓸 주소 */
 public String store(MultipartFile file) {
     if (file == null || file.isEmpty()) {
         throw ApiException.badRequest("사진 파일을 골라 주세요.");
     }
     if (file.getSize() > MAX_BYTES) {
         throw ApiException.badRequest("사진은 5MB 이하만 올릴 수 있습니다.");
     }
     String ext;
     try (InputStream in = file.getInputStream()) {
         ext = detectImageType(in.readNBytes(12));
     } catch (IOException e) {
         throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "사진을 읽지 못했습니다.");
     }
     if (ext == null) {
         throw ApiException.badRequest("JPG, PNG, GIF, WEBP 사진만 올릴 수 있습니다.");
     }
     String fileName = UUID.randomUUID() + "." + ext;
     try {
         Files.createDirectories(dir);
         try (InputStream in = file.getInputStream()) {
             Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
         }
     } catch (IOException e) {
         throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "사진을 저장하지 못했습니다.");
     }
     return URL_PREFIX + fileName;
 }

 /** 주소가 우리가 저장한 사진인지 확인 (글 저장 때). null이면 사진 없음 */
 public String validateUrl(String imageUrl) {
     if (imageUrl == null || imageUrl.isBlank()) {
         return null;
     }
     String fileName = fileNameOf(imageUrl);
     if (fileName == null || !Files.exists(dir.resolve(fileName))) {
         throw ApiException.badRequest("첨부 사진을 찾을 수 없습니다. 사진을 다시 올려 주세요.");
     }
     return URL_PREFIX + fileName;
 }

 /** 사진 파일 꺼내기 (GET /api/community/images/{fileName}) */
 public Resource load(String fileName) {
     if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
         throw ApiException.notFound("사진을 찾을 수 없습니다.");
     }
     Path path = dir.resolve(fileName).normalize();
     if (!path.startsWith(dir) || !Files.exists(path)) {
         throw ApiException.notFound("사진을 찾을 수 없습니다.");
     }
     return new FileSystemResource(path);
 }

 public String contentTypeOf(String fileName) {
     return CONTENT_TYPE.getOrDefault(fileName.substring(fileName.lastIndexOf('.') + 1), "application/octet-stream");
 }

 /** 글에서 사진을 바꾸거나 지웠을 때 예전 파일 정리 (실패해도 글 저장에는 영향 없음) */
 public void deleteQuietly(String imageUrl) {
     String fileName = fileNameOf(imageUrl);
     if (fileName == null) return;
     try {
         Files.deleteIfExists(dir.resolve(fileName));
     } catch (IOException ignored) {
         // 파일이 남아도 화면에는 영향 없음
     }
 }

 private String fileNameOf(String imageUrl) {
     if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) return null;
     String name = imageUrl.substring(URL_PREFIX.length());
     Matcher m = FILE_NAME.matcher(name);
     return m.matches() ? name : null;
 }

 /** 파일 앞 몇 바이트로 사진 종류 판별 */
 private static String detectImageType(byte[] b) {
     if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
     if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
     if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
     if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
             && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
     return null;
 }
}
