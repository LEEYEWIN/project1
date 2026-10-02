package kr.fast.Jejuro.Repository;


//[6페이지 카카오맵 동선 - 주변 숙소 (FR-26)]

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.fast.Jejuro.Entity.Accommodation;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

 /** 반경 검색 결과 한 줄 (distanceM = 기준 지점에서 직선거리, 미터) */
 interface NearbyRow {
     Number getId();
     String getSourceId();
     String getName();
     String getType();
     String getAddress();
     Number getLatitude();
     Number getLongitude();
     String getPhone();
     String getImageUrl();
     Number getDistanceM();
 }

 /**
  * 기준 지점 주변 숙소를 가까운 순으로.
  * 1) 위도·경도 사각형으로 먼저 좁히고(색인 ix_accommodation_lat_lng 사용)
  * 2) ST_Distance_Sphere로 실제 거리(미터)를 계산해 반경 밖은 버린다.
  * 3) 주소가 다른 숙소와 좌표가 똑같은 숙소는 뺀다. 원본 데이터가 위치를 못 찾은 숙소에
  *    지역 대표 좌표를 넣어 둔 경우라(예: 한경면·애월읍 숙소 3곳이 같은 좌표), 거리·지도가 틀리게 안내된다.
  */
 @Query(value = """
         SELECT a.accommodation_id   AS id,
                a.source_id          AS sourceId,
                a.name               AS name,
                a.accommodation_type AS type,
                a.address            AS address,
                a.latitude           AS latitude,
                a.longitude          AS longitude,
                a.phone              AS phone,
                a.image_url          AS imageUrl,
                ST_Distance_Sphere(POINT(a.longitude, a.latitude), POINT(:lng, :lat)) AS distanceM
           FROM accommodation a
          WHERE a.latitude  BETWEEN :minLat AND :maxLat
            AND a.longitude BETWEEN :minLng AND :maxLng
            AND NOT EXISTS (SELECT 1 FROM accommodation d
                             WHERE d.latitude = a.latitude
                               AND d.longitude = a.longitude
                               AND d.address <> a.address)
         HAVING distanceM <= :radiusM
          ORDER BY distanceM
          LIMIT :size
         """, nativeQuery = true)
 List<NearbyRow> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                            @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                            @Param("radiusM") double radiusM, @Param("size") int size);
}