-- 로컬 테스트용 서울 스팟 시드 데이터
-- 실행: cat http/seed-spots.sql | docker compose exec -T postgres psql -U moodi -d moodi

INSERT INTO spot (id, content_id, content_type, area, district, longitude, latitude, source, route_excluded, status, created_at, updated_at)
VALUES
    (1,  '2733967', 'TOURIST_ATTRACTION', '서울', '종로구',   126.9846467509,     37.5820334711,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (2,  '2763807', 'TOURIST_ATTRACTION', '서울', '동대문구', 127.04906426645805, 37.57278096759384,  'TOUR_API', false, 'PUBLISHED', now(), now()),
    (3,  '3483083', 'TOURIST_ATTRACTION', '서울', '도봉구',   127.02818672762793, 37.664892967926406, 'TOUR_API', false, 'PUBLISHED', now(), now()),
    (4,  '1116925', 'TOURIST_ATTRACTION', '서울', '양천구',   126.8684105358,     37.5061176314,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (5,  '294439',  'TOURIST_ATTRACTION', '서울', '종로구',   127.00663769536024, 37.57532850974662,  'TOUR_API', false, 'PUBLISHED', now(), now()),
    (6,  '4080919', 'TOURIST_ATTRACTION', '서울', '종로구',   126.9764826207,     37.5733087047,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (7,  '264570',  'TOURIST_ATTRACTION', '서울', '강남구',   127.0270732739,     37.5268958352,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (8,  '2456536', 'TOURIST_ATTRACTION', '서울', '강남구',   127.059217995,      37.5119175967,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (9,  '127377',  'TOURIST_ATTRACTION', '서울', '강동구',   127.1204,           37.5427,            'TOUR_API', false, 'PUBLISHED', now(), now()),
    (10, '128961',  'TOURIST_ATTRACTION', '서울', '광진구',   127.0913227521,     37.5349377239,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (11, '809490',  'TOURIST_ATTRACTION', '서울', '강서구',   126.8171490732,     37.5860879769,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (12, '3043735', 'TOURIST_ATTRACTION', '서울', '강서구',   126.8366643489,     37.5722343802,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (13, '2733968', 'TOURIST_ATTRACTION', '서울', '강서구',   126.81714524346386, 37.58604150556127,  'TOUR_API', false, 'PUBLISHED', now(), now()),
    (14, '2591792', 'TOURIST_ATTRACTION', '서울', '구로구',   126.8632141714,     37.4924524597,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (15, '126501',  'TOURIST_ATTRACTION', '서울', '성북구',   127.02827335807942, 37.59016231349472,  'TOUR_API', false, 'PUBLISHED', now(), now()),
    (16, '3385911', 'TOURIST_ATTRACTION', '서울', '강서구',   126.8045,           37.5812,            'TOUR_API', false, 'PUBLISHED', now(), now()),
    (17, '2733966', 'TOURIST_ATTRACTION', '서울', '구로구',   126.8916141621,     37.4995521419,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (18, '2758868', 'TOURIST_ATTRACTION', '서울', '용산구',   126.969158,         37.517195,          'TOUR_API', false, 'PUBLISHED', now(), now()),
    (19, '1604652', 'TOURIST_ATTRACTION', '서울', '종로구',   126.9767375783,     37.5760836609,      'TOUR_API', false, 'PUBLISHED', now(), now()),
    (20, '294505',  'TOURIST_ATTRACTION', '서울', '성북구',   127.0056310926,     37.6139242251,      'TOUR_API', false, 'PUBLISHED', now(), now())
ON CONFLICT (source, content_id) DO NOTHING;

INSERT INTO spot_translation (spot_id, locale, title, created_at, updated_at)
VALUES
    (1,  'ko-KR', '가회동성당',                      now(), now()),
    (2,  'ko-KR', '간데메공원',                      now(), now()),
    (3,  'ko-KR', '간송옛집',                        now(), now()),
    (4,  'ko-KR', '갈산공원',                        now(), now()),
    (5,  'ko-KR', '감로암(서울)',                    now(), now()),
    (6,  'ko-KR', '감사의 정원',                     now(), now()),
    (7,  'ko-KR', '강남',                            now(), now()),
    (8,  'ko-KR', '강남 마이스 관광특구',            now(), now()),
    (9,  'ko-KR', '강동예찬시비',                    now(), now()),
    (10, 'ko-KR', '강변스파랜드',                    now(), now()),
    (11, 'ko-KR', '강서습지생태공원',                now(), now()),
    (12, 'ko-KR', '강서역사문화거리',                now(), now()),
    (13, 'ko-KR', '강서한강공원',                    now(), now()),
    (14, 'ko-KR', '개봉유수지 생태공원',             now(), now()),
    (15, 'ko-KR', '개운사(서울)',                    now(), now()),
    (16, 'ko-KR', '개화산 호국공원',                 now(), now()),
    (17, 'ko-KR', '거리공원',                        now(), now()),
    (18, 'ko-KR', '거북선나루터(이촌 수상훈련장)',   now(), now()),
    (19, 'ko-KR', '건청궁',                          now(), now()),
    (20, 'ko-KR', '경국사(서울)',                    now(), now())
ON CONFLICT (spot_id, locale) DO NOTHING;
