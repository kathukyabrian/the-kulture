package tech.kitucode.kulture.api.web.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.kitucode.kulture.api.service.VehicleLocationBroadcaster;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleLocationEventResource {
	private final VehicleLocationBroadcaster broadcaster;
	public VehicleLocationEventResource(VehicleLocationBroadcaster broadcaster) { this.broadcaster = broadcaster; }
	@GetMapping(path = "/location-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter events() { return broadcaster.subscribe(); }
}
