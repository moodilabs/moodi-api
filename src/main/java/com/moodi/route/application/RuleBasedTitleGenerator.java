package com.moodi.route.application;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleBasedTitleGenerator implements RouteTitleGenerator {

    @Override
    public String generate(List<String> areas, int totalDays) {
        String periodText = totalDays + "-Day";
        String areaText = areas.isEmpty() ? "" : " " + areas.getFirst();
        return periodText + areaText + " Trip";
    }
}
