package com.moodi.shared.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void of_calculatesTotalPagesCorrectly() {
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 0, 10, 25L);

        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void of_hasNextFalse_whenLastPage() {
        PageResponse<String> response = PageResponse.of(List.of("a"), 2, 10, 25L);

        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void of_singlePage_whenElementsLessEqualSize() {
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 0, 10, 5L);

        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }
}
