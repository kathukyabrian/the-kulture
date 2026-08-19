package tech.kitucode.kulture.api.web.rest.dto;

import java.math.BigDecimal;

public record CrewAdminRequest(String displayName, String role, BigDecimal rating) {
}
