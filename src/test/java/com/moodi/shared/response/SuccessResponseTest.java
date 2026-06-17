package com.moodi.shared.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SuccessResponseTest {

    @Test
    void of_wrapsDataCorrectly() {
        SuccessResponse<String> response = SuccessResponse.of("hello");

        assertThat(response.data()).isEqualTo("hello");
    }

    @Test
    void of_supportsNullData() {
        SuccessResponse<Object> response = SuccessResponse.of(null);

        assertThat(response.data()).isNull();
    }
}
