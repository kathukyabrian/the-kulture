package tech.kitucode.kulture.api.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.web.rest.dto.RouteCalculationRequest;
import tech.kitucode.kulture.api.web.rest.dto.RouteCalculationResponse;
import tools.jackson.databind.JsonNode;

@Service
public class OpenRouteService {
	private final RestClient client = RestClient.create();
	private final String baseUrl;
	private final String apiKey;

	public OpenRouteService(
		@Value("${APP_ROUTING_ORS_BASE_URL:https://api.openrouteservice.org}") String baseUrl,
		@Value("${APP_ROUTING_ORS_API_KEY:}") String apiKey
	) {
		this.baseUrl = baseUrl.replaceAll("/+$", "");
		this.apiKey = apiKey.trim();
	}

	public RouteCalculationResponse calculate(RouteCalculationRequest request) {
		if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Road routing is not configured");
		List<RouteCalculationRequest.Coordinate> waypoints = request == null ? null : request.waypoints();
		if (waypoints == null || waypoints.size() < 2 || waypoints.size() > 25) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide between 2 and 25 route waypoints");
		for (RouteCalculationRequest.Coordinate waypoint : waypoints) validate(waypoint);
		List<List<Double>> coordinates = waypoints.stream().map(point -> List.of(point.longitude().doubleValue(), point.latitude().doubleValue())).toList();

		try {
			JsonNode response = client.post()
				.uri(baseUrl + "/v2/directions/driving-car/geojson")
				.header("Authorization", apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("coordinates", coordinates))
				.retrieve()
				.body(JsonNode.class);
			JsonNode feature = response == null ? null : response.path("features").path(0);
			if (feature == null || feature.isMissingNode() || !"LineString".equals(feature.path("geometry").path("type").asText())) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No road route was found");
			}
			JsonNode summary = feature.path("properties").path("summary");
			return new RouteCalculationResponse(feature.path("geometry"), summary.path("distance").asDouble(), summary.path("duration").asDouble());
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Road routing credentials were rejected", exception);
			if (exception.getStatusCode().is4xxClientError()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No road route was found for those points", exception);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Road routing service is unavailable", exception);
		} catch (RestClientException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Road routing service is unavailable", exception);
		}
	}

	private void validate(RouteCalculationRequest.Coordinate point) {
		if (point == null || point.longitude() == null || point.latitude() == null
			|| point.longitude().doubleValue() < -180 || point.longitude().doubleValue() > 180
			|| point.latitude().doubleValue() < -90 || point.latitude().doubleValue() > 90) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each waypoint must contain valid longitude and latitude");
		}
	}
}
