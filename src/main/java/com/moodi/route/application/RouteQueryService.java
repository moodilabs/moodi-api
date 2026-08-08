package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteQueryService {

    private final RouteRepository routeRepository;
    private final RouteQueryRepository routeQueryRepository;

    public CursorResponse<RouteListRow> getList(UUID memberId, String cursor, int size) {
        Long cursorId = null;
        LocalDateTime cursorUpdatedAt = null;

        if (cursor != null && !cursor.isBlank()) {
            String[] parts = cursor.split(",", 2);
            if (parts.length != 2) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
            }
            try {
                cursorUpdatedAt = LocalDateTime.parse(parts[0]);
                cursorId = Long.parseLong(parts[1]);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
            }
        }

        List<RouteListRow> rows = routeQueryRepository.findByMemberLatest(
                memberId, cursorId, cursorUpdatedAt, size);

        boolean hasNext = rows.size() > size;
        List<RouteListRow> pageRows = hasNext ? rows.subList(0, size) : rows;

        if (pageRows.isEmpty()) {
            return CursorResponse.empty();
        }

        String nextCursor = null;
        if (hasNext) {
            RouteListRow last = pageRows.getLast();
            nextCursor = last.updatedAt().toString() + "," + last.id();
        }

        return CursorResponse.of(pageRows, nextCursor, hasNext);
    }

    public Route getDetail(UUID publicId, UUID memberId) {
        Route route = routeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        route.validateOwner(memberId);

        return route;
    }

    public Route getSharedDetail(UUID publicId) {
        return routeRepository.findSharedByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));
    }
}
