package tech.kitucode.kulture.api.web.rest.dto;
import java.time.Instant;
import java.util.UUID;
public record CrewAssignmentContextResponse(UUID id, String role, Instant startedAt) {}
