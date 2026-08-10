package com.moodi.spot.application;

import java.util.List;

public interface SpotBatchWriter {

    UpsertResult upsertAll(List<SpotImportRow> rows);

    record UpsertResult(int inserted, int updated) {
    }
}
