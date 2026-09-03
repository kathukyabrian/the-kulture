import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import type { FeatureCollection, LineString, Point } from 'geojson';
import * as maplibregl from 'maplibre-gl';
import type { GeoJSONSource, LngLatBoundsLike, Map } from 'maplibre-gl';
import { RouteGeometry, RouteResponse, VehicleSummaryResponse } from '../../core/api.models';

@Component({ selector: 'app-kulture-map', standalone: true, imports: [CommonModule], template: '<div class="relative h-full min-h-[inherit] w-full"><div #container class="absolute inset-0"></div><div *ngIf="editable" class="absolute left-3 top-3 z-10 flex gap-2"><button type="button" (click)="undo()" [disabled]="!draft.length" class="rounded bg-surface px-3 py-2 text-xs text-ink shadow disabled:opacity-40">Undo last</button><button type="button" (click)="clear()" [disabled]="!draft.length" class="rounded bg-danger px-3 py-2 text-xs text-white shadow disabled:opacity-40">Start over</button></div><div class="absolute right-3 top-3 z-10 flex flex-col items-end gap-2"><button *ngIf="followedVehicle" type="button" (click)="resumeFollowing()" class="rounded bg-surface px-3 py-2 font-mono text-xs font-bold uppercase shadow" [ngClass]="followingVehicle ? \'text-primary\' : \'text-secondary\'">{{ followingVehicle ? \'Following \' : \'Follow \' }}{{ followedVehicle.name }}</button><button type="button" (click)="moveToCurrentLocation()" [disabled]="locating" class="rounded bg-surface px-3 py-2 font-mono text-xs font-bold uppercase text-secondary shadow disabled:opacity-60">{{ locating ? \'Locating…\' : \'My location\' }}</button><button type="button" (click)="resetToNairobi()" class="rounded bg-surface px-3 py-2 font-mono text-xs font-bold uppercase text-secondary shadow">Nairobi</button><p *ngIf="locationError" class="max-w-48 rounded bg-danger px-2 py-1 text-right text-xs text-white shadow">{{ locationError }}</p></div><div *ngIf="editable" class="absolute bottom-3 left-3 right-3 z-10 rounded bg-surface/95 p-3 text-center shadow"><p class="font-bold text-ink">{{ draft.length === 0 ? \'1. Click the route starting point\' : draft.length === 1 ? \'2. Click the destination\' : \'Route ready · click along roads to refine it\' }}</p><p class="mt-1 text-xs text-muted">{{ draft.length < 2 ? \'You can zoom and move the map before selecting.\' : draft.length + \' points selected. Use Undo last if you make a mistake.\' }}</p></div></div>', styles: [':host{display:block;min-height:inherit}'] })
export class KultureMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('container', { static: true }) container!: ElementRef<HTMLElement>;
  @Input() routes: RouteResponse[] = [];
  @Input() vehicles: VehicleSummaryResponse[] = [];
  @Input() geometry: RouteGeometry | null = null;
  @Input() waypoints: [number, number][] = [];
  @Input() editable = false;
  @Output() geometryChange = new EventEmitter<RouteGeometry | null>();
  @Output() waypointsChange = new EventEmitter<[number, number][]>();
  private map?: Map;
  private currentLocationMarker?: maplibregl.Marker;
  private pulseAnimationFrame?: number;
  private followedVehicleId: string | null = null;
  followingVehicle = false;
  draft: [number, number][] = [];
  locating = false;
  locationError = '';
  private viewportInitialized = false;

  ngAfterViewInit(): void {
    const configured = (window as Window & { KULTURE_MAP_STYLE_URL?: string }).KULTURE_MAP_STYLE_URL;
    maplibregl.setWorkerUrl(new URL('maplibre-gl-worker.mjs', document.baseURI).toString());
    const styleUrl = configured || new URL('map-styles/liberty.json', document.baseURI).toString();
    this.map = new maplibregl.Map({ container: this.container.nativeElement, style: styleUrl, center: [36.8219, -1.2921], zoom: 10.5 });
    this.map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'bottom-right');
    this.map.on('styledata', () => { this.installLayers(); this.sync(!this.viewportInitialized); });
    this.map.on('click', event => { if (!this.editable) return; this.draft.push([event.lngLat.lng, event.lngLat.lat]); this.emitDraft(); });
    this.map.on('dragstart', () => this.pauseFollowing());
    this.map.on('rotatestart', () => this.pauseFollowing());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['waypoints']) this.draft = this.waypoints.map(point => [...point] as [number, number]);
    const fitViewport = !this.viewportInitialized || (!this.editable && (!!changes['routes'] || !!changes['geometry'] || !!changes['waypoints']));
    this.sync(fitViewport);
  }
  ngOnDestroy(): void { if (this.pulseAnimationFrame !== undefined) cancelAnimationFrame(this.pulseAnimationFrame); this.currentLocationMarker?.remove(); this.map?.remove(); }
  undo(): void { this.draft.pop(); this.emitDraft(); }
  clear(): void { this.draft = []; this.emitDraft(); }
  get followedVehicle(): VehicleSummaryResponse | undefined { return this.vehicles.find(vehicle => vehicle.id === this.followedVehicleId); }
  resetToNairobi(): void { this.followedVehicleId = null; this.followingVehicle = false; this.map?.easeTo({ center: [36.8219, -1.2921], zoom: 12, bearing: 0, pitch: 0, duration: 500 }); this.viewportInitialized = true; }
  resumeFollowing(): void { if (!this.followedVehicle) return; this.followingVehicle = true; this.updateFollowCamera(); }
  moveToCurrentLocation(): void {
    if (!navigator.geolocation) { this.locationError = 'Location is unavailable on this device.'; return; }
    this.locating = true;
    this.locationError = '';
    navigator.geolocation.getCurrentPosition(position => {
      this.locating = false;
      const coordinates: [number, number] = [position.coords.longitude, position.coords.latitude];
      this.followedVehicleId = null;
      this.followingVehicle = false;
      this.currentLocationMarker?.remove();
      this.currentLocationMarker = new maplibregl.Marker({ color: '#a7ffb3' }).setLngLat(coordinates).addTo(this.map!);
      this.map?.easeTo({ center: coordinates, zoom: 15, duration: 700 });
      this.viewportInitialized = true;
    }, error => {
      this.locating = false;
      this.locationError = error.code === error.PERMISSION_DENIED ? 'Allow location access to use this button.' : 'Could not find your current location.';
    }, { enableHighAccuracy: true, timeout: 15000, maximumAge: 10000 });
  }

  private emitDraft(): void { this.geometry = this.draft.length > 1 ? { type: 'LineString', coordinates: [...this.draft] } : null; this.geometryChange.emit(this.geometry); this.waypointsChange.emit(this.draft.map(point => [...point] as [number, number])); this.sync(false); }
  private installLayers(): void {
    if (!this.map || this.map.getSource('routes')) return;
    this.map.addSource('routes', { type: 'geojson', data: this.routeCollection() });
    this.map.addLayer({ id: 'route-glow', type: 'line', source: 'routes', paint: { 'line-color': '#ecb2ff', 'line-width': 8, 'line-opacity': 0.22 } });
    this.map.addLayer({ id: 'route-lines', type: 'line', source: 'routes', paint: { 'line-color': '#ecb2ff', 'line-width': 4 } });
    this.map.addSource('draft-points', { type: 'geojson', data: this.draftPointCollection() });
    this.map.addLayer({ id: 'draft-point-circles', type: 'circle', source: 'draft-points', paint: { 'circle-radius': 11, 'circle-color': '#131313', 'circle-stroke-color': '#ecb2ff', 'circle-stroke-width': 3 } });
    this.map.addLayer({ id: 'draft-point-labels', type: 'symbol', source: 'draft-points', layout: { 'text-field': ['get', 'label'], 'text-size': 12 }, paint: { 'text-color': '#ffffff' } });
    this.map.addSource('vehicles', { type: 'geojson', data: this.vehicleCollection() });
    const statusColor: maplibregl.ExpressionSpecification = ['case', ['boolean', ['get', 'stale'], false], '#ffb44a', '#a7ffb3'];
    this.map.addLayer({ id: 'vehicle-pulse', type: 'circle', source: 'vehicles', paint: { 'circle-radius': 13, 'circle-color': statusColor, 'circle-opacity': 0.18 } });
    this.map.addLayer({ id: 'vehicle-points', type: 'circle', source: 'vehicles', paint: { 'circle-radius': 8, 'circle-color': statusColor, 'circle-stroke-color': '#131313', 'circle-stroke-width': 3 } });
    this.animateVehiclePulse();
    this.map.on('click', 'vehicle-points', event => {
      const feature = event.features?.[0];
      if (!feature || feature.geometry.type !== 'Point') return;
      this.followedVehicleId = String(feature.properties?.['id'] ?? '');
      this.followingVehicle = true;
      this.updateFollowCamera();
      new maplibregl.Popup().setLngLat(feature.geometry.coordinates as [number, number]).setHTML(`<strong>${feature.properties?.['name'] ?? 'Nganya'}</strong><br>${feature.properties?.['status'] ?? ''}`).addTo(this.map!);
    });
  }

  private sync(fitViewport = false): void {
    if (!this.map) return;
    (this.map.getSource('routes') as GeoJSONSource | undefined)?.setData(this.routeCollection());
    (this.map.getSource('draft-points') as GeoJSONSource | undefined)?.setData(this.draftPointCollection());
    (this.map.getSource('vehicles') as GeoJSONSource | undefined)?.setData(this.vehicleCollection());
    if (this.followingVehicle && this.followedVehicle?.status === 'ONLINE' && this.followedVehicle.latestLocation) { this.updateFollowCamera(); return; }
    if (!fitViewport) return;
    const points: [number, number][] = [...this.routes.flatMap(route => route.geometry?.coordinates ?? []), ...(this.geometry?.coordinates ?? []), ...this.vehicles.flatMap(vehicle => vehicle.status === 'ONLINE' && vehicle.latestLocation ? [[Number(vehicle.latestLocation.longitude), Number(vehicle.latestLocation.latitude)] as [number, number]] : [])];
    if (!points.length) {
      this.resetToNairobi();
    } else if (points.length === 1) {
      this.map.easeTo({ center: points[0], zoom: 15, duration: 500 });
    } else if (points.length > 1) {
      const bounds = points.reduce((value, point) => value.extend(point), new maplibregl.LngLatBounds(points[0], points[0]));
      this.map.fitBounds(bounds as LngLatBoundsLike, { padding: 55, maxZoom: 15, duration: 500 });
    }
    this.viewportInitialized = true;
  }

  private routeCollection(): FeatureCollection<LineString> { const geometries = [...this.routes.flatMap(route => route.geometry ? [{ geometry: route.geometry, name: route.name }] : []), ...(this.geometry ? [{ geometry: this.geometry, name: 'Draft route' }] : [])]; return { type: 'FeatureCollection', features: geometries.map(item => ({ type: 'Feature', properties: { name: item.name }, geometry: item.geometry })) }; }
  private draftPointCollection(): FeatureCollection<Point> { return { type: 'FeatureCollection', features: this.draft.map((coordinates, index) => ({ type: 'Feature', properties: { label: String(index + 1) }, geometry: { type: 'Point', coordinates } })) }; }
  private animateVehiclePulse(): void {
    if (!this.map?.getLayer('vehicle-pulse')) return;
    const phase = (performance.now() % 1800) / 1800;
    this.map.setPaintProperty('vehicle-pulse', 'circle-radius', 11 + phase * 6);
    this.map.setPaintProperty('vehicle-pulse', 'circle-opacity', 0.24 * (1 - phase));
    this.pulseAnimationFrame = requestAnimationFrame(() => this.animateVehiclePulse());
  }

  private pauseFollowing(): void { if (this.followingVehicle) this.followingVehicle = false; }

  private updateFollowCamera(): void {
    const location = this.followedVehicle?.latestLocation;
    if (!this.map || !location) return;
    const heading = Number(location.headingDegrees);
    const hasReliableHeading = location.headingDegrees != null && Number(location.speedKph) >= 3 && Number.isFinite(heading);
    const currentBearing = this.map.getBearing();
    const bearing = hasReliableHeading ? currentBearing + ((((heading - currentBearing) % 360) + 540) % 360 - 180) : currentBearing;
    this.map.easeTo({
      center: [Number(location.longitude), Number(location.latitude)],
      zoom: Math.max(this.map.getZoom(), 15.5),
      bearing,
      pitch: hasReliableHeading ? 45 : this.map.getPitch(),
      duration: 900
    });
    this.viewportInitialized = true;
  }

  private vehicleCollection(): FeatureCollection<Point> { return { type: 'FeatureCollection', features: this.vehicles.flatMap(vehicle => vehicle.status === 'ONLINE' && vehicle.latestLocation ? [{ type: 'Feature' as const, properties: { id: vehicle.id, name: vehicle.name, status: vehicle.status, stale: Date.now() - new Date(vehicle.latestLocation.recordedAt).getTime() > 60000 }, geometry: { type: 'Point' as const, coordinates: [Number(vehicle.latestLocation.longitude), Number(vehicle.latestLocation.latitude)] } }] : []) }; }
}
