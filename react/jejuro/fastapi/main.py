from typing import List, Literal
from difflib import SequenceMatcher
from pathlib import Path

import pandas as pd

from catboost import CatBoostRegressor
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


# ============================================================
# 1. FastAPI 앱
# ============================================================

app = FastAPI(
    title="Jejuro Recommendation API",
    version="3.0.0"
)


# ============================================================
# 2. 파일 경로
# ============================================================

BASE_DIR = Path(__file__).resolve().parent

MODEL_PATH = (
    BASE_DIR
    / "output"
    / "jejuro_catboost_18slot.cbm"
)

CANDIDATE_PATH = (
    BASE_DIR
    / "output"
    / "jeju_place_candidates_18slot.csv"
)


# ============================================================
# 3. 모델 / 후보 관광지 로딩
# ============================================================

model = CatBoostRegressor()

model.load_model(
    str(MODEL_PATH)
)


place_info_jeju = pd.read_csv(
    CANDIDATE_PATH,
    low_memory=False
)


# ============================================================
# 4. DB 코드 → 모델 값 변환
# ============================================================

# DB
# 1 = 남
# 2 = 여
#
# MODEL
# "남", "여"

GENDER_MAP = {
    1: "남",
    2: "여"
}


# DB
# 1 = 9세 이하
# 2 = 10대
# 3 = 20대
# 4 = 30대
# 5 = 40대
# 6 = 50대
# 7 = 60대
# 8 = 70세 이상
#
# 현재 모델 학습 데이터에는
# 20 / 30 / 40 / 50 / 60만 존재

AGE_GROUP_MAP = {
    1: 20,   # 9세 이하 → 20대로 계산
    2: 20,   # 10대     → 20대로 계산
    3: 20,
    4: 30,
    5: 40,
    6: 50,
    7: 60,
    8: 60    # 70세 이상 → 60대로 계산
}


def convert_gender(
    gender_code: int
):

    if gender_code not in GENDER_MAP:

        raise HTTPException(
            status_code=400,
            detail=(
                f"지원하지 않는 gender_code입니다: "
                f"{gender_code}"
            )
        )

    return GENDER_MAP[
        gender_code
    ]


def convert_age_group(
    age_group_code: int
):

    if age_group_code not in AGE_GROUP_MAP:

        raise HTTPException(
            status_code=400,
            detail=(
                "현재 모델은 본인 연령대 "
                "20~60대만 학습되어 있습니다. "
                f"입력 age_group_code: "
                f"{age_group_code}"
            )
        )

    return AGE_GROUP_MAP[
        age_group_code
    ]


def validate_income(
    income_code: int
):

    # 현재 학습 데이터:
    # 1 ~ 12

    if income_code < 1 or income_code > 12:

        raise HTTPException(
            status_code=400,
            detail=(
                "income_code는 현재 "
                "1~12 범위만 지원합니다."
            )
        )

    return income_code


# ============================================================
# 5. 제주 4권역 매핑
# ============================================================
#
# 주의:
# 원본 CSV에는 EAST/WEST/SOUTH/NORTH 구분이 없기 때문에
# 아래는 서비스용 매핑 규칙
#
# 총 52개 EUPMYEON을 모두 하나의 권역에 배정
# ============================================================

REGION_MAP = {

    # --------------------------------------------------------
    # 북부
    # 제주시 도심권
    # --------------------------------------------------------
    "NORTH": {

        "건입동",
        "노형동",
        "도두일동",
        "봉개동",

        "삼도2동",
        "삼도이동",

        "삼양이동",

        "아라일동",

        "연동",

        "오등동",
        "오라이동",

        "용강동",

        "용담1동",
        "용담2동",
        "용담삼동",
        "용담일동",

        "이도일동",

        "이호일동",

        "일도1동",
        "일도2동",
        "일도이동",
        "일도일동",

        "해안동"
    },


    # --------------------------------------------------------
    # 동부
    # --------------------------------------------------------
    "EAST": {

        "구좌읍",

        "우도면",

        "조천읍",

        "성산읍",

        "표선면"
    },


    # --------------------------------------------------------
    # 서부
    # --------------------------------------------------------
    "WEST": {

        "애월읍",

        "한경면",

        "한림읍",

        "대정읍",

        "안덕면"
    },


    # --------------------------------------------------------
    # 남부
    # 서귀포 도심 + 남원권
    # --------------------------------------------------------
    "SOUTH": {

        "남원읍",

        "대포동",

        "도순동",

        "동홍동",

        "법환동",

        "보목동",

        "상효동",

        "색달동",

        "서귀동",

        "서홍동",

        "신효동",

        "영남동",

        "중문동",

        "천지동",

        "토평동",

        "하예동",

        "하원동",

        "하효동",

        "호근동"
    }
}


# ============================================================
# 6. 권역 컬럼 생성
# ============================================================

def get_region_from_eupmyeon(
    eupmyeon
):

    eupmyeon = str(
        eupmyeon
    ).strip()

    for region, locations in REGION_MAP.items():

        if eupmyeon in locations:

            return region

    return "UNKNOWN"


place_info_jeju[
    "REGION"
] = (

    place_info_jeju[
        "EUPMYEON"
    ]

    .apply(
        get_region_from_eupmyeon
    )
)


# ============================================================
# 7. 권역 매핑 검증
# ============================================================

unknown_regions = (

    place_info_jeju[
        place_info_jeju[
            "REGION"
        ] == "UNKNOWN"
    ][
        "EUPMYEON"
    ]

    .drop_duplicates()

    .tolist()
)


if unknown_regions:

    print(
        "WARNING - 권역 미매핑:",
        unknown_regions
    )


# ============================================================
# 8. 유사 관광지 판별
# ============================================================

def is_similar_place(
    name1: str,
    name2: str,
    threshold: float = 0.65
):

    name1 = str(
        name1
    ).replace(
        " ",
        ""
    )

    name2 = str(
        name2
    ).replace(
        " ",
        ""
    )


    # --------------------------------------------------------
    # 대표 관광지 그룹
    # --------------------------------------------------------

    group_keywords = [

        "한라산",

        "성산일출봉",

        "우도",

        "섭지코지",

        "천지연폭포",

        "정방폭포",

        "협재해수욕장",

        "함덕해수욕장"
    ]


    for keyword in group_keywords:

        if (
            keyword in name1
            and
            keyword in name2
        ):

            return True


    # --------------------------------------------------------
    # 포함 관계
    # --------------------------------------------------------

    if (
        name1 in name2
        or
        name2 in name1
    ):

        return True


    # --------------------------------------------------------
    # 일반 문자열 유사도
    # --------------------------------------------------------

    similarity = SequenceMatcher(
        None,
        name1,
        name2
    ).ratio()


    return (
        similarity
        >=
        threshold
    )


# ============================================================
# 9. 동반자 slot 설정
# ============================================================

MAX_COMPANIONS = 18


companion_slot_cols = []


for i in range(
    1,
    MAX_COMPANIONS + 1
):

    companion_slot_cols.extend(
        [
            f"COMPANION_{i}_REL",

            f"COMPANION_{i}_GENDER",

            f"COMPANION_{i}_AGE"
        ]
    )


# ============================================================
# 10. 모델 Feature
# ============================================================

base_features = [

    "VISIT_AREA_NM",

    "SIDO",

    "GUNGU",

    "EUPMYEON",

    "VISIT_AREA_TYPE_CD",

    "TRAVEL_MISSION_PRIORITY_WEB",

    "GENDER",

    "AGE_GRP",

    "INCOME",

    "TRAVEL_STYL_1",

    "TRAVEL_STYL_3",

    "TRAVEL_STYL_5",

    "TRAVEL_STYL_6",

    "TRAVEL_STYL_7",

    "TRAVEL_STYL_8",

    "TRAVEL_MOTIVE_1",

    "TRAVEL_COMPANIONS_NUM",

    "RESIDENCE_TIME_MIN_mean",

    "RCMDTN_INTENTION_mean",

    "REVISIT_YN_mean",

    "TRAVEL_COMPANIONS_NUM_mean",

    "REVISIT_INTENTION_mean"
]


final_features = (

    base_features
    +
    companion_slot_cols
)


cat_features = [

    "VISIT_AREA_NM",

    "SIDO",

    "GUNGU",

    "EUPMYEON",

    "VISIT_AREA_TYPE_CD",

    "TRAVEL_MISSION_PRIORITY_WEB",

    "AGE_GRP",

    "GENDER",

    *companion_slot_cols
]


# ============================================================
# 11. 요청 DTO
# ============================================================

class CompanionRequest(
    BaseModel
):

    relation_code: int

    gender_code: int

    age_group_code: int


class RecommendRequest(
    BaseModel
):

    # --------------------------------------------------------
    # 본인 정보
    # DB 코드 그대로 전달
    # --------------------------------------------------------

    gender_code: int

    age_group_code: int

    income_code: int


    # --------------------------------------------------------
    # 여행 정보
    # --------------------------------------------------------

    TRAVEL_MISSION_PRIORITY_WEB: int


    # --------------------------------------------------------
    # 여행 스타일
    # --------------------------------------------------------

    TRAVEL_STYL_1: int

    TRAVEL_STYL_3: int

    TRAVEL_STYL_5: int

    TRAVEL_STYL_6: int

    TRAVEL_STYL_7: int

    TRAVEL_STYL_8: int


    # --------------------------------------------------------
    # 여행 동기
    # --------------------------------------------------------

    TRAVEL_MOTIVE_1: int


    # --------------------------------------------------------
    # 지역
    #
    # ALL
    # SELECTED
    # --------------------------------------------------------

    region_mode: Literal[
        "ALL",
        "SELECTED"
    ] = "ALL"


    # SELECTED일 경우
    # 예:
    #
    # ["EAST"]
    #
    # ["EAST", "SOUTH"]
    # --------------------------------------------------------

    regions: List[
        Literal[
            "EAST",
            "WEST",
            "SOUTH",
            "NORTH"
        ]
    ] = Field(
        default_factory=list
    )


    # --------------------------------------------------------
    # 동반자
    # --------------------------------------------------------

    companions: List[
        CompanionRequest
    ] = Field(
        default_factory=list
    )


    top_n: int = 10


# ============================================================
# 12. 동반자 → 18-slot
# ============================================================

def build_companion_slots(
    companions: List[
        CompanionRequest
    ]
):

    # --------------------------------------------------------
    # 최대 인원 검증
    # --------------------------------------------------------

    if (
        len(companions)
        >
        MAX_COMPANIONS
    ):

        raise HTTPException(
            status_code=400,
            detail=(
                f"동반자는 최대 "
                f"{MAX_COMPANIONS}명까지 "
                f"입력할 수 있습니다."
            )
        )


    # --------------------------------------------------------
    # 학습 때와 동일
    #
    # relation
    # → gender
    # → age
    #
    # 정렬
    # --------------------------------------------------------

    companions = sorted(

        companions,

        key=lambda x: (

            x.relation_code,

            x.gender_code,

            x.age_group_code
        )
    )


    slot_data = {}


    # --------------------------------------------------------
    # NONE 초기화
    # --------------------------------------------------------

    for i in range(
        1,
        MAX_COMPANIONS + 1
    ):

        slot_data[
            f"COMPANION_{i}_REL"
        ] = "NONE"

        slot_data[
            f"COMPANION_{i}_GENDER"
        ] = "NONE"

        slot_data[
            f"COMPANION_{i}_AGE"
        ] = "NONE"


    # --------------------------------------------------------
    # 실제 값
    # --------------------------------------------------------

    for i, companion in enumerate(
        companions,
        start=1
    ):

        slot_data[
            f"COMPANION_{i}_REL"
        ] = str(
            companion.relation_code
        )

        slot_data[
            f"COMPANION_{i}_GENDER"
        ] = str(
            companion.gender_code
        )

        slot_data[
            f"COMPANION_{i}_AGE"
        ] = str(
            companion.age_group_code
        )


    return slot_data


# ============================================================
# 13. 지역 필터 함수
# ============================================================

def filter_candidates_by_region(
    candidates: pd.DataFrame,
    region_mode: str,
    regions: List[str]
):

    # --------------------------------------------------------
    # ALL
    # 제주 전체
    # --------------------------------------------------------

    if region_mode == "ALL":

        return (
            candidates
            .copy()
            .reset_index(
                drop=True
            )
        )


    # --------------------------------------------------------
    # SELECTED인데 선택 지역 없음
    # --------------------------------------------------------

    if (
        region_mode == "SELECTED"
        and
        len(regions) == 0
    ):

        raise HTTPException(
            status_code=400,
            detail=(
                "region_mode가 SELECTED이면 "
                "regions를 1개 이상 선택해야 합니다."
            )
        )


    # --------------------------------------------------------
    # 여러 지역 선택 가능
    # --------------------------------------------------------

    filtered = (

        candidates[
            candidates[
                "REGION"
            ].isin(
                regions
            )
        ]

        .copy()

        .reset_index(
            drop=True
        )
    )


    if len(filtered) == 0:

        raise HTTPException(
            status_code=400,
            detail=(
                "선택한 지역에 추천 가능한 "
                "관광지가 없습니다."
            )
        )


    return filtered


# ============================================================
# 14. 응답 DTO
# ============================================================

class RecommendationItem(
    BaseModel
):

    rank: int

    place_name: str

    sido: str

    gungu: str

    eupmyeon: str

    region: str

    visit_area_type_cd: str

    score: float


class RecommendResponse(
    BaseModel
):

    recommendations: List[
        RecommendationItem
    ]


# ============================================================
# 15. 상태 확인
# ============================================================

@app.get("/")
def root():

    return {

        "message":
            "Jejuro Recommendation API",

        "model":
            "18-slot companion CatBoost",

        "candidate_count":
            len(
                place_info_jeju
            ),

        "feature_count":
            len(
                final_features
            ),

        "companion_slot_feature_count":
            len(
                companion_slot_cols
            ),

        "region_counts":
            place_info_jeju[
                "REGION"
            ].value_counts().to_dict()
    }


@app.get("/health")
def health():

    return {

        "status":
            "ok",

        "model_loaded":
            True,

        "candidate_count":
            len(
                place_info_jeju
            ),

        "feature_count":
            len(
                final_features
            ),

        "max_companions":
            MAX_COMPANIONS,

        "unknown_region_count":
            int(
                (
                    place_info_jeju[
                        "REGION"
                    ]
                    ==
                    "UNKNOWN"
                ).sum()
            )
    }


# ============================================================
# 16. 추천 API
# ============================================================

@app.post(
    "/recommend",
    response_model=RecommendResponse
)
def recommend(
    request: RecommendRequest
):

    # ========================================================
    # 1) 본인 DB 코드 → 모델 코드
    # ========================================================

    gender_model = (
        convert_gender(
            request.gender_code
        )
    )


    age_model = (
        convert_age_group(
            request.age_group_code
        )
    )


    income_model = (
        validate_income(
            request.income_code
        )
    )


    # ========================================================
    # 2) 제주 후보
    # ========================================================

    candidates = (

        place_info_jeju

        .copy()

        .reset_index(
            drop=True
        )
    )


    # ========================================================
    # 3) 지역 필터
    # ========================================================

    candidates = (
        filter_candidates_by_region(

            candidates,

            request.region_mode,

            request.regions
        )
    )


    # ========================================================
    # 4) 동반자 18-slot
    # ========================================================

    slot_data = (
        build_companion_slots(
            request.companions
        )
    )


    # ========================================================
    # 5) 사용자 feature
    # ========================================================

    user_features = {

        "TRAVEL_MISSION_PRIORITY_WEB":
            request.TRAVEL_MISSION_PRIORITY_WEB,

        "GENDER":
            gender_model,

        "AGE_GRP":
            age_model,

        "INCOME":
            income_model,

        "TRAVEL_STYL_1":
            request.TRAVEL_STYL_1,

        "TRAVEL_STYL_3":
            request.TRAVEL_STYL_3,

        "TRAVEL_STYL_5":
            request.TRAVEL_STYL_5,

        "TRAVEL_STYL_6":
            request.TRAVEL_STYL_6,

        "TRAVEL_STYL_7":
            request.TRAVEL_STYL_7,

        "TRAVEL_STYL_8":
            request.TRAVEL_STYL_8,

        "TRAVEL_MOTIVE_1":
            request.TRAVEL_MOTIVE_1,

        "TRAVEL_COMPANIONS_NUM":
            len(
                request.companions
            )
    }


    # --------------------------------------------------------
    # 동반자 slot 합치기
    # --------------------------------------------------------

    user_features.update(
        slot_data
    )


    # ========================================================
    # 6) 후보 관광지마다 사용자 feature 붙이기
    # ========================================================

    user_feature_df = pd.DataFrame(

        [
            user_features
        ]
        *
        len(
            candidates
        )
    )


    candidates = pd.concat(

        [

            candidates
            .reset_index(
                drop=True
            ),

            user_feature_df
            .reset_index(
                drop=True
            )
        ],

        axis=1
    )


    # ========================================================
    # 7) feature 누락 검사
    # ========================================================

    missing_features = [

        col

        for col in final_features

        if col not in candidates.columns
    ]


    if missing_features:

        raise HTTPException(
            status_code=500,
            detail={

                "message":
                    "모델 입력 feature가 누락되었습니다.",

                "missing_features":
                    missing_features
            }
        )


    # ========================================================
    # 8) 모델 입력
    # ========================================================

    X = (

        candidates[
            final_features
        ]

        .copy()
    )


    # ========================================================
    # 9) categorical → 문자열
    # ========================================================

    for col in cat_features:

        X[col] = (

            X[col]

            .astype(
                str
            )
        )


    # ========================================================
    # 10) CatBoost 예측
    # ========================================================

    try:

        candidates[
            "y_pred"
        ] = (
            model.predict(
                X
            )
        )

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=(
                "CatBoost 예측 중 오류: "
                f"{str(e)}"
            )
        )


    # ========================================================
    # 11) top_n
    # ========================================================

    top_n = max(

        1,

        min(

            request.top_n,

            len(
                candidates
            )
        )
    )


    # ========================================================
    # 12) 예측 점수 순 정렬
    # ========================================================

    sorted_candidates = (

        candidates

        .sort_values(
            "y_pred",
            ascending=False
        )

        .drop_duplicates(
            "VISIT_AREA_NM"
        )

        .head(
            50
        )

        .reset_index(
            drop=True
        )
    )


    # ========================================================
    # 13) 유사 관광지 제거
    # ========================================================

    selected_rows = []


    for _, row in (
        sorted_candidates
        .iterrows()
    ):

        current_name = (
            row[
                "VISIT_AREA_NM"
            ]
        )


        is_duplicate = False


        for selected in selected_rows:

            selected_name = (
                selected[
                    "VISIT_AREA_NM"
                ]
            )


            if is_similar_place(

                current_name,

                selected_name,

                threshold=0.65
            ):

                is_duplicate = True

                break


        if not is_duplicate:

            selected_rows.append(
                row
            )


        if (
            len(
                selected_rows
            )
            >=
            top_n
        ):

            break


    # ========================================================
    # 14) DataFrame 변환
    # ========================================================

    result_df = pd.DataFrame(
        selected_rows
    )


    # ========================================================
    # 15) 유사도 제거 때문에 부족하면 보충
    # ========================================================

    if (
        len(
            result_df
        )
        <
        top_n
    ):

        if len(
            result_df
        ) == 0:

            selected_names = set()

        else:

            selected_names = set(

                result_df[
                    "VISIT_AREA_NM"
                ].tolist()
            )


        for _, row in (
            sorted_candidates
            .iterrows()
        ):

            place_name = (
                row[
                    "VISIT_AREA_NM"
                ]
            )


            if (
                place_name
                in
                selected_names
            ):

                continue


            result_df = pd.concat(

                [

                    result_df,

                    pd.DataFrame(
                        [row]
                    )
                ],

                ignore_index=True
            )


            selected_names.add(
                place_name
            )


            if (
                len(
                    result_df
                )
                >=
                top_n
            ):

                break


    # ========================================================
    # 16) 최종 top_n
    # ========================================================

    result_df = (

        result_df

        .head(
            top_n
        )

        .reset_index(
            drop=True
        )
    )


    # ========================================================
    # 17) 응답
    # ========================================================

    result = []


    for index, row in (
        result_df
        .iterrows()
    ):

        result.append(

            RecommendationItem(

                rank=
                    index + 1,

                place_name=
                    str(
                        row[
                            "VISIT_AREA_NM"
                        ]
                    ),

                sido=
                    str(
                        row[
                            "SIDO"
                        ]
                    ),

                gungu=
                    str(
                        row[
                            "GUNGU"
                        ]
                    ),

                eupmyeon=
                    str(
                        row[
                            "EUPMYEON"
                        ]
                    ),

                region=
                    str(
                        row[
                            "REGION"
                        ]
                    ),

                visit_area_type_cd=
                    str(
                        row[
                            "VISIT_AREA_TYPE_CD"
                        ]
                    ),

                score=
                    float(
                        row[
                            "y_pred"
                        ]
                    )
            )
        )


    return RecommendResponse(
        recommendations=result
    )


# ============================================================
# 17. 실행 확인
# ============================================================

print(
    "MODEL EXISTS:",
    MODEL_PATH.exists()
)

print(
    "CANDIDATE EXISTS:",
    CANDIDATE_PATH.exists()
)

print(
    "FINAL FEATURES:",
    len(
        final_features
    )
)

print(
    "COMPANION SLOT FEATURES:",
    len(
        companion_slot_cols
    )
)

print()
print(
    "REGION COUNTS:"
)

print(
    place_info_jeju[
        "REGION"
    ].value_counts()
)

print()
print(
    "UNKNOWN REGIONS:",
    unknown_regions
)