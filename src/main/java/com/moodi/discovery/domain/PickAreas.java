package com.moodi.discovery.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 한 번의 추천 요청에서 고른 지역들 (DSC-04).
 *
 * <p>화면은 상위 지역을 고르면 하위 지역을 Disabled 처리하고, 하위를 고른 상태에서 상위를 고르면
 * 하위 칩을 상위로 대체한다. 서버도 같은 결과가 되도록 <b>포함 관계가 있으면 상위만 남긴다.</b>
 * 거부하지 않고 정리하는 이유는, 화면이 이미 대체 동작을 하므로 사용자가 의도한 결과가 "상위 하나"이기 때문이다.
 */
public class PickAreas {

    public static final int MAX_SIZE = 5;

    private final List<PickArea> values;

    private PickAreas(List<PickArea> values) {
        this.values = List.copyOf(values);
    }

    public static PickAreas of(List<PickArea> areas) {
        if (areas == null || areas.isEmpty()) {
            throw new BusinessException(ErrorCode.PICK_INVALID_AREA_SELECTION);
        }
        if (areas.size() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PICK_INVALID_AREA_SELECTION);
        }
        return new PickAreas(collapse(areas));
    }

    /**
     * 서로 포함 관계인 지역은 넓은 쪽만 남긴다. 같은 지역이 중복돼도 하나로 접힌다.
     */
    private static List<PickArea> collapse(List<PickArea> areas) {
        List<PickArea> collapsed = new ArrayList<>();
        for (PickArea candidate : areas) {
            if (areas.stream().anyMatch(other -> other != candidate && covers(other, candidate))) {
                continue;
            }
            if (collapsed.stream().noneMatch(kept -> kept.equals(candidate))) {
                collapsed.add(candidate);
            }
        }
        return collapsed;
    }

    /**
     * 완전히 같은 지역은 "덮는다"고 보지 않는다. 그러면 중복 선택 시 양쪽이 서로를 지워 모두 사라진다.
     */
    private static boolean covers(PickArea wider, PickArea narrower) {
        return !wider.equals(narrower) && wider.contains(narrower);
    }

    public List<PickArea> values() {
        return values;
    }

    public int size() {
        return values.size();
    }
}
