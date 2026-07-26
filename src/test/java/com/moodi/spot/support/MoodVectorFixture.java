package com.moodi.spot.support;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;

import java.util.Map;

public class MoodVectorFixture {

    public static MoodVector create() {
        return new MoodVector(
                Map.of(Atmosphere.COZY, 0.5, Atmosphere.SERENE, 0.3, Atmosphere.LIVELY, 0.2),
                Map.of(Color.WARM, 0.6, Color.MONO, 0.4),
                Map.of(Lighting.DAYLIGHT, 0.7, Lighting.INDOOR, 0.3),
                Map.of(Space.NATURE, 0.5, Space.URBAN, 0.5),
                Map.of(Structure.ORGANIC, 0.6, Structure.GEOMETRIC, 0.4),
                Map.of(Era.MODERN, 0.5, Era.RETRO, 0.5)
        );
    }

    private MoodVectorFixture() {
    }
}
