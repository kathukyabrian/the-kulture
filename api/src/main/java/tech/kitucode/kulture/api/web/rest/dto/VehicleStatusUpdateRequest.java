package tech.kitucode.kulture.api.web.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record VehicleStatusUpdateRequest(
	@NotBlank String status,
	@NotBlank String occupancyStatus
) {
}
