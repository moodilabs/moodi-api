package com.moodi.route.application;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleBasedTitleGenerator implements RouteTitleGenerator {

    @Override
    public String generate(List<String> areas, int totalDays) {
        String areaText = areas.isEmpty() ? "여행" : areas.getFirst();
        String periodText = formatPeriod(totalDays);
        return areaText + " " + periodText + " 코스";
    }

    private String formatPeriod(int totalDays) {
        if (totalDays == 1) {
            return "당일치기";
        }
        return (totalDays - 1) + "박 " + totalDays + "일";
    }
}
