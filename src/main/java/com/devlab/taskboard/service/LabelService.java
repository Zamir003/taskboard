package com.devlab.taskboard.service;

import com.devlab.taskboard.dto.request.LabelRequest;
import com.devlab.taskboard.dto.response.LabelResponse;
import com.devlab.taskboard.entity.Label;
import com.devlab.taskboard.entity.User;
import com.devlab.taskboard.exception.DuplicateResourceException;
import com.devlab.taskboard.exception.ResourceNotFoundException;
import com.devlab.taskboard.mapper.LabelMapper;
import com.devlab.taskboard.repository.LabelRepository;
import com.devlab.taskboard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LabelService {

    private static final String DUPLICATE_NAME_MESSAGE = "Label with this name already exists";

    private final LabelRepository labelRepository;
    private final UserRepository userRepository;
    private final LabelMapper labelMapper;
    private final Clock clock;

    public LabelService(LabelRepository labelRepository,
                        UserRepository userRepository,
                        LabelMapper labelMapper,
                        Clock clock) {
        this.labelRepository = labelRepository;
        this.userRepository = userRepository;
        this.labelMapper = labelMapper;
        this.clock = clock;
    }

    public List<LabelResponse> list(Long ownerId) {
        return labelRepository.findAllByOwnerIdOrderByNameAsc(ownerId).stream()
                .map(labelMapper::toResponse)
                .toList();
    }

    public LabelResponse get(Long ownerId, Long labelId) {
        return labelMapper.toResponse(findOwnedLabel(ownerId, labelId));
    }

    @Transactional
    public LabelResponse create(Long ownerId, LabelRequest request) {
        String name = request.name().trim();

        if (labelRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, name)) {
            throw new DuplicateResourceException(DUPLICATE_NAME_MESSAGE);
        }

        // getReferenceById: User üçün SELECT atılmır, yalnız owner_id lazımdır.
        User owner = userRepository.getReferenceById(ownerId);
        Label saved = labelRepository.save(labelMapper.toEntity(owner, name));
        return labelMapper.toResponse(saved);
    }

    @Transactional
    public LabelResponse update(Long ownerId, Long labelId, LabelRequest request) {
        Label label = findOwnedLabel(ownerId, labelId);
        String name = request.name().trim();

        // IdNot: label-in özü yoxlamadan çıxarılır, ona görə "work" -- "Work" kimi dəyişiklik mümkündür.
        if (labelRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(ownerId, name, labelId)) {
            throw new DuplicateResourceException(DUPLICATE_NAME_MESSAGE);
        }

        label.setName(name);
        return labelMapper.toResponse(label);
    }

    @Transactional
    public void delete(Long ownerId, Long labelId) {
        Label label = findOwnedLabel(ownerId, labelId);
        label.markDeleted(Instant.now(clock));
    }

    private Label findOwnedLabel(Long ownerId, Long labelId) {
        return labelRepository.findByIdAndOwnerId(labelId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));
    }
}
