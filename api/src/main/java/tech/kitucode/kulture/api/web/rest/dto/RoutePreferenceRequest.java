package tech.kitucode.kulture.api.web.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoutePreferenceRequest(@NotNull UUID routeId) {}
