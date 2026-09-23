package com.moodi.shared.mood;

public record MoodTagResponse(String key, String label, String displayTag, String displayName) {

    public static MoodTagResponse from(MoodTag tag) {
        return new MoodTagResponse(tag.getKey(), tag.getLabel(), tag.getDisplayTag(), tag.getDisplayName());
    }
}
