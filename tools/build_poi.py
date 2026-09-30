# -*- coding: utf-8 -*-
"""
제주 관광지 목록 만들기 (AI 후보 275곳 + 추가 관광지) → POI / POI_SOURCE_MAP SQL
================================================================================
데이터 출처와 역할
  ⓪ poi_base.csv (팀이 만든 기준 목록, 있으면 사용): poi_id·이름·좌표·설명을 기본으로 삼고 부족한 칸만 채움
  ① 비짓제주 API (제주관광공사)   : 관광지 목록 + 사진 + 한 줄 소개(introduction)   ← 목록을 늘리는 주 재료
  ② TourAPI (한국관광공사)         : 관광지·문화시설·레포츠·쇼핑 목록 + 사진 + 소개글 ← 목록을 늘리는 보조 재료
  ③ 카카오 로컬 API               : 좌표·주소 확인, 좌표 → 읍면동(권역 계산), 분류 힌트
  ④ 네이버 지역 검색 API          : 카카오가 못 찾은 곳의 좌표·주소·분류 힌트(보조)
  ※ 네이버·카카오는 사진·설명을 API로 주지 않아 쓰지 않습니다(이미지 검색 결과는 저작권 문제로 사용 안 함).

결과 (output/ 폴더)
  poi_all.sql          : POI + POI_SOURCE_MAP INSERT
                         번호: 기준 목록(poi_base.csv) poi_id 그대로 / 없던 AI 후보 1001~ / 새 관광지 2001~
  poi_제외목록.csv       : 기준 목록에서 뺀 곳(숙박·가게·기관·중복·제주 밖) + 사유
  poi_목록.csv          : 전체 목록(출처·분류·사진·한 줄 소개·세부 설명) → 엑셀로 검토
  poi_확인필요.csv       : 좌표·사진·설명이 애매한 곳
  poi_확인.html         : 지도 + 사진·설명 카드 (브라우저로 열기)
  api_cache.json       : API 결과 저장(다시 실행하면 API를 또 부르지 않음 → 하루 한도 절약)

손으로 고치기: poi_overrides.csv (처음 실행 때 빈 양식 생성)
  칸: 이름, latitude, longitude, address, image_url, description, detail_description, category, exclude
  - 이름: poi_목록.csv의 name 그대로 (AI 후보는 VISIT_AREA_NM로 적어도 됨)
  - exclude: Y = 뺌 / N = 자동 제외된 곳을 다시 넣음
  - category: NATURE/HISTORY/CULTURE/COMMERCIAL/LEISURE/THEME/TRAIL/FESTIVAL/EXPERIENCE
  - exclude: Y 이면 목록에서 뺌(추가 관광지만)

실행
  python -m pip install requests
  python build_poi.py                    ← 키를 차례로 물어봄(없는 키는 Enter로 건너뜀)
  python build_poi.py --include-all      ← 사진·설명이 없는 추가 관광지도 포함
  python build_poi.py --max-extra 500    ← 추가 관광지 최대 개수(품질 높은 순)
키 환경변수(선택): KAKAO_REST_KEY, VISITJEJU_API_KEY, TOUR_API_KEY, NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
"""
import argparse
import csv
import difflib
import getpass
import html
import json
import math
import os
import re
import sys
import time

try:
    import requests
except ImportError:
    requests = None

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "output")
AI_CSV = os.path.join(HERE, "jeju_place_candidates_18slot.csv")
OVERRIDE = os.path.join(HERE, "poi_overrides.csv")
EXTRA_ID_START = 1001

# ============================================================== 규칙
REGION_ID = {"EAST": 1, "WEST": 2, "SOUTH": 3, "NORTH": 4}
WEST_EUP = {"애월읍", "한림읍", "한경면", "대정읍", "안덕면"}
EAST_EUP = {"구좌읍", "조천읍", "우도면", "성산읍", "표선면"}
SOUTH_EUP = {"남원읍"}
NORTH_EUP = {"추자면"}


def region_of(gungu, eup):
    """AI/FastAPI와 같은 규칙: 서부 5읍면 / 동부 5읍면 / 남부 = 서귀포 동 + 남원 / 북부 = 제주시 동"""
    if eup in WEST_EUP:
        return "WEST"
    if eup in EAST_EUP:
        return "EAST"
    if eup in SOUTH_EUP:
        return "SOUTH"
    if eup in NORTH_EUP:
        return "NORTH"
    if eup.endswith("동") or eup.endswith("가"):
        return "SOUTH" if gungu == "서귀포시" else "NORTH"
    return None


# VISIT_AREA_TYPE_CD → category_code (팀 확정표, 13 → 9)
TYPE_TO_CAT = {"1": "NATURE", "2": "HISTORY", "3": "CULTURE", "4": "COMMERCIAL", "5": "LEISURE",
               "6": "THEME", "7": "TRAIL", "8": "FESTIVAL", "9": "EXPERIENCE", "13": "EXPERIENCE"}
CAT_TO_TYPE = {c: t for t, c in TYPE_TO_CAT.items()}   # 13(체험)→EXPERIENCE 역방향도 13
CAT_KO = {"NATURE": "자연관광지", "HISTORY": "역사·유적·종교 시설", "CULTURE": "문화시설",
          "COMMERCIAL": "상업지구", "LEISURE": "레저·스포츠 관련 시설", "THEME": "테마시설",
          "TRAIL": "산책로·둘레길", "FESTIVAL": "지역 축제·행사", "EXPERIENCE": "체험 활동 관광지"}

# 추가 관광지 분류: 이름 + 출처 분류 글자에서 아래 순서대로 먼저 맞는 것 (필요하면 단어 추가)
CAT_RULES = [
    ("EXPERIENCE", r"체험|농장|목장|승마|감귤따기|귤따기|공방|만들기|카약|요트|잠수함|유람선|투어|낚시체험|해녀체험|트랙터"),
    ("THEME", r"테마파크|테마공원|랜드|월드|아쿠아|동물원|미로|파크(?!골프)|아일랜드|워터|키즈|정원|가든"),
    ("LEISURE", r"레포츠|카트|서핑|골프|패러|스쿠버|다이빙|레일바이크|짚라인|집라인|수영장|경기장|사격|번지|ATV|스포츠"),
    ("CULTURE", r"박물관|미술관|갤러리|기념관|전시|문화|공연|아트|뮤지엄|도서관|예술|영화|전시관|문학관"),
    ("HISTORY", r"유적|사적|향교|읍성|관아|성지|순교|성당|교회|4\.3|항몽|진성|돌하르방|비석|사찰|[가-힣]{1,4}사$|절$|암자|문화재|옛터|방사탑|봉수대|연대$"),
    ("TRAIL", r"올레|둘레길|산책로|숲길|해안도로|탐방로|트레킹|길$|코스"),
    ("COMMERCIAL", r"시장|면세|쇼핑|아울렛|마트|몰$|거리$|상점가|카페거리|특산"),
    ("NATURE", r"오름|해변|해수욕장|폭포|숲|계곡|동굴|굴$|봉$|산$|섬$|도$|곶자왈|포구|등대|바위|해안|습지|수목원|전망대|주상절리|용천수|휴양림|공원|호수|저수지|못$|코지"),
]
# 출처가 알려주는 종류 → 분류(이름 규칙보다 우선)
TOUR_TYPE_CAT = {"14": "CULTURE", "28": "LEISURE", "38": "COMMERCIAL", "15": "FESTIVAL"}
VJ_CAT_CAT = {"c2": "COMMERCIAL", "c5": "FESTIVAL"}


def guess_category(name, hints="", tour_type=None, vj_cd=None):
    if vj_cd in VJ_CAT_CAT:
        return VJ_CAT_CAT[vj_cd]
    if tour_type in TOUR_TYPE_CAT:
        return TOUR_TYPE_CAT[tour_type]
    for cat, pat in CAT_RULES:
        if re.search(pat, name) or (hints and re.search(pat, hints)):
            return cat
    return "NATURE"


CITY_CENTER = {"제주시": (33.4996, 126.5312), "서귀포시": (33.2541, 126.5601)}
JEJU_BOX = (33.10, 34.00, 126.10, 127.00)   # 위도·경도 범위(추자도 포함)

KAKAO = "https://dapi.kakao.com/v2/local"
TOUR = "https://apis.data.go.kr/B551011/KorService2"
TOUR_COMMON = {"MobileOS": "ETC", "MobileApp": "Jejuro", "_type": "json"}
VISITJEJU = "https://api.visitjeju.net/vsjApi/contents/searchList"
NAVER_LOCAL = "https://openapi.naver.com/v1/search/local.json"


# ============================================================== 도구
def norm(s):
    s = re.sub(r"\(.*?\)|\[.*?\]", "", s or "")
    s = re.sub(r"<[^>]+>", "", s)
    return re.sub(r"[^0-9A-Za-z가-힣]", "", s).lower()


def sim(a, b):
    a, b = norm(a), norm(b)
    if not a or not b:
        return 0.0
    if a == b:
        return 1.0
    if a in b or b in a:
        return max(0.85, difflib.SequenceMatcher(None, a, b).ratio())
    return difflib.SequenceMatcher(None, a, b).ratio()


def dist_m(lat1, lng1, lat2, lng2):
    if None in (lat1, lng1, lat2, lng2):
        return 1e9
    r = 6371000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lng2 - lng1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def in_jeju(lat, lng):
    return lat is not None and JEJU_BOX[0] <= lat <= JEJU_BOX[1] and JEJU_BOX[2] <= lng <= JEJU_BOX[3]


def to_float(v):
    try:
        f = float(v)
        return f if f != 0 else None
    except (TypeError, ValueError):
        return None


def clean_text(s):
    s = html.unescape(re.sub(r"<[^>]+>", " ", s or ""))
    return re.sub(r"\s+", " ", s).strip()


def full_text(s, limit=4000):
    """소개글 전체(세부 설명): <br>·문단은 줄바꿈으로 살리고 태그 제거"""
    s = re.sub(r"(?i)<br\s*/?>|</p>", "\n", s or "")
    s = html.unescape(re.sub(r"<[^>]+>", "", s))
    s = re.sub(r"[ \t\u00a0]+", " ", s)
    s = re.sub(r"\n\s*\n+", "\n\n", s).strip()
    return s[:limit].rstrip() + ("…" if len(s) > limit else "")


def one_line(s, limit=80):
    """긴 소개글 → 첫 문장 한 줄(최대 limit자)"""
    s = clean_text(s)
    if not s:
        return ""
    m = re.search(r"^(.+?(?:다\.|요\.|[.!?])(?=\s|$))", s)
    first = m.group(1) if m else s
    if len(first) > limit:
        first = first[: limit - 1].rstrip(" ,·") + "…"
    return first


def sql_str(v):
    if v is None:
        return "NULL"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def https(url):
    return (url or "").strip().replace("http://", "https://", 1)


class Cache:
    def __init__(self, path):
        self.path = path
        try:
            with open(path, encoding="utf-8") as f:
                self.data = json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            self.data = {}
        self.dirty = 0

    def get(self, k):
        return self.data.get(k)

    def put(self, k, v):
        self.data[k] = v
        self.dirty += 1
        if self.dirty >= 20:
            self.save()

    def save(self):
        with open(self.path, "w", encoding="utf-8") as f:
            json.dump(self.data, f, ensure_ascii=False)
        self.dirty = 0


def http_get(cache, key, url, params=None, headers=None, sleep=0.05):
    hit = cache.get(key)
    if hit is not None:
        return hit
    r = requests.get(url, params=params, headers=headers, timeout=20)
    if r.status_code in (401, 403):
        raise PermissionError(f"{url} → {r.status_code} (키를 확인하세요): {r.text[:150]}")
    r.raise_for_status()
    data = r.json() if "json" in r.headers.get("Content-Type", "") or r.text.strip().startswith(("{", "[")) else {"_text": r.text}
    cache.put(key, data)
    time.sleep(sleep)
    return data


# ============================================================== 출처별 수집
class Kakao:
    def __init__(self, key, cache):
        self.h = {"Authorization": f"KakaoAK {key}"}
        self.cache = cache

    def keyword(self, q):
        d = http_get(self.cache, "kakao:kw:" + q, f"{KAKAO}/search/keyword.json", {"query": q, "size": 15}, self.h)
        return d.get("documents", [])

    def region(self, lat, lng):
        """좌표 → (시, 읍면동) 행정동 기준"""
        k = f"kakao:rg:{lat:.5f},{lng:.5f}"
        d = http_get(self.cache, k, f"{KAKAO}/geo/coord2regioncode.json", {"x": lng, "y": lat}, self.h)
        docs = d.get("documents", [])
        doc = next((x for x in docs if x.get("region_type") == "H"), docs[0] if docs else None)
        if not doc:
            return None, None
        return doc.get("region_2depth_name", ""), doc.get("region_3depth_name", "")

    def address(self, lat, lng):
        """좌표 → 주소(도로명 우선)"""
        d = http_get(self.cache, f"kakao:ad:{lat:.5f},{lng:.5f}", f"{KAKAO}/geo/coord2address.json", {"x": lng, "y": lat}, self.h)
        docs = d.get("documents", [])
        if not docs:
            return ""
        doc = docs[0]
        return ((doc.get("road_address") or {}).get("address_name") or (doc.get("address") or {}).get("address_name") or "")

    def find(self, name, gungu="", eup=""):
        best, score = None, 0.0
        for q in (name, f"제주 {name}", f"{gungu} {eup} {name}".strip()):
            for d in self.keyword(q):
                addr = d.get("address_name", "")
                if not addr.startswith("제주"):
                    continue
                s = sim(name, d.get("place_name", ""))
                if eup and eup in addr:
                    s += 0.15
                elif gungu and gungu in addr:
                    s += 0.05
                if s > score:
                    best, score = d, s
            if score >= 1.0:
                break
        return best, round(min(score, 1.15), 2)


class Naver:
    def __init__(self, cid, secret, cache):
        self.h = {"X-Naver-Client-Id": cid, "X-Naver-Client-Secret": secret}
        self.cache = cache

    @staticmethod
    def _coord(v):
        f = to_float(v)
        if f is None:
            return None
        return f / 1e7 if f > 1000 else f   # 2023년 이후 WGS84 × 10^7

    def find(self, name):
        q = f"제주 {name}"
        d = http_get(self.cache, "naver:" + q, NAVER_LOCAL, {"query": q, "display": 5}, self.h, sleep=0.12)
        best, score = None, 0.0
        for it in d.get("items", []):
            addr = it.get("address", "") or it.get("roadAddress", "")
            if "제주" not in addr:
                continue
            s = sim(name, it.get("title", ""))
            if s > score:
                best, score = it, s
        if best:
            best = dict(best, lat=self._coord(best.get("mapy")), lng=self._coord(best.get("mapx")),
                        title=clean_text(best.get("title")))
        return best, round(score, 2)


def load_visitjeju(key, cache, categories=("c1", "c2")):
    """비짓제주 전체 목록 (c1 관광지, c2 쇼핑). 숙박 c3·음식점 c4·축제 c5는 제외"""
    items = []
    for cat in categories:
        page, pages = 1, 1
        while page <= pages:
            d = http_get(cache, f"vj:{cat}:{page}", VISITJEJU,
                         {"apiKey": key, "locale": "kr", "category": cat, "page": page}, sleep=0.1)
            if str(d.get("result", "00")) not in ("00", "0", "None") and not d.get("items"):
                raise PermissionError(f"비짓제주 응답 오류: {str(d)[:200]}")
            size = int(d.get("pageSize") or d.get("resultCount") or 100) or 100
            pages = int(d.get("pageCount") or 0) or max(1, math.ceil(int(d.get("totalCount") or 0) / size))
            for it in d.get("items") or []:
                photo = ((it.get("repPhoto") or {}).get("photoid") or {})
                items.append({
                    "src": "VJ", "id": it.get("contentsid"), "name": clean_text(it.get("title")),
                    "lat": to_float(it.get("latitude")), "lng": to_float(it.get("longitude")),
                    "address": it.get("roadaddress") or it.get("address") or "",
                    "image": https(photo.get("imgpath") or photo.get("thumbnailpath")),
                    "desc": one_line(it.get("introduction")),
                    "detail": full_text(it.get("introduction")),
                    "hints": f"{it.get('tag') or ''} {it.get('alltag') or ''}",
                    "vj_cd": ((it.get("contentscd") or {}).get("value")),
                    "phone": (it.get("phoneno") or "").strip(),
                })
            print(f"  비짓제주 {cat} {page}/{pages}쪽")
            page += 1
    return items


def load_tour(key, cache, types=("12", "14", "28", "38")):
    """TourAPI 제주 목록 (12 관광지, 14 문화시설, 28 레포츠, 38 쇼핑). 제주 = lDongRegnCd 50"""
    items = []
    for t in types:
        page, total = 1, None
        while True:
            d = http_get(cache, f"tour:list:{t}:{page}", f"{TOUR}/areaBasedList2",
                         dict(TOUR_COMMON, serviceKey=key, lDongRegnCd=50, contentTypeId=t,
                              numOfRows=100, pageNo=page, arrange="A"))
            text = d.get("_text", "")
            if "SERVICE_KEY_IS_NOT_REGISTERED" in text:
                raise PermissionError("TourAPI 키가 아직 등록 안 됨(1~2시간 뒤) 또는 Decoding 키가 아님")
            if "LIMITED_NUMBER_OF_SERVICE_REQUESTS" in text:
                raise PermissionError("TourAPI 오늘 호출 한도 초과 → 내일 다시 실행(캐시로 이어짐)")
            body = (d.get("response") or {}).get("body") or {}
            total = int(body.get("totalCount") or 0)
            got = body.get("items") or {}
            got = got.get("item", []) if isinstance(got, dict) else []
            if isinstance(got, dict):
                got = [got]
            for it in got:
                items.append({
                    "src": "TOUR", "id": str(it.get("contentid")), "name": clean_text(it.get("title")),
                    "lat": to_float(it.get("mapy")), "lng": to_float(it.get("mapx")),
                    "address": f"{it.get('addr1') or ''} {it.get('addr2') or ''}".strip(),
                    "image": https(it.get("firstimage")), "desc": "", "detail": "",
                    "hints": " ".join(str(it.get(k) or "") for k in ("lclsSystm1", "lclsSystm2", "lclsSystm3", "cat3")),
                    "tour_type": str(it.get("contenttypeid") or t),
                    "phone": (it.get("tel") or "").strip(),
                })
            print(f"  TourAPI 종류 {t} {page}쪽 (총 {total}건)")
            if page * 100 >= total or not got:
                break
            page += 1
    return items


def tour_overview(key, cache, content_id):
    """TourAPI 소개글 전체(세부 설명용). 한 줄 소개는 one_line()으로 첫 문장만"""
    d = http_get(cache, f"tour:ov:{content_id}", f"{TOUR}/detailCommon2",
                 dict(TOUR_COMMON, serviceKey=key, contentId=content_id))
    body = (d.get("response") or {}).get("body") or {}
    got = body.get("items") or {}
    got = got.get("item", []) if isinstance(got, dict) else []
    if isinstance(got, dict):
        got = [got]
    return full_text(got[0].get("overview", "")) if got else ""


def best_match(name, lat, lng, catalog, max_m=1500, min_sim=0.6):
    """이름이 비슷하고 가까운 항목. 좌표가 없으면 이름만(0.85 이상)"""
    best, bscore = None, 0.0
    for it in catalog:
        s = sim(name, it["name"])
        if s < min_sim:
            continue
        d = dist_m(lat, lng, it["lat"], it["lng"])
        if lat is not None and it["lat"] is not None:
            if d > max_m and not (s >= 0.99 and d <= 20000):   # 이름이 완전히 같으면 20km까지 허용
                continue
            s += 0.1 * max(0.0, 1 - d / max_m)
        elif s < 0.85:
            continue
        if s > bscore:
            best, bscore = it, s
    return best, round(bscore, 2)


# ============================================================== 메인
def read_csv(path):
    for enc in ("utf-8-sig", "cp949"):
        try:
            with open(path, encoding=enc, newline="") as f:
                return list(csv.DictReader(f))
        except UnicodeDecodeError:
            continue
    sys.exit(f"CSV를 읽을 수 없습니다: {path}")


def read_overrides():
    cols = ["이름", "latitude", "longitude", "address", "image_url", "description", "detail_description", "category", "exclude"]
    if not os.path.exists(OVERRIDE):
        with open(OVERRIDE, "w", encoding="utf-8-sig", newline="") as f:
            csv.writer(f).writerow(cols)
        return {}
    out = {}
    for r in read_csv(OVERRIDE):
        n = (r.get("이름") or r.get("VISIT_AREA_NM") or r.get("name") or "").strip()
        if n:
            out[n] = r
    return out


def ask(env, label, secret=True):
    v = os.environ.get(env)
    if v:
        return v.strip()
    return (getpass.getpass(label) if secret else input(label)).strip()


# ============================================================== 기준 목록 정리 규칙
# 관광지가 아닌 곳(숙박·가게·기관·주거 등) → 자동 제외. poi_overrides.csv에서 exclude=N 이면 다시 포함
EXCLUDE_PAT = (r"펜션|게스트하우스|민박|호텔|콘도|리조트|풀빌라|POOLVILLA|빌라|아파트|오피스텔|빌딩|빌리지$|\d+동(?![가-힣])|[A-Z]동$|본관|모텔|스테이$"
               r"|마트|편의점|다이소|약국|의원|병원|은행|신협|농협|카센타|카센터|렌트카|학원|초교|중고$|학교|유치원|대학|어린이집"
               r"|장례식장|묘지|납골|묘$|부녀회|복지회관|관리실|주차|출장소|사우나|식당|횟집|해장국|뷔페|고기|베이커리|가스$|부품|가구"
               r"|어린이공원|장례|추모|공원묘|센타$|오피스|지점$|대행신고소|수련원|인재개발원|교육관|클럽하우스")
REVIEW_PAT = r"도서관|안내소|안내센터|체육|운동장|테니스|교회|골프|센터$|농수산|직판장|위판장|휴게소|스튜디오|갤러리$"
AI_COUNT = 275
JEJU_ADDR = re.compile(r"\s*(제주특별자치도|제주도|제주시|서귀포시|제주\s)")
NEW_ID_START = 2001      # 비짓제주·TourAPI에서 새로 추가하는 관광지 번호


def junk_reason(name):
    m = re.search(EXCLUDE_PAT, name)
    if m:
        return f"관광지 아님 추정('{m.group(0)}')"
    if name.endswith("수산") and "시장" not in name:
        return "관광지 아님 추정('수산' 가게)"
    return ""


def rule_category(name, hints=""):
    for cat, pat in CAT_RULES:
        if re.search(pat, name) or (hints and re.search(pat, hints)):
            return cat
    return None


def pick_category(name, hints="", tour_type=None, vj_cd=None, fallback=None):
    """출처 종류 → 이름 규칙 → 기준 목록에 있던 분류 → NATURE"""
    if vj_cd in VJ_CAT_CAT:
        return VJ_CAT_CAT[vj_cd]
    if tour_type in TOUR_TYPE_CAT:
        return TOUR_TYPE_CAT[tour_type]
    return rule_category(name, hints) or (fallback if fallback in CAT_KO else None) or "NATURE"


def fill_overview(p, tour_key, cache, n_ov, limit):
    """TourAPI 소개글 전체 → 세부 설명·한 줄 소개가 비어 있을 때 채움"""
    if not p.get("tour_id") or not tour_key or (p.get("detail") and p.get("desc")):
        return
    if n_ov[0] >= limit:
        p["pending_detail"] = True
        return
    try:
        text = tour_overview(tour_key, cache, p["tour_id"])
    except PermissionError as e:
        print("  !", e)
        n_ov[0] = limit
        p["pending_detail"] = True
        return
    n_ov[0] += 1
    if text:
        p["detail"] = p.get("detail") or text
        p["desc"] = p.get("desc") or one_line(text)


# ============================================================== 메인
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default=os.path.join(HERE, "poi_base.csv"), help="팀이 만든 기준 관광지 목록 CSV")
    ap.add_argument("--no-catalog-extra", action="store_true", help="비짓제주·TourAPI에서 새 관광지를 추가하지 않음")
    ap.add_argument("--include-all", action="store_true", help="사진·설명 없는 새 관광지도 추가")
    ap.add_argument("--max-extra", type=int, default=0, help="새로 추가할 관광지 최대 개수(0 = 제한 없음)")
    ap.add_argument("--max-overview", type=int, default=900, help="TourAPI 소개글 조회 최대 횟수(하루 한도 보호)")
    args = ap.parse_args()
    if requests is None:
        sys.exit("먼저 설치: python -m pip install requests")
    os.makedirs(OUT, exist_ok=True)
    cache = Cache(os.path.join(OUT, "api_cache.json"))
    ov = read_overrides()

    print("키를 입력하세요(화면에 안 보이는 게 정상, 없으면 Enter로 건너뜀)")
    kakao_key = ask("KAKAO_REST_KEY", "카카오 REST API 키(필수): ")
    if not kakao_key:
        sys.exit("카카오 REST 키는 필수입니다(좌표·주소·권역).")
    vj_key = ask("VISITJEJU_API_KEY", "비짓제주 API 키: ")
    tour_key = ask("TOUR_API_KEY", "TourAPI Decoding 키: ")
    nv_id = ask("NAVER_CLIENT_ID", "네이버 Client ID: ", secret=False)
    nv_secret = ask("NAVER_CLIENT_SECRET", "네이버 Client Secret: ") if nv_id else ""
    kakao = Kakao(kakao_key, cache)
    naver = Naver(nv_id, nv_secret, cache) if nv_id and nv_secret else None

    # ---------- 1) 비짓제주·TourAPI 목록
    catalog = []
    try:
        if vj_key:
            print("[1/7] 비짓제주 목록…")
            catalog += load_visitjeju(vj_key, cache)
        if tour_key:
            print("[1/7] TourAPI 목록…")
            catalog += load_tour(tour_key, cache)
    except PermissionError as e:
        cache.save()
        sys.exit(str(e))
    catalog = [c for c in catalog if c["name"] and in_jeju(c["lat"], c["lng"])]
    vj_cat = [c for c in catalog if c["src"] == "VJ"]
    tour_cat = [c for c in catalog if c["src"] == "TOUR"]
    print(f"  비짓제주 {len(vj_cat)}건, TourAPI {len(tour_cat)}건")
    used = set()
    n_ov = [0]

    # ---------- 2) 기준 목록(팀이 만든 CSV) 읽기 + 정리
    pois, dropped = [], []
    if os.path.exists(args.base):
        print(f"[2/7] 기준 목록 정리: {os.path.basename(args.base)}")
        for r in read_csv(args.base):
            name = clean_text(r.get("poi_name") or r.get("name") or "")
            if not name:
                continue
            p = new_poi(int(r["poi_id"]), name, None, None, "", "", ai=False)
            p.update(lat=to_float(r.get("latitude")), lng=to_float(r.get("longitude")),
                     address=(r.get("address") or "").strip(),
                     base_cat=(r.get("category_code") or "").strip().upper(),
                     image=https(r.get("image_url") or ""),
                     detail=full_text(r.get("description") or ""))
            p["src"].append("BASE")
            o = ov.get(name) or {}
            forced_in = (o.get("exclude") or "").strip().upper() == "N"
            reason = "" if forced_in else junk_reason(name)
            if not reason and p["address"] and not JEJU_ADDR.match(p["address"]):
                reason = f"주소가 제주 아님({p['address'][:20]})"
            if not reason and not in_jeju(p["lat"], p["lng"]):
                reason = "좌표가 제주 밖"
            if reason:
                dropped.append((p, reason))
                continue
            pois.append(p)
        # 기준 목록 안의 중복(이름 비슷 + 700m 이내) → 설명이 긴 쪽만 남김
        pois.sort(key=lambda p: (-len(p["detail"]), p["poi_id"]))
        kept = []
        for p in pois:
            d = next((k for k in kept if sim(k["name"], p["name"]) >= 0.85
                      and dist_m(k["lat"], k["lng"], p["lat"], p["lng"]) < 700), None)
            if d:
                d["image"] = d["image"] or p["image"]
                dropped.append((p, f"중복 → {d['poi_id']}. {d['name']}에 합침"))
                continue
            for same in (k for k in kept if sim(k["name"], p["name"]) >= 0.99):
                far = dist_m(same["lat"], same["lng"], p["lat"], p["lng"])
                p["notes"].append(f"확인필요: 같은 이름이 {far/1000:.1f}km 떨어진 곳에 또 있음({same['poi_id']}번) → 틀린 쪽은 exclude=Y")
                same["notes"].append(f"확인필요: 같은 이름이 {far/1000:.1f}km 떨어진 곳에 또 있음({p['poi_id']}번) → 틀린 쪽은 exclude=Y")
            kept.append(p)
        pois = sorted(kept, key=lambda p: p["poi_id"])
        print(f"  기준 {len(pois) + len(dropped)}곳 → 사용 {len(pois)}곳, 제외·합침 {len(dropped)}곳")
    else:
        print(f"[2/7] 기준 목록 없음({os.path.basename(args.base)}) → AI 후보 + 비짓제주·TourAPI로만 만듭니다")

    # ---------- 3) AI 후보 275곳 연결 (기준 목록에 있으면 그 번호, 없으면 새 번호)
    print("[3/7] AI 후보 275곳 연결…")
    next_ai_id = max([EXTRA_ID_START - 1] + [p["poi_id"] for p in pois if p["poi_id"] < NEW_ID_START]) + 1
    for r in read_csv(AI_CSV):
        name = r["VISIT_AREA_NM"]
        gungu, eup = r["GUNGU"].strip(), r["EUPMYEON"].strip()
        cat = TYPE_TO_CAT.get(r["VISIT_AREA_TYPE_CD"].strip())
        if not cat:
            sys.exit(f"모르는 VISIT_AREA_TYPE_CD '{r['VISIT_AREA_TYPE_CD']}' ({name}) → TYPE_TO_CAT에 추가")
        lat = lng = None
        addr = ""
        doc, ks = kakao.find(name, gungu, eup)
        if doc and ks >= 0.6:
            lat, lng = float(doc["y"]), float(doc["x"])
            addr = doc.get("road_address_name") or doc.get("address_name")
        elif naver:
            it, ns = naver.find(name)
            if it and ns >= 0.6 and in_jeju(it["lat"], it["lng"]):
                lat, lng, addr = it["lat"], it["lng"], it.get("roadAddress") or it.get("address")
        target, best = None, 0.0
        exact = [p for p in pois if not p["ai"] and sim(name, p["name"]) >= 0.99]
        if exact:   # 이름이 완전히 같으면 거리와 상관없이 연결(같은 이름이 여럿이면 가장 가까운 곳)
            target = min(exact, key=lambda p: dist_m(lat, lng, p["lat"], p["lng"]))
            best = 1.0
        else:
            for p in pois:
                if p["ai"]:
                    continue
                s_ = sim(name, p["name"])
                if s_ >= 0.85 and dist_m(lat, lng, p["lat"], p["lng"]) < 1500 and s_ > best:
                    target, best = p, s_
        if target:
            target.update(ai=True, ai_name=name, category=cat, region=region_of(gungu, eup), gungu=gungu, eup=eup)
            target["src"].insert(0, "AI")
            if best < 0.99:
                target["notes"].append(f"AI 후보 '{name}'와 연결(이름 다름) → 확인")
        else:
            p = new_poi(next_ai_id, name, cat, region_of(gungu, eup), gungu, eup, ai=True)
            p.update(ai_name=name, lat=lat, lng=lng, address=addr or p["address"])
            if lat is None:
                p["notes"].append("AI 후보 위치 못 찾음")
            pois.append(p)
            next_ai_id += 1
    print(f"  AI 후보 중 기준 목록과 연결 {sum(1 for p in pois if p['ai'] and 'BASE' in p['src'])}곳, "
          f"새로 추가 {sum(1 for p in pois if p['ai'] and 'BASE' not in p['src'])}곳")

    # ---------- 4) 사진·한 줄 소개·세부 설명 채우기
    print("[4/7] 사진·설명 채우기(비짓제주·TourAPI)…")
    for i, p in enumerate(pois, 1):
        enrich(p, vj_cat, tour_cat, used)
        fill_overview(p, tour_key, cache, n_ov, args.max_overview)
        if i % 100 == 0:
            print(f"  {i}/{len(pois)}")

    # ---------- 5) 비짓제주·TourAPI에만 있는 관광지 추가
    if not args.no_catalog_extra and catalog:
        print("[5/7] 새 관광지 추가(비짓제주·TourAPI에만 있는 곳)…")
        merged = []
        for c in sorted(catalog, key=lambda x: (x["src"] != "VJ", x["name"])):
            if id(c) in used or junk_reason(c["name"]):
                continue
            if any(sim(c["name"], p["name"]) >= 0.85 and dist_m(c["lat"], c["lng"], p["lat"], p["lng"]) < 700 for p in pois):
                continue
            dup = next((m for m in merged if sim(m["name"], c["name"]) >= 0.85
                        and dist_m(m["lat"], m["lng"], c["lat"], c["lng"]) < 700), None)
            if dup:
                dup["src"].append(c["src"])
                dup["ids"].append(f"{c['src']}:{c['id']}")
                dup["image"] = dup["image"] or c["image"]
                dup["hints"] += " " + c.get("hints", "")
                dup["desc"] = dup["desc"] or c["desc"]
                dup["detail"] = dup.get("detail") or c.get("detail", "")
                dup["tour_id"] = dup.get("tour_id") or (c["id"] if c["src"] == "TOUR" else None)
                dup["tour_type"] = dup.get("tour_type") or c.get("tour_type")
                continue
            merged.append(dict(c, src=[c["src"]], ids=[f"{c['src']}:{c['id']}"],
                               tour_id=c["id"] if c["src"] == "TOUR" else None))
        for m in merged:
            fill_overview(m, tour_key, cache, n_ov, args.max_overview)
            m["quality"] = len(set(m["src"])) + bool(m["image"]) + bool(m["desc"] or m.get("detail"))
        if not args.include_all:
            merged = [m for m in merged if m["image"] and (m["desc"] or m.get("detail"))]
        merged.sort(key=lambda m: (-m["quality"], m["name"]))
        if args.max_extra:
            merged = merged[: args.max_extra]
        merged.sort(key=lambda m: m["name"])
        start_id = max([NEW_ID_START] + [p["poi_id"] + 1 for p in pois if p["poi_id"] >= NEW_ID_START])
        for n, m in enumerate(merged):
            p = new_poi(start_id + n, m["name"], None, None, "", "", ai=False)
            p.update(lat=m["lat"], lng=m["lng"], address=m["address"], image=m["image"], desc=m["desc"],
                     detail=m.get("detail", ""), src=list(dict.fromkeys(m["src"])), ids=m["ids"],
                     hints=m.get("hints", ""), tour_type=m.get("tour_type"), vj_cd=m.get("vj_cd"),
                     pending_detail=m.get("pending_detail"))
            pois.append(p)
        print(f"  새 관광지 {len(merged)}곳")
    pend = sum(1 for p in pois if p.get("pending_detail"))
    if pend:
        print(f"  ! TourAPI 소개글 {pend}곳은 하루 한도 때문에 다음 실행(내일 같은 명령)에서 채웁니다")

    # ---------- 6) 주소·권역·분류 정리
    print("[6/7] 주소·권역·분류 정리(카카오)…")
    for i, p in enumerate(pois, 1):
        if i % 50 == 0:
            print(f"  {i}/{len(pois)}")
        if p["lat"] is None:
            continue
        if not p["ai"]:
            gungu, eup = kakao.region(p["lat"], p["lng"])
            p["gungu"], p["eup"] = gungu or "", eup or ""
            p["region"] = region_of(p["gungu"], p["eup"]) or ("NORTH" if p["gungu"].startswith("제주") else "SOUTH")
        if not (p["address"] or "").startswith("제주"):
            a = kakao.address(p["lat"], p["lng"])
            if a:
                p["notes"].append("주소를 좌표로 다시 구함")
                p["address"] = a
        if not p["ai"]:
            old = p.get("base_cat")
            p["category"] = pick_category(p["name"], p.get("hints", ""), p.get("tour_type"), p.get("vj_cd"), fallback=old)
            if old and old in CAT_KO and old != p["category"]:
                p["cat_changed"] = f"{old}→{p['category']}"

    # ---------- 7) 손으로 고친 값 + 마무리
    print("[7/7] 마무리…")
    final = []
    for p in pois:
        o = ov.get(p["name"]) or ov.get(p.get("ai_name") or "")
        if o:
            if (o.get("exclude") or "").strip().upper() == "Y":
                if p["ai"]:
                    p["notes"].append("학습된 275곳은 제외할 수 없음(모델이 추천하는 이름)")
                else:
                    dropped.append((p, "수동 제외"))
                    continue
            if o.get("latitude") and o.get("longitude"):
                p["lat"], p["lng"] = float(o["latitude"]), float(o["longitude"])
                p["notes"] = [x for x in p["notes"] if "위치" not in x]
            for k, f in (("address", "address"), ("image_url", "image"), ("description", "desc"),
                         ("detail_description", "detail")):
                if (o.get(k) or "").strip():
                    p[f] = o[k].strip()
            if (o.get("category") or "").strip().upper() in CAT_KO:
                p["category"] = o["category"].strip().upper()
            p["notes"].append("수동 수정 반영")
        if p["lat"] is None:
            p["lat"], p["lng"] = CITY_CENTER.get(p["gungu"], CITY_CENTER["제주시"])
            p["notes"].append("위치 못 찾음 → 시청 좌표(수정 필요)")
        p["region"] = p["region"] or "NORTH"
        p["category"] = p["category"] or "NATURE"
        if not p["desc"] and p["detail"]:
            p["desc"] = one_line(p["detail"])
        if not p["desc"]:
            p["desc"] = f"{p['gungu']} {p['eup']}에 있는 {CAT_KO[p['category']]}입니다.".replace("  ", " ").strip()
            p["notes"].append("설명 기본 문구")
        if not p["detail"]:
            p["detail"] = p["desc"]
            p["notes"].append("세부설명 없음 → 한 줄 소개로 대체")
        if p.get("pending_detail"):
            p["notes"].append("세부설명 다음 실행에서 채움(TourAPI 한도)")
        if not p["image"]:
            p["notes"].append("사진 없음")
        if re.search(REVIEW_PAT, p["name"]):
            p["notes"].append("관광지인지 확인(도서관·교회·골프 등)")
        final.append(p)
    final.sort(key=lambda p: p["poi_id"])
    cache.save()

    assign_model_keys(final)
    write_sql(final)
    write_ai_candidates(final)
    write_list(final)
    write_dropped(dropped)
    write_html(final)
    ai = [p for p in final if p["ai"]]
    print("\n=== 결과 ===")
    print(f"전체 {len(final)}곳 (기준 목록 {sum('BASE' in p['src'] for p in final)}"
          f" / AI 후보만 {sum(p['ai'] and 'BASE' not in p['src'] for p in final)}"
          f" / 새 관광지 {sum(p['poi_id'] >= NEW_ID_START for p in final)})")
    print(f"AI 추천 후보 = 전체 {len(final)}곳 (학습된 곳 {len(ai)} + 새로 넣은 곳 {len(final) - len(ai)})"
          " → output/jeju_place_candidates_all.csv (AI 담당에게 전달)")
    print("권역: " + ", ".join(f"{k} {sum(p['region'] == k for p in final)}" for k in REGION_ID))
    print(f"제외·합침 {len(dropped)}곳 → output/poi_제외목록.csv (다시 넣으려면 poi_overrides.csv에 exclude=N)")
    print(f"사진 {sum(bool(p['image']) for p in final)} / 한 줄 소개 {sum('설명 기본 문구' not in p['notes'] for p in final)}"
          f" / 세부설명 {sum(not any('세부설명' in n for n in p['notes']) for p in final)}")
    cats = {}
    for p in final:
        cats[p["category"]] = cats.get(p["category"], 0) + 1
    print("분류:", cats, f"/ 기준 목록 분류가 바뀐 곳 {sum(1 for p in final if p.get('cat_changed'))}곳")
    print(f"확인 필요 {sum(1 for p in final if [n for n in p['notes'] if n != '수동 수정 반영'])}곳 → output/poi_확인필요.csv")
    if len(ai) != AI_COUNT:
        print(f"! 학습된 275곳 중 {len(ai)}곳만 연결됐습니다. 기준 목록 한 곳에 학습 장소 둘이 연결됐는지 확인하세요.")
    print(f"결과 폴더: {OUT}")


def new_poi(pid, name, cat, region, gungu, eup, ai):
    return dict(poi_id=pid, name=name, category=cat, region=region, gungu=gungu, eup=eup, ai=ai, ai_name=None,
                lat=None, lng=None, address=f"제주특별자치도 {gungu} {eup}".strip(), image="", desc="", detail="",
                src=["AI"] if ai else [], ids=[], notes=[], hints="", tour_type=None, vj_cd=None, base_cat=None)


def assign_model_keys(final):
    """모든 관광지에 AI 모델용 이름(place_name = 추천 연결 키)을 붙임. 학습된 275곳은 원래 이름 그대로"""
    used = set()
    for p in final:
        if p["ai"] and p.get("ai_name"):
            p["model_key"] = p["ai_name"]
            used.add(p["ai_name"])
    for p in final:
        if p.get("model_key"):
            continue
        key = p["name"]
        if key in used:
            key = f"{p['name']}({p['eup'] or p['gungu']})"
            p["notes"].append(f"AI용 이름이 겹쳐서 '{key}'로 등록")
        if key in used:
            key = f"{p['name']}#{p['poi_id']}"
        p["model_key"] = key
        used.add(key)


def write_ai_candidates(final):
    """AI 담당에게 줄 후보 파일: 기존 275곳 CSV와 같은 형식 + 전체 관광지.
    학습 안 된 곳의 _mean 값은 같은 분류(VISIT_AREA_TYPE_CD) 275곳의 평균으로 채움 → TRAINED=0"""
    rows = read_csv(AI_CSV)
    head = list(rows[0].keys())
    num_cols = [c for c in head if c.endswith("_mean")]
    by_name = {r["VISIT_AREA_NM"]: r for r in rows}
    sums = {}
    for r in rows:
        for t in (r["VISIT_AREA_TYPE_CD"].strip(), "*"):
            s = sums.setdefault(t, {"n": 0, **{c: 0.0 for c in num_cols}})
            s["n"] += 1
            for c in num_cols:
                s[c] += float(r[c] or 0)
    avg = {t: {c: s[c] / s["n"] for c in num_cols} for t, s in sums.items()}
    with open(os.path.join(OUT, "jeju_place_candidates_all.csv"), "w", encoding="utf-8-sig", newline="") as f:
        wr = csv.writer(f)
        wr.writerow(head + ["POI_ID", "TRAINED"])
        for p in final:
            orig = by_name.get(p["model_key"]) if p["ai"] else None
            if orig:
                wr.writerow([orig[c] for c in head] + [p["poi_id"], 1])
                continue
            tcd = CAT_TO_TYPE[p["category"]]
            a = avg.get(tcd) or avg["*"]
            vals = {"VISIT_AREA_NM": p["model_key"], "SIDO": "제주특별자치도", "GUNGU": p["gungu"],
                    "EUPMYEON": p["eup"], "VISIT_AREA_TYPE_CD": tcd, **{c: a[c] for c in num_cols}}
            wr.writerow([vals.get(c, "") for c in head] + [p["poi_id"], 0])


def write_dropped(dropped):
    with open(os.path.join(OUT, "poi_제외목록.csv"), "w", encoding="utf-8-sig", newline="") as f:
        wr = csv.writer(f)
        wr.writerow(["poi_id", "name", "사유", "address", "다시_넣으려면"])
        for p, why in sorted(dropped, key=lambda x: x[0]["poi_id"]):
            wr.writerow([p["poi_id"], p["name"], why, p["address"], "poi_overrides.csv에 이름 + exclude=N"])


def enrich(p, vj_cat, tour_cat, used):
    """AI 후보에 비짓제주·TourAPI의 사진·설명 붙이기"""
    vj, vs = best_match(p["name"], p["lat"], p["lng"], vj_cat)
    if vj:
        used.add(id(vj))
        p["src"].append("VJ")
        p["ids"].append(f"VJ:{vj['id']}")
        p["image"] = p["image"] or vj["image"]
        p["desc"] = p["desc"] or vj["desc"]
        p["detail"] = p["detail"] or vj.get("detail", "")
        if p["lat"] is None and vj["lat"] is not None:
            p["lat"], p["lng"] = vj["lat"], vj["lng"]
            p["address"] = vj["address"] or p["address"]
            p["notes"].append("비짓제주 좌표 사용")
        if vs < 0.8:
            p["notes"].append(f"비짓제주 이름 다름({vj['name']})")
    tr, ts = best_match(p["name"], p["lat"], p["lng"], tour_cat)
    if tr:
        used.add(id(tr))
        p["src"].append("TOUR")
        p["ids"].append(f"TOUR:{tr['id']}")
        p["image"] = p["image"] or tr["image"]
        p["tour_id"] = tr["id"]
        if p["lat"] is None and tr["lat"] is not None:
            p["lat"], p["lng"] = tr["lat"], tr["lng"]
            p["address"] = tr["address"] or p["address"]
            p["notes"].append("TourAPI 좌표 사용")
        if ts < 0.8:
            p["notes"].append(f"TourAPI 이름 다름({tr['name']})")


def write_sql(final):
    L = []
    w = L.append
    w("-- 제주 관광지 — build_poi.py 자동 생성")
    w("-- 번호: 기준 목록(poi_base.csv)의 poi_id 그대로 / 기준 목록에 없던 AI 후보 1001~ / 비짓제주·TourAPI 새 관광지 2001~")
    w("-- 전제: POI·POI_SOURCE_MAP이 비어 있는 새 DB (jeju_schema.sql → test_user.sql → 이 파일)")
    w("-- POI_SOURCE_MAP: 모든 관광지의 AI용 이름(place_name, 추천 연결 키) + 출처 ID(VJ:…, TOUR:…)")
    w("-- 사진·설명 출처: 비짓제주(제주관광공사), TourAPI(한국관광공사) — 화면에 출처 표시")
    w("SET NAMES utf8mb4;")
    w("START TRANSACTION;")
    w("")
    w("INSERT IGNORE INTO `CODE_GROUP` (`group_code`, `group_name`) VALUES ('POI_CAT', '관광지 분류');")
    w("INSERT IGNORE INTO `CODE` (`group_code`, `code_value`, `code_name`) VALUES")
    w(",\n".join(f"('POI_CAT', '{c}', '{k}')" for c, k in CAT_KO.items()) + ";")
    w("")
    cols = "(`poi_id`, `poi_name`, `address`, `latitude`, `longitude`, `category_code`, `region_id`, `description`, `detail_description`, `image_url`)"
    for i in range(0, len(final), 200):
        chunk = final[i:i + 200]
        w(f"INSERT INTO `POI` {cols} VALUES")
        w(",\n".join(
            f"({p['poi_id']}, {sql_str(p['name'])}, {sql_str(p['address'])}, {p['lat']:.7f}, {p['lng']:.7f}, "
            f"{sql_str(p['category'])}, {REGION_ID[p['region']]}, {sql_str(p['desc'])}, {sql_str(p['detail'])}, {sql_str(p['image'])})"
            for p in chunk) + ";")
        w("")
    maps, seen = [], set()
    for p in final:
        keys = [p["model_key"]] + p["ids"]
        for k in keys:
            if k and k not in seen:
                seen.add(k)
                maps.append(f"({sql_str(k)}, {p['poi_id']})")
    for i in range(0, len(maps), 500):
        w("INSERT INTO `POI_SOURCE_MAP` (`source_poi_id`, `poi_id`) VALUES")
        w(",\n".join(maps[i:i + 500]) + ";")
        w("")
    w("COMMIT;")
    w("")
    w("-- 확인")
    w("SELECT COUNT(*) AS all_poi FROM `POI`;")
    w("SELECT COUNT(DISTINCT m.`poi_id`) AS ai_poi FROM `POI_SOURCE_MAP` m WHERE m.`source_poi_id` NOT LIKE '%:%';   -- all_poi와 같아야 함(전체가 AI 후보)")
    w("SELECT r.`region_code`, COUNT(*) FROM `POI` p JOIN `REGION` r ON r.`region_id` = p.`region_id` GROUP BY r.`region_code`;")
    w("SELECT `category_code`, COUNT(*) FROM `POI` GROUP BY `category_code`;")
    with open(os.path.join(OUT, "poi_all.sql"), "w", encoding="utf-8") as f:
        f.write("\n".join(L) + "\n")


def write_list(final):
    head = ["poi_id", "name", "AI후보(모델이름)", "권역", "분류", "분류_한글", "분류변경", "읍면동", "latitude", "longitude",
            "address", "사진", "한줄설명", "세부설명", "출처", "확인사항", "카카오맵"]
    def row(p):
        return [p["poi_id"], p["name"], (p.get("ai_name") or "Y") if p["ai"] else "", p["region"], p["category"],
                CAT_KO[p["category"]], p.get("cat_changed") or "",
                f"{p['gungu']} {p['eup']}", round(p["lat"], 7), round(p["lng"], 7), p["address"], p["image"],
                p["desc"], p["detail"], "+".join(dict.fromkeys(p["src"])), "; ".join(p["notes"]),
                f"https://map.kakao.com/?q={p['name']}"]
    with open(os.path.join(OUT, "poi_목록.csv"), "w", encoding="utf-8-sig", newline="") as f:
        wr = csv.writer(f)
        wr.writerow(head)
        for p in final:
            wr.writerow(row(p))
    with open(os.path.join(OUT, "poi_확인필요.csv"), "w", encoding="utf-8-sig", newline="") as f:
        wr = csv.writer(f)
        wr.writerow(head)
        for p in final:
            if [n for n in p["notes"] if n != "수동 수정 반영"]:
                wr.writerow(row(p))


def write_html(final):
    data = [{"id": p["poi_id"], "n": p["name"], "a": p["lat"], "o": p["lng"], "r": p["region"],
             "c": CAT_KO[p["category"]], "i": p["image"], "d": p["desc"], "t": p["detail"], "ai": p["ai"],
             "s": "+".join(dict.fromkeys(p["src"])), "w": [n for n in p["notes"] if n != "수동 수정 반영"]} for p in final]
    colors = {"EAST": "#e4572e", "WEST": "#2e86de", "SOUTH": "#20a37a", "NORTH": "#8e44ad"}
    page = """<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>관광지 확인</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css">
<script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"></script>
<style>body{font-family:sans-serif;margin:0}#map{height:50vh}.bar{padding:10px 12px;display:flex;gap:12px;flex-wrap:wrap;align-items:center}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:10px;padding:0 12px 20px}
.c{border:1px solid #ddd;border-radius:8px;overflow:hidden;font-size:13px}.c img,.c .no{width:100%;height:115px;object-fit:cover;background:#eee;display:grid;place-items:center;color:#888}
.c div{padding:6px}.warn{outline:2px solid #e4572e}.n{color:#e4572e;font-size:12px}.ai{background:#fff3d6;padding:0 4px;border-radius:4px;font-size:11px}</style></head><body>
<div id="map"></div><div class="bar"><b id="cnt"></b>
<label><input type="checkbox" id="w"> 확인 필요만</label><label><input type="checkbox" id="ex"> 추가 관광지만</label>
<select id="cat"><option value="">모든 분류</option></select><input id="q" placeholder="이름 검색">
<span>색: <b style="color:#e4572e">동</b> <b style="color:#2e86de">서</b> <b style="color:#20a37a">남</b> <b style="color:#8e44ad">북</b></span></div>
<div class="grid" id="g"></div>
<script>const D=__DATA__,C=__COLORS__;const m=L.map('map').setView([33.38,126.55],10);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap'}).addTo(m);
D.forEach(p=>L.circleMarker([p.a,p.o],{radius:p.ai?6:4,color:C[p.r],fillOpacity:.8}).addTo(m).bindPopup(p.id+'. '+p.n+'<br>'+p.c));
const cs=[...new Set(D.map(p=>p.c))];cs.forEach(c=>{const o=document.createElement('option');o.value=o.textContent=c;cat.appendChild(o)});
function draw(){const L2=D.filter(p=>(!w.checked||p.w.length)&&(!ex.checked||!p.ai)&&(!cat.value||p.c===cat.value)&&(!q.value||p.n.includes(q.value)));
cnt.textContent=L2.length+'곳';g.innerHTML=L2.slice(0,600).map(p=>`<div class="c ${p.w.length?'warn':''}">${p.i?`<img src="${p.i}" loading="lazy">`:'<span class="no">사진 없음</span>'}
<div><b>${p.id}. ${p.n}</b> ${p.ai?'<span class="ai">AI</span>':''}<br>${p.r} · ${p.c} · <small>${p.s}</small><br><i>${p.d}</i><details><summary>세부설명</summary><div style="white-space:pre-line">${p.t}</div></details>
${p.w.length?`<div class="n">${p.w.join('<br>')}</div>`:''}</div></div>`).join('')}
[w,ex,cat,q].forEach(e=>e.oninput=draw);draw();</script></body></html>"""
    page = page.replace("__DATA__", json.dumps(data, ensure_ascii=False)).replace("__COLORS__", json.dumps(colors))
    with open(os.path.join(OUT, "poi_확인.html"), "w", encoding="utf-8") as f:
        f.write(page)


_CACHES = []
_orig_cache_init = Cache.__init__


def _track_init(self, path):
    _orig_cache_init(self, path)
    _CACHES.append(self)


Cache.__init__ = _track_init

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        for c in _CACHES:
            c.save()
        print("\n중단했습니다. 지금까지 받은 결과는 저장됐어요 → 다시 실행하면 이어서 빠르게 진행됩니다.")
