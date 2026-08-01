package com.moodi.route.application;

import java.util.List;

public interface RouteTitleGenerator {

    String generate(List<String> areas, int totalDays);
}
