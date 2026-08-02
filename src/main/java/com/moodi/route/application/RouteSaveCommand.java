package com.moodi.route.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteSaveCommand(
        UUID memberId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<DayCommand> days
) {
    public record DayCommand(
            int dayNumber,
            LocalDate date,
            List<Long> spotIds
    ) {
    }
}
