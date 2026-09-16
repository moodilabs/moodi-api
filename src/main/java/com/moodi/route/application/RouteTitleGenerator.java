package com.moodi.route.application;

import java.util.List;

public interface RouteTitleGenerator {

    String generate(List<String> areas, List<String> moodTagKeys, int totalDays);
}
