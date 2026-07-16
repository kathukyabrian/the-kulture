package tech.kitucode.kulture.api.web.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.VehicleService;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleSummaryResponse;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleResource {

	private final VehicleService vehicleService;

	public VehicleResource(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	@GetMapping
	public List<VehicleSummaryResponse> list() {
		return vehicleService.list();
	}

	@GetMapping("/search")
	public List<VehicleSummaryResponse> search(@RequestParam String q) {
		return vehicleService.search(q);
	}

	@GetMapping("/nearby")
	public List<VehicleSummaryResponse> nearby(@RequestParam BigDecimal lat, @RequestParam BigDecimal lng) {
		return vehicleService.nearby(lat, lng);
	}

	@GetMapping("/{id}")
	public VehicleDetailResponse get(@PathVariable UUID id) {
		return vehicleService.get(id);
	}
}
