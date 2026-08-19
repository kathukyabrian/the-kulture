package tech.kitucode.kulture.api.web.rest.dto;

import java.util.UUID;
public record AssignCrewRequest(UUID userId, String role, boolean confirmMove) {}
