package tech.kitucode.kulture.api.web.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String phoneNumber, String role, String status,
	UUID assignmentId, UUID vehicleId, String vehicleName, String assignmentRole, Instant createdAt) {}
