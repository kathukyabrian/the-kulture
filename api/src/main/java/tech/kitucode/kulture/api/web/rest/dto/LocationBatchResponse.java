package tech.kitucode.kulture.api.web.rest.dto;

import java.util.List;
import java.util.UUID;

public record LocationBatchResponse(List<UUID> accepted, List<UUID> duplicates, List<LocationRejection> rejected) {
	public record LocationRejection(UUID sampleId, String reason) {}
}
