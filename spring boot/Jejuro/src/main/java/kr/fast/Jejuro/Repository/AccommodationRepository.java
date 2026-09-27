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
           FROM ACCOMMODATION a
          WHERE a.latitude  BETWEEN :minLat AND :maxLat
            AND a.longitude BETWEEN :minLng AND :maxLng
         HAVING distanceM <= :radiusM
          ORDER BY distanceM
          LIMIT :size
         """, nativeQuery = true)
 List<NearbyRow> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                            @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                            @Param("radiusM") double radiusM, @Param("size") int size);
}