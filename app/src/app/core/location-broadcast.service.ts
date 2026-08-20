import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { environment } from '../../environments/environment';

export interface LocationSample {
  sampleId: string;
  latitude: number;
  longitude: number;
  accuracyMeters: number | null;
  speedKph: number;
  headingDegrees: number | null;
  recordedAt: string;
}

interface QueuedBroadcast { vehicleId: string; sample: LocationSample; }
interface BatchResponse { accepted: string[]; duplicates: string[]; rejected: { sampleId: string; reason: string }[]; }

@Injectable({ providedIn: 'root' })
export class LocationBroadcastService {
  private readonly storageKey = 'kulture.location.queue';
  private readonly sessionKey = 'kulture.location.session';
  private readonly activeVehicleKey = 'kulture.location.activeVehicle';
  private readonly apiBaseUrl = environment.apiBaseUrl;
  private queue = this.readQueue();
  private flushing = false;
  private retryDelayMs = 2000;
  private retryTimer: number | null = null;
  private lastQueued: LocationSample | null = null;
  private watchId: number | null = null;
  readonly sharing = signal(false);
  readonly state = signal<'idle' | 'queued' | 'broadcasting' | 'offline' | 'unauthorized' | 'error'>('idle');
  readonly queueSize = signal(this.queue.length);

  constructor(private readonly http: HttpClient) {
    window.addEventListener('online', () => this.flush());
    if (this.queue.length) this.flush();
  }

  enqueue(vehicleId: string, position: GeolocationPosition): boolean {
    const sample: LocationSample = {
      sampleId: crypto.randomUUID(),
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      accuracyMeters: Number.isFinite(position.coords.accuracy) ? position.coords.accuracy : null,
      speedKph: Math.max(0, Math.min(160, Math.round((position.coords.speed ?? 0) * 3.6))),
      headingDegrees: position.coords.heading === null || !Number.isFinite(position.coords.heading) ? null : position.coords.heading,
      recordedAt: new Date(position.timestamp).toISOString()
    };
    if (!this.isUseful(sample)) return false;
    this.lastQueued = sample;
    this.queue.push({ vehicleId, sample });
    if (this.queue.length > 240) this.queue.splice(0, this.queue.length - 240);
    this.persist();
    this.state.set(navigator.onLine ? 'queued' : 'offline');
    this.scheduleFlush(0);
    return true;
  }

  start(vehicleId: string): boolean {
    if (!navigator.geolocation) return false;
    if (this.watchId !== null) {
      this.sharing.set(localStorage.getItem(this.activeVehicleKey) === vehicleId);
      return this.sharing();
    }
    localStorage.setItem(this.activeVehicleKey, vehicleId);
    this.watchId = navigator.geolocation.watchPosition(
      position => this.enqueue(vehicleId, position),
      error => {
        if (error.code === error.PERMISSION_DENIED) this.stop();
        else this.state.set('error');
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 10000 }
    );
    this.sharing.set(true);
    this.state.set('idle');
    return true;
  }

  restore(vehicleId: string): boolean {
    const activeVehicleId = localStorage.getItem(this.activeVehicleKey);
    if (activeVehicleId === vehicleId) return this.start(vehicleId);
    if (activeVehicleId || this.watchId !== null) this.stop();
    return false;
  }

  stop(): void {
    if (this.watchId !== null && navigator.geolocation) navigator.geolocation.clearWatch(this.watchId);
    this.watchId = null;
    localStorage.removeItem(this.activeVehicleKey);
    this.sharing.set(false);
    this.state.set('idle');
  }

  flush(): void {
    if (this.flushing || !this.queue.length) return;
    if (!navigator.onLine) { this.state.set('offline'); return; }
    const vehicleId = this.queue[0].vehicleId;
    const batch = this.queue.filter(item => item.vehicleId === vehicleId).slice(0, 20);
    this.flushing = true;
    this.http.post<BatchResponse>(`${this.apiBaseUrl}/crew/vehicles/${vehicleId}/locations`, {
      sessionId: this.sessionId(), samples: batch.map(item => item.sample)
    }).subscribe({
      next: response => {
        const completed = new Set([...response.accepted, ...response.duplicates, ...response.rejected.map(item => item.sampleId)]);
        this.queue = this.queue.filter(item => !completed.has(item.sample.sampleId));
        this.flushing = false;
        this.retryDelayMs = 2000;
        this.persist();
        this.state.set(response.accepted.length || response.duplicates.length ? 'broadcasting' : response.rejected.length ? 'error' : 'idle');
        if (this.queue.length) this.scheduleFlush(1000);
      },
      error: response => {
        this.flushing = false;
        if (response.status === 401 || response.status === 403) { this.state.set('unauthorized'); return; }
        this.state.set(navigator.onLine ? 'error' : 'offline');
        this.scheduleFlush(this.retryDelayMs + Math.round(Math.random() * 500));
        this.retryDelayMs = Math.min(60000, Math.round(this.retryDelayMs * 2.2));
      }
    });
  }

  clear(): void {
    this.queue = [];
    this.lastQueued = null;
    this.persist();
    this.state.set('idle');
    if (this.retryTimer !== null) window.clearTimeout(this.retryTimer);
    this.retryTimer = null;
  }

  private isUseful(sample: LocationSample): boolean {
    if (sample.latitude < -90 || sample.latitude > 90 || sample.longitude < -180 || sample.longitude > 180) return false;
    if (sample.accuracyMeters !== null && sample.accuracyMeters > 50) { this.state.set('error'); return false; }
    if (!this.lastQueued) return true;
    const elapsed = new Date(sample.recordedAt).getTime() - new Date(this.lastQueued.recordedAt).getTime();
    return elapsed >= 30000 || this.distanceMetres(this.lastQueued, sample) >= 25;
  }

  private distanceMetres(a: LocationSample, b: LocationSample): number {
    const radians = (value: number) => value * Math.PI / 180;
    const dLat = radians(b.latitude - a.latitude), dLon = radians(b.longitude - a.longitude);
    const value = Math.sin(dLat / 2) ** 2 + Math.cos(radians(a.latitude)) * Math.cos(radians(b.latitude)) * Math.sin(dLon / 2) ** 2;
    return 6371000 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
  }

  private scheduleFlush(delayMs: number): void {
    if (this.retryTimer !== null) window.clearTimeout(this.retryTimer);
    this.retryTimer = window.setTimeout(() => { this.retryTimer = null; this.flush(); }, delayMs);
  }

  private persist(): void { localStorage.setItem(this.storageKey, JSON.stringify(this.queue)); this.queueSize.set(this.queue.length); }
  private readQueue(): QueuedBroadcast[] { try { return JSON.parse(localStorage.getItem(this.storageKey) ?? '[]') as QueuedBroadcast[]; } catch { return []; } }
  private sessionId(): string { const existing = sessionStorage.getItem(this.sessionKey); if (existing) return existing; const value = crypto.randomUUID(); sessionStorage.setItem(this.sessionKey, value); return value; }
}
