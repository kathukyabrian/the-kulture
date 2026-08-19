package tech.kitucode.kulture.api.web.rest.dto;
import jakarta.validation.constraints.NotBlank;
public record OccupancyUpdateRequest(@NotBlank String occupancyStatus) {}
