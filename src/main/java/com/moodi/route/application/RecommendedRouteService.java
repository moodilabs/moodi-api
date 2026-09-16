package com.moodi.route.application;

import com.moodi.route.domain.RecommendedRoute;
import com.moodi.route.domain.RecommendedRouteRepository;
import com.moodi.route.domain.RecommendedRouteStop;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class RecommendedRouteService {

    private final RecommendedRouteRepository repository;
    private final SpotSnapshotReader spotReader;

    public RecommendedRouteService(RecommendedRouteRepository repository, SpotSnapshotReader spotReader) {
        this.repository = repository;
        this.spotReader = spotReader;
    }

    public record Command(String title, String imageUrl, String region, boolean visible, int sortOrder, List<Stop> stops) {
        public record Stop(Long spotId, int day, int sequence) {}
        public List<RecommendedRouteStop> toStops() {
            if (stops == null || stops.stream().anyMatch(Objects::isNull)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
            return stops.stream().map(s -> new RecommendedRouteStop(s.spotId(), s.day(), s.sequence())).toList();
        }
    }

    public record View(Long id, String title, String imageUrl, String region, boolean visible, int sortOrder, List<Command.Stop> stops) {
        public static View from(RecommendedRoute e) {
            return new View(e.getId(), e.getTitle(), e.getImageUrl(), e.getRegion(), e.isVisible(), e.getSortOrder(), e.getStops().stream().map(s -> new Command.Stop(s.getSpotId(), s.getDay(), s.getSequence())).toList());
        }
    }

    @Transactional(readOnly = true)
    public List<View> getAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(View::from).toList();
    }

    @Transactional(readOnly = true)
    public List<View> getPublished() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().filter(this::available).limit(3).map(View::from).toList();
    }

    @Transactional(readOnly = true)
    public View get(Long id) {
        return View.from(find(id));
    }

    public Long create(Command c) {
        RecommendedRoute entity = RecommendedRoute.create(c.title(), c.imageUrl(), c.region(), c.visible(), c.sortOrder(), c.toStops());
        validateReferences(entity);
        return repository.save(entity).getId();
    }

    public void update(Long id, Command c) {
        RecommendedRoute entity = find(id);
        entity.update(c.title(), c.imageUrl(), c.region(), c.visible(), c.sortOrder(), c.toStops());
        validateReferences(entity);
        repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    public void reorder(List<Long> ids) {
        List<RecommendedRoute> entities = repository.findAllByOrderBySortOrderAscIdAsc();
        if (ids == null || ids.stream().anyMatch(Objects::isNull) || ids.size() != entities.size() || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(entities.stream().map(RecommendedRoute::getId).toList()))) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        for (RecommendedRoute entity : entities) {
            entity.reorder(ids.indexOf(entity.getId()));
            repository.save(entity);
        }
    }

    public void changeVisibility(Long id, boolean visible) {
        RecommendedRoute entity = find(id);
        if (visible) validateReferences(entity);
        entity.changeVisibility(visible);
        repository.save(entity);
    }

    private RecommendedRoute find(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateReferences(RecommendedRoute entity) {
        if (spotReader.readBySpotIds(entity.getStops().stream().map(RecommendedRouteStop::getSpotId).toList()).size() != entity.getStops().size()) throw new BusinessException(ErrorCode.SPOT_NOT_AVAILABLE);
    }

    private boolean available(RecommendedRoute entity) {
        return entity.isVisible() && spotReader.readBySpotIds(entity.getStops().stream().map(RecommendedRouteStop::getSpotId).toList()).size() == entity.getStops().size();
    }
}
