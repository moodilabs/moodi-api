package com.moodi.route.application;

import java.time.LocalDate;
import java.util.List;

public record RouteGenerateCommand(
        List<Long> spotIds,
        List<AreaCondition> areas,
        LocalDate startDate,
        LocalDate endDate
) {
}
