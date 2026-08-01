package com.moodi.route.application;

import java.util.Optional;

public interface LegClient {

    Optional<LegResult> findTransitRoute(double startLongitude, double startLatitude,
                                          double endLongitude, double endLatitude);

    Optional<LegResult> findWalkRoute(double startLongitude, double startLatitude,
                                       double endLongitude, double endLatitude);
}
