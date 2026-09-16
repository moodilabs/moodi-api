package com.moodi.route.application;

import com.moodi.shared.mood.MoodTag;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleBasedTitleGenerator implements RouteTitleGenerator {

    private static final int MAX_LENGTH = 40;

    @Override
    public String generate(List<String> areas, List<String> moodTagKeys, int totalDays) {
        String period = totalDays + "-Day";
        String mood = findDominantMoodDisplayTag(moodTagKeys);
        String area = areas.isEmpty() ? "" : areas.getFirst();

        // "{Mood} {Area} {N}-Day Trip" — 무드나 지역이 없으면 빈 부분 생략
        StringBuilder sb = new StringBuilder();
        if (!mood.isEmpty()) {
            sb.append(mood).append(" ");
        }
        if (!area.isEmpty()) {
            sb.append(area).append(" ");
        }
        sb.append(period).append(" Trip");

        String title = sb.toString();
        if (title.length() > MAX_LENGTH) {
            title = title.substring(0, MAX_LENGTH - 1) + "…";
        }
        return title;
    }

    private String findDominantMoodDisplayTag(List<String> moodTagKeys) {
        if (moodTagKeys == null || moodTagKeys.isEmpty()) {
            return "";
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String key : moodTagKeys) {
            counts.merge(key, 1, Integer::sum);
        }
        String dominant = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (dominant == null) {
            return "";
        }
        return MoodTag.fromKey(dominant).getDisplayTag();
    }
}
