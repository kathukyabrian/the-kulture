package tech.kitucode.kulture.api.web.rest.dto;

import java.util.UUID;

public record MediaResponse(UUID id, String originalName, String contentType, long size, int sortOrder, boolean approved, String url) {
}
