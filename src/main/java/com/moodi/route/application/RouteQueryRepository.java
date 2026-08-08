package com.moodi.route.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RouteQueryRepository {

    List<RouteListRow> findByMemberLatest(UUID memberId, Long cursorId,
                                          LocalDateTime cursorUpdatedAt, int size);
}
