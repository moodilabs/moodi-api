package com.moodi.member.application;

import com.moodi.member.domain.SurveyImage;
import com.moodi.member.domain.SurveyImageRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class SurveyImageService {

    private final SurveyImageRepository repository;
    private final SurveySpotReader spotReader;

    public SurveyImageService(SurveyImageRepository repository, SurveySpotReader spotReader) {
        this.repository = repository;
        this.spotReader = spotReader;
    }

    public record Command(MoodTag mood, Long spotId, String imageUrl, int sortOrder) {}

    public record View(Long id, MoodTag mood, Long spotId, String imageUrl, int sortOrder) {
        public static View from(SurveyImage e) {
            return new View(e.getId(), e.getMood(), e.getSpotId(), e.getImageUrl(), e.getSortOrder());
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
        SurveyImage entity = SurveyImage.create(c.mood(), c.spotId(), c.imageUrl(), c.sortOrder());
        validateReferences(entity);
        return repository.save(entity).getId();
    }

    public void update(Long id, Command c) {
        SurveyImage entity = find(id);
        entity.update(c.mood(), c.spotId(), c.imageUrl(), c.sortOrder());
        validateReferences(entity);
        repository.save(entity);
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    public void reorder(List<Long> ids) {
        List<SurveyImage> entities = repository.findAllByOrderBySortOrderAscIdAsc();
        if (ids == null || ids.stream().anyMatch(Objects::isNull) || ids.size() != entities.size() || new HashSet<>(ids).size() != ids.size()
                || !new HashSet<>(ids).equals(new HashSet<>(entities.stream().map(SurveyImage::getId).toList()))) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        for (SurveyImage entity : entities) {
            entity.reorder(ids.indexOf(entity.getId()));
            repository.save(entity);
        }
    }

    private SurveyImage find(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateReferences(SurveyImage entity) {
        if (!spotReader.isAvailable(entity.getSpotId(), entity.getImageUrl())) throw new BusinessException(ErrorCode.SPOT_NOT_AVAILABLE);
    }

    private boolean available(SurveyImage entity) {
        return spotReader.isAvailable(entity.getSpotId(), entity.getImageUrl());
    }
}
