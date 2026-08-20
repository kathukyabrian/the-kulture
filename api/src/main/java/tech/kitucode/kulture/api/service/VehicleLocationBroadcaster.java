package tech.kitucode.kulture.api.service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.kitucode.kulture.api.web.rest.dto.VehicleLocationEvent;

@Component
public class VehicleLocationBroadcaster {
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "vehicle-location-sse-heartbeat");
		thread.setDaemon(true);
		return thread;
	});

	public VehicleLocationBroadcaster() {
		heartbeat.scheduleAtFixedRate(this::sendHeartbeat, 20, 20, TimeUnit.SECONDS);
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(0L);
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(error -> emitters.remove(emitter));
		try { emitter.send(SseEmitter.event().comment("connected")); } catch (IOException error) { remove(emitter); }
		return emitter;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void broadcast(VehicleLocationEvent event) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().id(event.sampleId().toString()).name("vehicle-location").data(event));
			} catch (IOException | IllegalStateException error) {
				remove(emitter);
			}
		}
	}

	private void sendHeartbeat() {
		for (SseEmitter emitter : emitters) {
			try { emitter.send(SseEmitter.event().comment("keepalive")); } catch (IOException | IllegalStateException error) { remove(emitter); }
		}
	}

	private void remove(SseEmitter emitter) {
		emitters.remove(emitter);
		try { emitter.complete(); } catch (IllegalStateException ignored) {}
	}

	@PreDestroy
	void stop() { heartbeat.shutdownNow(); }
}
