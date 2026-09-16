package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.domain.RecommendedArea;
import com.moodi.discovery.domain.RecommendedAreaRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class RecommendedAreaService {

    private final RecommendedAreaRepository repository;
    private final AreaSuggestReader areaReader;

    public RecommendedAreaService(RecommendedAreaRepository repository, AreaSuggestReader areaReader) {
        this.repository = repository;
        this.areaReader = areaReader;
    }

    public record Command(PickAreaLevel level, String region, String district, String neighborhood, String label, int sortOrder) {}

    public record View(Long id, PickAreaLevel level, String region, String district, String neighborhood, String label, int sortOrder) {
        public static View from(RecommendedArea e) {
            return new View(e.getId(), e.getLevel(), e.getRegion(), e.getDistrict(), e.getNeighborhood(), e.getLabel(), e.getSortOrder());
        }
    }

    @Transactional(readOnly = true)
    public List<View> getAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(View::from).toList();
    }

    @Transactional(readOnly = true)
    public List<View> getPublished() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().filter(this::available).map(View::from).toList();
    }

    @Transactional(readOnly = true)
    public View get(Long id) {
        return View.from(find(id));
    }

    public Long create(Command c) {
        RecommendedArea entity = RecommendedArea.create(c.level(), c.region(), c.district(), c.neighborhood(), c.label(), c.sortOrder());
        validateReferences(entity);
        return repository.save(entity).getId();
    }

    public void update(Long id, Command c) {
        RecommendedArea entity = find(id);
        entity.update(c.level(), c.region(), c.district(), c.neighborhood(), c.label(), c.sortOrder());
        validateReferences(entity);
        repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    public void reorder(List<Long> ids) {
        List<RecommendedArea> entities = repository.findAllByOrderBySortOrderAscIdAsc();
        if (ids == null || ids.stream().anyMatch(Objects::isNull) || ids.size() != entities.size() || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(entities.stream().map(RecommendedArea::getId).toList()))) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        for (RecommendedArea entity : entities) {
            entity.reorder(ids.indexOf(entity.getId()));
            repository.save(entity);
        }
    }

    private RecommendedArea find(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateReferences(RecommendedArea entity) {
        if (!available(entity)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private boolean available(RecommendedArea entity) {
        String keyword = switch (entity.getLevel()) {
            case REGION -> entity.getRegion();
            case DISTRICT -> entity.getDistrict();
            case NEIGHBORHOOD -> entity.getNeighborhood();
        }
        ;
        return areaReader.search(keyword, Integer.MAX_VALUE).stream().anyMatch(a -> a.level() == entity.getLevel() && Objects.equals(a.region(), entity.getRegion()) && Objects.equals(a.district(), entity.getDistrict()) && Objects.equals(a.neighborhood(), entity.getNeighborhood()));
    }
}
