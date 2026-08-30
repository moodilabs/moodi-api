package com.moodi.discovery.domain;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * 업로드 사진과 스팟의 무드 벡터 유사도 (DSC-05).
 *
 * <p>Feed는 무드 태그 교집합 <b>개수</b>로 정렬한다. 정수 등급이라 동점이 많이 생기는데,
 * 피드는 그 안을 셔플로 갈라도 되지만 Pick은 상위 5개만 뽑으므로 순서가 촘촘해야 한다.
 * 그래서 태그가 아니라 벡터를 직접 비교한다.
 *
 * <p>6개 축을 하나로 이어 붙여 코사인 유사도를 구한다. 축마다 따로 재고 평균 내는 방법도 있지만,
 * 그러면 사진에서 거의 드러나지 않는 축(예: era)이 뚜렷한 축과 같은 비중을 갖는다.
 * 이어 붙이면 분포가 한쪽으로 쏠린 축이 자연스럽게 더 큰 영향을 준다.
 */
public final class MoodSimilarity {

    private MoodSimilarity() {
    }

    public static double between(MoodVector left, MoodVector right) {
        double[] a = flatten(left);
        double[] b = flatten(right);

        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            leftNorm += a[i] * a[i];
            rightNorm += b[i] * b[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    /**
     * 축 순서와 축 안의 enum 순서를 고정해야 같은 자리끼리 비교된다.
     */
    private static double[] flatten(MoodVector vector) {
        List<Double> weights = new ArrayList<>();
        for (Atmosphere key : Atmosphere.values()) {
            weights.add(vector.getWeight(key));
        }
        for (Color key : Color.values()) {
            weights.add(vector.getWeight(key));
        }
        for (Lighting key : Lighting.values()) {
            weights.add(vector.getWeight(key));
        }
        for (Space key : Space.values()) {
            weights.add(vector.getWeight(key));
        }
        for (Structure key : Structure.values()) {
            weights.add(vector.getWeight(key));
        }
        for (Era key : Era.values()) {
            weights.add(vector.getWeight(key));
        }
        double[] flattened = new double[weights.size()];
        for (int i = 0; i < flattened.length; i++) {
            flattened[i] = weights.get(i);
        }
        return flattened;
    }
}
