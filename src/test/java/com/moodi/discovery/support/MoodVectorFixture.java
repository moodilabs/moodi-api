package com.moodi.discovery.support;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;

import java.util.Map;

public final class MoodVectorFixture {

    private MoodVectorFixture() {
    }

    /**
     * 각 축에서 지정한 항목에 가중치를 몰아준 벡터. 유사도 비교를 예측 가능하게 만든다.
     */
    public static MoodVector focused(Atmosphere atmosphere, Color color, Lighting lighting,
                                     Space space, Structure structure, Era era) {
        return new MoodVector(
                Map.of(atmosphere, 1.0),
                Map.of(color, 1.0),
                Map.of(lighting, 1.0),
                Map.of(space, 1.0),
                Map.of(structure, 1.0),
                Map.of(era, 1.0)
        );
    }

    public static MoodVector serene() {
        return focused(Atmosphere.SERENE, Color.COOL, Lighting.DAYLIGHT,
                Space.NATURE, Structure.OPEN, Era.MODERN);
    }

    public static MoodVector lively() {
        return focused(Atmosphere.LIVELY, Color.VIVID, Lighting.NIGHT_NEON,
                Space.URBAN, Structure.DENSE, Era.FUTURISTIC);
    }
}
