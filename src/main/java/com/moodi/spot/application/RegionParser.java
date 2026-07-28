package com.moodi.spot.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionParser {

    private static final Pattern NEIGHBORHOOD_PATTERN =
            Pattern.compile("\\(([^()]*(?:동|가|읍|면|리))\\)");

    private RegionParser() {
    }

    public static ParsedRegion parse(String addr1) {
        if (addr1 == null || addr1.isBlank()) {
            return new ParsedRegion(null, null);
        }

        String[] tokens = addr1.trim().split("\\s+");

        String district = extractDistrict(tokens);
        String neighborhood = extractNeighborhood(addr1);

        return new ParsedRegion(district, neighborhood);
    }

    private static String extractDistrict(String[] tokens) {
        if (tokens.length < 2) {
            return null;
        }

        String candidate = tokens[1];
        if (candidate.endsWith("구") || candidate.endsWith("군")) {
            return candidate;
        }

        return null;
    }

    private static String extractNeighborhood(String address) {
        Matcher matcher = NEIGHBORHOOD_PATTERN.matcher(address);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1).trim();
    }
}
