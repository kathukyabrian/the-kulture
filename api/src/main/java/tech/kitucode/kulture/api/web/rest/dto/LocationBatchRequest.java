package tech.kitucode.kulture.api.web.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record LocationBatchRequest(
	@NotNull UUID sessionId,
	@NotNull @Size(min = 1, max = 20) List<@Valid LocationSampleRequest> samples
) {}
