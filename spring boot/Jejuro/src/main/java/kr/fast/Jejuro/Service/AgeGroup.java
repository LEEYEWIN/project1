package kr.fast.Jejuro.Service;


import java.time.LocalDate;
import java.time.Period;

/** 생년월일 → 연령대 코드(AGE 1~8). 기준일은 여행 시작일. */
public final class AgeGroup {

    private AgeGroup() {
    }

    public static int of(LocalDate birthDate, LocalDate baseDate) {
        int age = Period.between(birthDate, baseDate).getYears();
        if (age <= 9) return 1;       // 9세 이하
        if (age >= 70) return 8;      // 70세 이상
        return age / 10 + 1;          // 10대=2, 20대=3, ... 60대=7
    }
}