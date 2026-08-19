package tech.kitucode.kulture.api.web.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.kitucode.kulture.api.service.MediaService;
import tech.kitucode.kulture.api.service.MediaStorageService;
import tech.kitucode.kulture.api.web.rest.dto.MediaResponse;

@RestController
public class MediaResource {
	private final MediaService service;
	private final MediaStorageService storageService;
	public MediaResource(MediaService service, MediaStorageService storageService) {
		this.service = service;
		this.storageService = storageService;
	}

	@GetMapping("/api/vehicles/{vehicleId}/images")
	public List<MediaResponse> images(@PathVariable UUID vehicleId) { return service.publicImages(vehicleId); }

	@GetMapping("/api/media/{imageId}/content")
	public ResponseEntity<Resource> content(@PathVariable UUID imageId) {
		var image = service.content(imageId);
		return ResponseEntity.ok().cacheControl(CacheControl.noCache()).contentType(MediaType.parseMediaType(image.getContentType())).contentLength(image.getSize()).body(storageService.load(image.getStorageKey()));
	}
}
