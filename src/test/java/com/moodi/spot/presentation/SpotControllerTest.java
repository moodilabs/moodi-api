package com.moodi.spot.presentation;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.SpotDetailService;
import com.moodi.spot.application.SpotSearchService;
import com.moodi.spot.application.dto.SpotSearchSortType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotControllerTest {

    @Mock
    private SpotDetailService spotDetailService;

    @Mock
    private SpotSearchService spotSearchService;

    @InjectMocks
    private SpotController spotController;

    @Test
    @DisplayName("비로그인 요청의 saved=true는 거부한다")
    void search_rejects_saved_filter_without_member() {
        // saved 를 조용히 무시하면 저장한 적 없는 스팟이 담긴 목록을 "저장한 스팟"
        // 이라며 돌려주게 된다 — 필터 없는 응답과 완전히 같은 결과였다.
        assertThatThrownBy(() -> spotController.searchSpots(
                null, null, null, true, SpotSearchSortType.BEST_MATCH, null, null, 20, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("로그인 요청의 saved=true는 그대로 조회한다")
    void search_allows_saved_filter_with_member() {
        UUID memberId = UUID.randomUUID();
        when(spotSearchService.search(eq(memberId), any())).thenReturn(CursorResponse.empty());

        assertThatCode(() -> spotController.searchSpots(
                null, null, null, true, SpotSearchSortType.BEST_MATCH, null, null, 20, memberId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("saved=false면 비로그인도 그대로 조회한다")
    void search_allows_unauthenticated_when_not_saved() {
        when(spotSearchService.search(isNull(), any())).thenReturn(CursorResponse.empty());

        assertThatCode(() -> spotController.searchSpots(
                null, null, null, false, SpotSearchSortType.BEST_MATCH, null, null, 20, null))
                .doesNotThrowAnyException();
    }
}
