package com.moodi.route.application;

import java.util.List;

public interface SpotSnapshotReader {

    List<SpotSnapshot> readBySpotIds(List<Long> spotIds);
}
