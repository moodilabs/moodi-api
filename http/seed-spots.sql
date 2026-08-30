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

-- 스팟 이미지 (검색 결과에 대표 이미지 노출용)
INSERT INTO spot_image (spot_id, image_url, is_primary, sort_order, created_at, updated_at)
VALUES
    (1,  'https://example.com/images/spot1/400/300',  true, 0, now(), now()),
    (2,  'https://example.com/images/spot2/400/300',  true, 0, now(), now()),
    (3,  'https://example.com/images/spot3/400/300',  true, 0, now(), now()),
    (4,  'https://example.com/images/spot4/400/300',  true, 0, now(), now()),
    (5,  'https://example.com/images/spot5/400/300',  true, 0, now(), now()),
    (6,  'https://example.com/images/spot6/400/300',  true, 0, now(), now()),
    (7,  'https://example.com/images/spot7/400/300',  true, 0, now(), now()),
    (8,  'https://example.com/images/spot8/400/300',  true, 0, now(), now()),
    (9,  'https://example.com/images/spot9/400/300',  true, 0, now(), now()),
    (10, 'https://example.com/images/spot10/400/300', true, 0, now(), now()),
    (11, 'https://example.com/images/spot11/400/300', true, 0, now(), now()),
    (12, 'https://example.com/images/spot12/400/300', true, 0, now(), now()),
    (13, 'https://example.com/images/spot13/400/300', true, 0, now(), now()),
    (14, 'https://example.com/images/spot14/400/300', true, 0, now(), now()),
    (15, 'https://example.com/images/spot15/400/300', true, 0, now(), now()),
    (16, 'https://example.com/images/spot16/400/300', true, 0, now(), now()),
    (17, 'https://example.com/images/spot17/400/300', true, 0, now(), now()),
    (18, 'https://example.com/images/spot18/400/300', true, 0, now(), now()),
    (19, 'https://example.com/images/spot19/400/300', true, 0, now(), now()),
    (20, 'https://example.com/images/spot20/400/300', true, 0, now(), now())
ON CONFLICT DO NOTHING;

-- 스팟 설명 (검색 결과에 description 노출)
INSERT INTO spot_description (spot_id, locale, content, created_at, updated_at)
VALUES
    (1,  'en-US', 'A beautiful cathedral in Jongno-gu, with distinctive architecture that harmonizes with the traditional hanok village.', now(), now()),
    (2,  'en-US', 'A nature park in Dongdaemun-gu, a popular walking spot close to the city center.', now(), now()),
    (3,  'en-US', 'The former residence of Gansong Jeon Hyeong-pil, where you can feel the history of cultural heritage preservation.', now(), now()),
    (5,  'en-US', 'A serene hermitage in Jongno-gu, offering a space for meditation and healing in the heart of the city.', now(), now()),
    (6,  'en-US', 'A garden of gratitude, a hidden healing spot in Jongno-gu.', now(), now()),
    (7,  'en-US', 'One of Seoul''s most iconic districts, a vibrant area where shopping, dining, and culture converge.', now(), now()),
    (8,  'en-US', 'The Gangnam MICE Tourism Special Zone, a business hub hosting conventions and exhibitions.', now(), now()),
    (10, 'en-US', 'A spa facility along the Han River in Gwangjin-gu, offering hot spring relaxation in the city.', now(), now()),
    (13, 'en-US', 'A riverside park in Gangseo-gu, ideal for cycling and picnics along the Han River.', now(), now()),
    (18, 'en-US', 'A waterfront experience facility at Ichon along the Han River, where you can learn history with a turtle ship replica.', now(), now()),
    (19, 'en-US', 'Geoncheonggung Palace in the rear garden of Gyeongbokgung, a place holding the historical pain of the Joseon royal family.', now(), now())
ON CONFLICT (spot_id, locale) DO NOTHING;

-- 스팟 무드 태그 (무드 필터 검색용)
-- mood_vector: 6축(atmosphere, color, lighting, space, structure, era) JSON
-- mood_tags: MoodTag enum key 배열
INSERT INTO spot_mood (spot_id, mood_vector, mood_tags, confidence, created_at, updated_at)
VALUES
    (1,  '{"atmosphere":{"cozy":0.15,"serene":0.45,"lively":0.05,"romantic":0.20,"moody":0.15},"color":{"warm":0.35,"cool":0.15,"pastel":0.15,"mono":0.25,"vivid":0.10},"lighting":{"daylight":0.30,"golden_hour":0.20,"night_neon":0.05,"overcast":0.15,"indoor":0.30},"space":{"nature":0.10,"ocean":0.00,"urban":0.30,"interior":0.40,"alley_local":0.20},"structure":{"open":0.15,"minimal":0.25,"dense":0.25,"geometric":0.20,"organic":0.15},"era":{"traditional":0.60,"retro":0.20,"modern":0.10,"futuristic":0.10}}',
         '["traditional","serene","cozy"]', 0.85, now(), now()),
    (2,  '{"atmosphere":{"cozy":0.10,"serene":0.50,"lively":0.15,"romantic":0.15,"moody":0.10},"color":{"warm":0.25,"cool":0.25,"pastel":0.15,"mono":0.15,"vivid":0.20},"lighting":{"daylight":0.50,"golden_hour":0.25,"night_neon":0.00,"overcast":0.20,"indoor":0.05},"space":{"nature":0.60,"ocean":0.05,"urban":0.10,"interior":0.10,"alley_local":0.15},"structure":{"open":0.40,"minimal":0.15,"dense":0.10,"geometric":0.05,"organic":0.30},"era":{"traditional":0.20,"retro":0.15,"modern":0.45,"futuristic":0.20}}',
         '["nature","serene"]', 0.80, now(), now()),
    (5,  '{"atmosphere":{"cozy":0.20,"serene":0.45,"lively":0.05,"romantic":0.15,"moody":0.15},"color":{"warm":0.35,"cool":0.15,"pastel":0.15,"mono":0.25,"vivid":0.10},"lighting":{"daylight":0.25,"golden_hour":0.25,"night_neon":0.00,"overcast":0.20,"indoor":0.30},"space":{"nature":0.30,"ocean":0.00,"urban":0.10,"interior":0.40,"alley_local":0.20},"structure":{"open":0.15,"minimal":0.30,"dense":0.20,"geometric":0.15,"organic":0.20},"era":{"traditional":0.55,"retro":0.20,"modern":0.15,"futuristic":0.10}}',
         '["serene","traditional"]', 0.90, now(), now()),
    (6,  '{"atmosphere":{"cozy":0.20,"serene":0.30,"lively":0.10,"romantic":0.30,"moody":0.10},"color":{"warm":0.30,"cool":0.15,"pastel":0.30,"mono":0.10,"vivid":0.15},"lighting":{"daylight":0.40,"golden_hour":0.30,"night_neon":0.00,"overcast":0.15,"indoor":0.15},"space":{"nature":0.45,"ocean":0.05,"urban":0.15,"interior":0.15,"alley_local":0.20},"structure":{"open":0.35,"minimal":0.20,"dense":0.10,"geometric":0.10,"organic":0.25},"era":{"traditional":0.25,"retro":0.15,"modern":0.40,"futuristic":0.20}}',
         '["nature","romantic","serene"]', 0.82, now(), now()),
    (7,  '{"atmosphere":{"cozy":0.05,"serene":0.05,"lively":0.50,"romantic":0.20,"moody":0.20},"color":{"warm":0.15,"cool":0.25,"pastel":0.05,"mono":0.20,"vivid":0.35},"lighting":{"daylight":0.20,"golden_hour":0.10,"night_neon":0.40,"overcast":0.05,"indoor":0.25},"space":{"nature":0.05,"ocean":0.00,"urban":0.50,"interior":0.30,"alley_local":0.15},"structure":{"open":0.15,"minimal":0.20,"dense":0.30,"geometric":0.25,"organic":0.10},"era":{"traditional":0.05,"retro":0.10,"modern":0.50,"futuristic":0.35}}',
         '["cityscape","lively","modern"]', 0.88, now(), now()),
    (10, '{"atmosphere":{"cozy":0.30,"serene":0.25,"lively":0.10,"romantic":0.25,"moody":0.10},"color":{"warm":0.35,"cool":0.15,"pastel":0.20,"mono":0.15,"vivid":0.15},"lighting":{"daylight":0.15,"golden_hour":0.20,"night_neon":0.10,"overcast":0.15,"indoor":0.40},"space":{"nature":0.10,"ocean":0.05,"urban":0.25,"interior":0.50,"alley_local":0.10},"structure":{"open":0.15,"minimal":0.30,"dense":0.15,"geometric":0.20,"organic":0.20},"era":{"traditional":0.10,"retro":0.20,"modern":0.45,"futuristic":0.25}}',
         '["cozy","romantic"]', 0.75, now(), now()),
    (13, '{"atmosphere":{"cozy":0.15,"serene":0.30,"lively":0.25,"romantic":0.20,"moody":0.10},"color":{"warm":0.25,"cool":0.20,"pastel":0.15,"mono":0.15,"vivid":0.25},"lighting":{"daylight":0.45,"golden_hour":0.30,"night_neon":0.00,"overcast":0.15,"indoor":0.10},"space":{"nature":0.50,"ocean":0.10,"urban":0.10,"interior":0.10,"alley_local":0.20},"structure":{"open":0.40,"minimal":0.15,"dense":0.10,"geometric":0.10,"organic":0.25},"era":{"traditional":0.15,"retro":0.10,"modern":0.50,"futuristic":0.25}}',
         '["nature","lively","expansive"]', 0.83, now(), now()),
    (18, '{"atmosphere":{"cozy":0.15,"serene":0.25,"lively":0.20,"romantic":0.20,"moody":0.20},"color":{"warm":0.25,"cool":0.20,"pastel":0.15,"mono":0.25,"vivid":0.15},"lighting":{"daylight":0.35,"golden_hour":0.20,"night_neon":0.05,"overcast":0.25,"indoor":0.15},"space":{"nature":0.25,"ocean":0.05,"urban":0.30,"interior":0.20,"alley_local":0.20},"structure":{"open":0.25,"minimal":0.20,"dense":0.15,"geometric":0.20,"organic":0.20},"era":{"traditional":0.45,"retro":0.25,"modern":0.20,"futuristic":0.10}}',
         '["traditional","riverside"]', 0.78, now(), now()),
    (19, '{"atmosphere":{"cozy":0.15,"serene":0.30,"lively":0.05,"romantic":0.25,"moody":0.25},"color":{"warm":0.35,"cool":0.15,"pastel":0.10,"mono":0.30,"vivid":0.10},"lighting":{"daylight":0.25,"golden_hour":0.25,"night_neon":0.00,"overcast":0.20,"indoor":0.30},"space":{"nature":0.15,"ocean":0.05,"urban":0.30,"interior":0.35,"alley_local":0.15},"structure":{"open":0.15,"minimal":0.20,"dense":0.25,"geometric":0.25,"organic":0.15},"era":{"traditional":0.55,"retro":0.20,"modern":0.15,"futuristic":0.10}}',
         '["traditional","serene","moody"]', 0.92, now(), now())
ON CONFLICT (spot_id) DO NOTHING;

-- 북마크 (saved 필터 + bookmark_count 테스트용, 테스트 회원 UUID 사용)
-- 토큰 발급 시 사용할 memberId: 11111111-1111-1111-1111-111111111111
INSERT INTO bookmark (member_id, spot_id, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 1,  now(), now()),
    ('11111111-1111-1111-1111-111111111111', 7,  now(), now()),
    ('11111111-1111-1111-1111-111111111111', 19, now(), now()),
    ('22222222-2222-2222-2222-222222222222', 7,  now(), now()),
    ('22222222-2222-2222-2222-222222222222', 1,  now(), now()),
    ('22222222-2222-2222-2222-222222222222', 13, now(), now()),
    ('33333333-3333-3333-3333-333333333333', 7,  now(), now()),
    ('33333333-3333-3333-3333-333333333333', 19, now(), now())
ON CONFLICT (member_id, spot_id) DO NOTHING;
