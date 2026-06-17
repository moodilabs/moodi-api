package com.moodi.shared.docs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

public class CommonDocumentation {

    public static FieldDescriptor[] errorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("title").type(JsonFieldType.STRING).description("HTTP 상태 메시지"),
                fieldWithPath("status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                fieldWithPath("detail").type(JsonFieldType.STRING).description("에러 상세 메시지"),
                fieldWithPath("instance").type(JsonFieldType.STRING).description("요청 URI"),
                fieldWithPath("timestamp").type(JsonFieldType.STRING).description("에러 발생 시간"),
                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드")
        };
    }

    public static FieldDescriptor[] pageResponseFields(FieldDescriptor... dataFields) {
        FieldDescriptor[] pageFields = new FieldDescriptor[]{
                fieldWithPath("page").type(JsonFieldType.NUMBER).description("현재 페이지 (0부터 시작)"),
                fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 데이터 수"),
                fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
        };

        FieldDescriptor[] result = new FieldDescriptor[dataFields.length + pageFields.length];
        System.arraycopy(dataFields, 0, result, 0, dataFields.length);
        System.arraycopy(pageFields, 0, result, dataFields.length, pageFields.length);
        return result;
    }

    private CommonDocumentation() {}
}
