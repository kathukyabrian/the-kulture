package tech.kitucode.kulture.api.web.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CrewMemberResponse(
	UUID id,
	String displayName,
	String role,
	BigDecimal rating
) {
}
