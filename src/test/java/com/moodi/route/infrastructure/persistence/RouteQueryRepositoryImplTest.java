package com.moodi.route.infrastructure.persistence;

import com.moodi.route.application.RouteListRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteQueryRepositoryImplTest {

    private final RouteQueryRepositoryImpl repository = new RouteQueryRepositoryImpl(null);

    private static final UUID PUBLIC_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 11);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 8, 9, 12, 30);

    @Test
    @DisplayName("네이티브 쿼리 스칼라가 java.time 타입으로 오면 그대로 매핑한다 (Hibernate 7)")
    void map_row_with_java_time_types() {
        Object[] row = {1L, PUBLIC_ID, "서울 여행", START, END, 3L, UPDATED_AT};

        RouteListRow result = repository.toRouteListRow(row);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.publicId()).isEqualTo(PUBLIC_ID);
        assertThat(result.title()).isEqualTo("서울 여행");
        assertThat(result.startDate()).isEqualTo(START);
        assertThat(result.endDate()).isEqualTo(END);
        assertThat(result.spotCount()).isEqualTo(3);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("네이티브 쿼리 스칼라가 java.sql 타입으로 와도 매핑한다")
    void map_row_with_java_sql_types() {
        Object[] row = {1, PUBLIC_ID, "서울 여행",
                Date.valueOf(START), Date.valueOf(END), 3, Timestamp.valueOf(UPDATED_AT)};

        RouteListRow result = repository.toRouteListRow(row);

        assertThat(result.startDate()).isEqualTo(START);
        assertThat(result.endDate()).isEqualTo(END);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("지원하지 않는 날짜 타입이면 실패한다")
    void map_row_with_unsupported_date_type() {
        Object[] row = {1L, PUBLIC_ID, "서울 여행", "2026-08-10", END, 3L, UPDATED_AT};

        assertThatThrownBy(() -> repository.toRouteListRow(row))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
