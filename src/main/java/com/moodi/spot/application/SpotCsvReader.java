package com.moodi.spot.application;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SpotCsvReader {

    public ReadResult read(Reader reader) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(reader, format)) {
            List<SpotCsvRow> rows = new ArrayList<>();
            int failedRows = 0;

            for (CSVRecord record : parser) {
                try {
                    rows.add(toRow(record));
                } catch (Exception e) {
                    failedRows++;
                    log.warn("CSV 파싱 실패 [행 {}]: {}", record.getRecordNumber() + 1, e.getMessage());
                }
            }

            return new ReadResult(rows, failedRows);
        }
    }

    private SpotCsvRow toRow(CSVRecord record) {
        return SpotCsvRow.builder()
                .contentId(record.get("content_id"))
                .title(record.get("title"))
                .contentType(record.get("content_type"))
                .area(record.get("area"))
                .source(record.get("source"))
                .overview(record.get("overview"))
                .spotImage(record.get("spot_image"))
                .longitude(record.get("longitude"))
                .latitude(record.get("latitude"))
                .addr1(record.get("addr1"))
                .addr2(record.get("addr2"))
                .tel(record.get("tel"))
                .lclsSystm1(record.get("lcls_systm1"))
                .lclsSystm2(record.get("lcls_systm2"))
                .lclsSystm3(record.get("lcls_systm3"))
                .homepage(record.get("homepage"))
                .build();
    }

    public record ReadResult(List<SpotCsvRow> rows, int failedRows) {
    }
}
