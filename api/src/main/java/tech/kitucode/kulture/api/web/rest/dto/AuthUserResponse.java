package tech.kitucode.kulture.api.web.rest.dto;
import java.util.UUID;
public record AuthUserResponse(UUID id, String displayName, String email, String role) {}
