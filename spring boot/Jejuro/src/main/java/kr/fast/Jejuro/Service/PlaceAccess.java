package kr.fast.Jejuro.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 차로 바로 가기 어려운 관광지(섬·산) 구분. 관광지 이름·주소·유형으로 판단한다.
 * - 섬: 우도·추자도·마라도·가파도·비양도·범섬 등 (배를 타야 함)
 * - 산: 오름·한라산 탐방로·○○산·○○봉·○○악 (자연·탐방 유형만, 차는 입구 주차장까지)
 */
public final class PlaceAccess {

    public enum Kind { ISLAND, MOUNTAIN }

    private static final List<String> ISLAND_ADDRESS = List.of("우도면", "추자면", "마라리", "마라로", "가파리", "비양도길");
    private static final Pattern ISLAND_NAME =
            Pattern.compile("^(우도|마라도|가파도|비양도|차귀도|추자도|범섬|섶섬|문섬|지귀도|형제섬)|섬$");
    private static final Set<String> MOUNTAIN_CATEGORIES = Set.of("NATURE", "TRAIL", "EXPERIENCE");
    private static final Pattern MOUNTAIN_SUFFIX = Pattern.compile("^[가-힣\\s]+(산|봉|악)$");

    private PlaceAccess() {}

    public static Optional<Kind> of(String name, String address, String category) {
        String n = name == null ? "" : name.strip();
        String a = address == null ? "" : address;
        if (ISLAND_ADDRESS.stream().anyMatch(a::contains) || ISLAND_NAME.matcher(n).find()) {
            return Optional.of(Kind.ISLAND);
        }
        if (category == null || !MOUNTAIN_CATEGORIES.contains(category)) return Optional.empty();
        String base = n.replaceAll("\\s*(입구|산책로|탐방로)$", "").strip();
        boolean mountain = n.contains("오름") || n.contains("탐방로") || n.contains("1100고지")
                || (n.startsWith("한라산") && !n.contains("마을"))
                || MOUNTAIN_SUFFIX.matcher(base).matches();
        return mountain ? Optional.of(Kind.MOUNTAIN) : Optional.empty();
    }

    /** 경로 짜기·동선 안내 화면에 보여 줄 안내 문구 */
    public static String message(Kind kind) {
        return switch (kind) {
            case ISLAND -> "섬이라 차로 바로 갈 수 없어요. 배편(출항 시간, 날씨에 따른 운항 여부)을 미리 확인하세요.";
            case MOUNTAIN -> "산·오름이라 차는 입구 주차장까지만 갈 수 있어요. 주차 가능 여부와 등산(탐방) 가능 여부(입산 통제, 탐방 예약)를 미리 확인하세요.";
        };
    }
}
