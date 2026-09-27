"""
제주 숙소 목록 만들기 → ACCOMMODATION 테이블 (루트 주변 숙소 안내 FR-26)

출처
  - 한국관광공사 TourAPI 숙박(contentTypeId=32)   → source_id 'TOUR:…'   (사진: 한국관광공사)
  - 비짓제주 숙박(category=c3)                      → source_id 'VJ:…'     (사진: 비짓제주)
  - (선택) 카카오 로컬 카테고리 AD5(숙박)           → source_id 'KAKAO:…'  --with-kakao 일 때만

실행 (tools 폴더에서)
  python build_lodging.py                 # TourAPI + 비짓제주
  python build_lodging.py --with-kakao    # 카카오까지 (숙소가 훨씬 많아짐, 카카오 REST 키 필요)

결과 (tools/output/)
  lodging.sql        → DB에 실행 (기존 ACCOMMODATION 행을 지우고 다시 넣음)
  lodging_목록.csv   → 확인용

키는 build_poi.py와 같음: 화면에서 입력(안 보임) 또는 환경변수
  TOUR_API_KEY, VISITJEJU_API_KEY, KAKAO_REST_KEY
"""
import argparse
import csv
import os
import re
import sys

import build_poi as B   # 같은 폴더의 build_poi.py (캐시·API 호출·이름 비교 재사용)

TYPE_KO = {"HOTEL": "호텔", "RESORT": "리조트·콘도", "PENSION": "펜션·풀빌라", "GUESTHOUSE": "게스트하우스·민박",
           "MOTEL": "모텔", "CAMPING": "캠핑·글램핑", "ETC": "기타 숙소"}

# 이름(+분류 힌트) 규칙. 위에서부터 먼저 맞는 것
TYPE_RULES = [
    ("CAMPING", r"캠핑|글램핑|카라반|캠프닉|오토캠"),
    ("RESORT", r"리조트|콘도|레지던스|resort|condo"),
    ("HOTEL", r"호텔|hotel"),
    ("MOTEL", r"모텔|motel|여관|여인숙"),
    ("GUESTHOUSE", r"게스트|게하|민박|호스텔|hostel|홈스테이|한옥"),
    ("PENSION", r"펜션|풀빌라|빌라|독채|코티지|별장|펜숀"),
]
WEAK_PENSION = r"스테이|하우스|house|stay"   # 이름에만 있고 다른 단서가 없을 때 펜션으로
# TourAPI 숙박 소분류(cat3)
TOUR_CAT3 = {"B02010100": "HOTEL", "B02010500": "RESORT", "B02010600": "GUESTHOUSE", "B02010700": "PENSION",
             "B02010900": "MOTEL", "B02011000": "GUESTHOUSE", "B02011100": "GUESTHOUSE", "B02011200": "GUESTHOUSE",
             "B02011300": "RESORT", "B02011600": "GUESTHOUSE"}

# 카카오 격자 검색 범위: 제주 본섬·우도 / 추자도
KAKAO_BOXES = [(126.14, 33.10, 127.00, 33.60), (126.25, 33.90, 126.40, 34.00)]
SRC_PRIORITY = {"TOUR": 0, "VJ": 1, "KAKAO": 2}


def lodging_type(name, hints="", cat3=""):
    if cat3 in TOUR_CAT3:
        return TOUR_CAT3[cat3]
    for text in (name, hints):
        for t, pat in TYPE_RULES:
            if text and re.search(pat, text, re.I):
                return t
    if re.search(WEAK_PENSION, name or "", re.I):
        return "PENSION"
    return "ETC"


def from_catalog(c):
    cat3 = next((h for h in (c.get("hints") or "").split() if h.startswith("B0201")), "")
    return {"src": c["src"], "id": str(c["id"]), "name": c["name"], "lat": c["lat"], "lng": c["lng"],
            "address": (c.get("address") or "").strip(), "phone": c.get("phone") or "",
            "image": c.get("image") or "", "type": lodging_type(c["name"], c.get("hints", ""), cat3)}


def kakao_rect(kakao, x1, y1, x2, y2, out, depth=0):
    """사각형 안 숙박(AD5). 한 번에 최대 45건이라 넘으면 4등분해서 다시 검색"""
    rect = f"{x1:.4f},{y1:.4f},{x2:.4f},{y2:.4f}"
    url = f"{B.KAKAO}/search/category.json"
    first = B.http_get(kakao.cache, f"kakao:ad5:{rect}:1", url,
                       {"category_group_code": "AD5", "rect": rect, "page": 1, "size": 15}, kakao.h)
    total = int((first.get("meta") or {}).get("total_count") or 0)
    if total > 45 and (x2 - x1) > 0.004:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        for bx in ((x1, y1, mx, my), (mx, y1, x2, my), (x1, my, mx, y2), (mx, my, x2, y2)):
            kakao_rect(kakao, *bx, out, depth + 1)
        return
    page, d = 1, first
    while True:
        for doc in d.get("documents") or []:
            out[doc["id"]] = doc
        if (d.get("meta") or {}).get("is_end", True) or page >= 3:
            break
        page += 1
        d = B.http_get(kakao.cache, f"kakao:ad5:{rect}:{page}", url,
                       {"category_group_code": "AD5", "rect": rect, "page": page, "size": 15}, kakao.h)


def load_kakao(kakao):
    docs = {}
    for i, box in enumerate(KAKAO_BOXES):
        x1, y1, x2, y2 = box
        step = 0.1
        xs = [x1 + step * k for k in range(int((x2 - x1) / step) + 1)]
        ys = [y1 + step * k for k in range(int((y2 - y1) / step) + 1)]
        for x in xs:
            for y in ys:
                kakao_rect(kakao, x, y, min(x + step, x2), min(y + step, y2), docs)
        print(f"  카카오 범위 {i + 1}/{len(KAKAO_BOXES)} → 누적 {len(docs)}건")
    items = []
    for doc in docs.values():
        items.append({"src": "KAKAO", "id": doc["id"], "name": B.clean_text(doc.get("place_name")),
                      "lat": B.to_float(doc.get("y")), "lng": B.to_float(doc.get("x")),
                      "address": doc.get("road_address_name") or doc.get("address_name") or "",
                      "phone": doc.get("phone") or "", "image": "",
                      "type": lodging_type(doc.get("place_name", ""), doc.get("category_name", ""))})
    return items


def merge(items):
    """같은 숙소(이름 비슷 + 200m 이내)를 하나로. 관광공사 > 비짓제주 > 카카오 순으로 대표를 정하고 빈 칸을 채움"""
    items.sort(key=lambda x: SRC_PRIORITY[x["src"]])
    kept, merged = [], 0
    grid = {}
    for it in items:
        cell = (round(it["lat"], 2), round(it["lng"], 2))
        near = [k for dx in (-1, 0, 1) for dy in (-1, 0, 1)
                for k in grid.get((round(cell[0] + dx * 0.01, 2), round(cell[1] + dy * 0.01, 2)), [])]
        dup = next((k for k in near if B.sim(k["name"], it["name"]) >= 0.85
                    and B.dist_m(k["lat"], k["lng"], it["lat"], it["lng"]) < 200), None)
        if dup:
            for f in ("address", "phone", "image"):
                dup[f] = dup[f] or it[f]
            if dup["type"] == "ETC":
                dup["type"] = it["type"]
            merged += 1
            continue
        kept.append(it)
        grid.setdefault(cell, []).append(it)
    return kept, merged


def write_sql(rows, path):
    L = ["-- 제주 숙소 — build_lodging.py 자동 생성 (루트 주변 숙소 안내 FR-26)",
         "-- 출처: 한국관광공사 TourAPI(TOUR:), 비짓제주(VJ:), 카카오 로컬(KAKAO:)",
         "-- 다시 실행해도 됨: 기존 숙소를 지우고 새로 넣음 (다른 테이블이 참조하지 않음)",
         "SET NAMES utf8mb4;", "START TRANSACTION;", "DELETE FROM `ACCOMMODATION`;", ""]
    cols = "(`source_id`, `name`, `accommodation_type`, `address`, `latitude`, `longitude`, `phone`, `image_url`)"
    for i in range(0, len(rows), 300):
        L.append(f"INSERT INTO `ACCOMMODATION` {cols} VALUES")
        L.append(",\n".join(
            f"({B.sql_str(r['src'] + ':' + r['id'])}, {B.sql_str(r['name'][:200])}, '{r['type']}', "
            f"{B.sql_str(r['address'][:500])}, {r['lat']:.7f}, {r['lng']:.7f}, "
            f"{B.sql_str(r['phone'][:50]) if r['phone'] else 'NULL'}, {B.sql_str(r['image']) if r['image'] else 'NULL'})"
            for r in rows[i:i + 300]) + ";")
        L.append("")
    L += ["COMMIT;", "", "-- 확인",
          "SELECT COUNT(*) FROM `ACCOMMODATION`;",
          "SELECT `accommodation_type`, COUNT(*) FROM `ACCOMMODATION` GROUP BY `accommodation_type`;"]
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(L) + "\n")


def write_csv(rows, path):
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["source_id", "name", "종류", "address", "latitude", "longitude", "phone", "image_url"])
        for r in rows:
            w.writerow([f"{r['src']}:{r['id']}", r["name"], TYPE_KO[r["type"]], r["address"],
                        round(r["lat"], 7), round(r["lng"], 7), r["phone"], r["image"]])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--with-kakao", action="store_true", help="카카오 숙박(AD5)까지 모으기")
    args = ap.parse_args()
    if B.requests is None:
        sys.exit("먼저 설치: python -m pip install requests")
    os.makedirs(B.OUT, exist_ok=True)
    cache = B.Cache(os.path.join(B.OUT, "api_cache.json"))

    print("키를 입력하세요(화면에 안 보이는 게 정상, 없으면 Enter)")
    tour_key = B.ask("TOUR_API_KEY", "TourAPI Decoding 키: ")
    vj_key = B.ask("VISITJEJU_API_KEY", "비짓제주 API 키: ")
    kakao_key = B.ask("KAKAO_REST_KEY", "카카오 REST API 키: ") if args.with_kakao else ""
    if not (tour_key or vj_key or kakao_key):
        sys.exit("키가 하나도 없습니다. TourAPI·비짓제주 키 중 하나 이상 필요합니다.")

    items = []
    try:
        if tour_key:
            print("[1/3] TourAPI 숙박…")
            items += [from_catalog(c) for c in B.load_tour(tour_key, cache, types=("32",))]
        if vj_key:
            print("[1/3] 비짓제주 숙박…")
            items += [from_catalog(c) for c in B.load_visitjeju(vj_key, cache, categories=("c3",))]
        if kakao_key:
            print("[1/3] 카카오 숙박(격자 검색, 처음엔 몇 분)…")
            items += load_kakao(B.Kakao(kakao_key, cache))
    except PermissionError as e:
        cache.save()
        sys.exit(str(e))
    cache.save()

    print("[2/3] 정리…")
    by_src = {}
    for it in items:
        by_src[it["src"]] = by_src.get(it["src"], 0) + 1
    ok, out_jeju = [], 0
    for it in items:
        if not it["name"] or not B.in_jeju(it["lat"], it["lng"]) or (it["address"] and not B.JEJU_ADDR.match(it["address"])):
            out_jeju += 1
            continue
        it["image"] = B.https(it["image"])
        ok.append(it)
    rows, merged = merge(ok)
    rows.sort(key=lambda r: (r["name"], r["src"]))

    print("[3/3] 파일 쓰기…")
    write_sql(rows, os.path.join(B.OUT, "lodging.sql"))
    write_csv(rows, os.path.join(B.OUT, "lodging_목록.csv"))

    types = {}
    for r in rows:
        types[TYPE_KO[r["type"]]] = types.get(TYPE_KO[r["type"]], 0) + 1
    print("\n=== 결과 ===")
    print("받은 숙소:", ", ".join(f"{k} {v}" for k, v in by_src.items()))
    print(f"제주 밖·좌표 없음 제외 {out_jeju} / 같은 숙소 합침 {merged}")
    print(f"최종 {len(rows)}곳 → output/lodging.sql")
    print("종류:", types)
    print(f"사진 있음 {sum(bool(r['image']) for r in rows)} / 전화 있음 {sum(bool(r['phone']) for r in rows)}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        for c in B._CACHES:
            c.save()
        print("\n중단했습니다. 받은 결과는 저장됐어요 → 다시 실행하면 이어서 진행됩니다.")
