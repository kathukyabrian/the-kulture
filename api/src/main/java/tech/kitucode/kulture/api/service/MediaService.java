package tech.kitucode.kulture.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.Media;
import tech.kitucode.kulture.api.repository.MediaRepository;
import tech.kitucode.kulture.api.web.rest.dto.MediaResponse;

@Service
@Transactional(readOnly = true)
public class MediaService {
	private static final long MAX_SIZE = 8 * 1024 * 1024;
	private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
	private final MediaRepository repository;
	private final VehicleService vehicleService;
	private final MediaStorageService storageService;

	public MediaService(MediaRepository repository, VehicleService vehicleService, MediaStorageService storageService) {
		this.repository = repository;
		this.vehicleService = vehicleService;
		this.storageService = storageService;
	}

	public List<MediaResponse> publicImages(UUID vehicleId) {
		vehicleService.get(vehicleId);
		return repository.findByVehicleIdAndApprovedTrueOrderBySortOrderAscCreatedAtAsc(vehicleId).stream().map(this::response).toList();
	}

	public List<MediaResponse> adminImages(UUID vehicleId) {
		vehicleService.getForAdmin(vehicleId);
		return repository.findByVehicleIdOrderBySortOrderAscCreatedAtAsc(vehicleId).stream().map(this::response).toList();
	}

	@Transactional
	public MediaResponse upload(UUID vehicleId, MultipartFile file) {
		if (file.isEmpty() || file.getSize() > MAX_SIZE || !ALLOWED_TYPES.contains(file.getContentType())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a JPG, PNG or WebP image up to 8 MB");
		Media media = new Media();
		media.setId(UUID.randomUUID());
		media.setVehicle(vehicleService.findById(vehicleId));
		media.setOriginalName(file.getOriginalFilename() == null ? "nganya-image" : file.getOriginalFilename());
		media.setContentType(file.getContentType());
		media.setSize(file.getSize());
		media.setStorageKey(storageService.store(media.getId(), file));
		media.setSortOrder((int) repository.countByVehicleId(vehicleId));
		media.setApproved(true);
		media.setCreatedAt(Instant.now());
		try {
			return response(repository.save(media));
		} catch (RuntimeException ex) {
			storageService.delete(media.getStorageKey());
			throw ex;
		}
	}

	public Media content(UUID imageId) { return repository.findById(imageId).filter(Media::isApproved).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found")); }

	@Transactional
	public void delete(UUID vehicleId, UUID imageId) {
		Media image = repository.findById(imageId).filter(item -> item.getVehicle().getId().equals(vehicleId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
		repository.delete(image);
		storageService.delete(image.getStorageKey());
	}

	private MediaResponse response(Media image) { return new MediaResponse(image.getId(), image.getOriginalName(), image.getContentType(), image.getSize(), image.getSortOrder(), image.isApproved(), "/api/media/" + image.getId() + "/content"); }
}
