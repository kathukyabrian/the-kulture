import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import type { FeatureCollection, LineString, Point } from 'geojson';
import * as maplibregl from 'maplibre-gl';
import type { GeoJSONSource, LngLatBoundsLike, Map } from 'maplibre-gl';
import { RouteGeometry, RouteResponse, VehicleSummaryResponse } from '../../core/api.models';

@Component({ selector: 'app-kulture-map', standalone: true, imports: [CommonModule], template: '<div class="relative h-full min-h-[inherit] w-full"><div #container class="absolute inset-0"></div><div *ngIf="editable" class="absolute left-3 top-3 z-10 flex gap-2"><button type="button" (click)="undo()" class="rounded bg-surface px-3 py-2 text-xs text-ink shadow">Undo</button><button type="button" (click)="clear()" class="rounded bg-danger px-3 py-2 text-xs text-white shadow">Clear</button></div><button type="button" (click)="resetToNairobi()" class="absolute right-3 top-3 z-10 rounded bg-surface px-3 py-2 font-mono text-xs font-bold uppercase text-secondary shadow">Nairobi</button><p *ngIf="editable" class="absolute bottom-2 left-2 right-2 z-10 rounded bg-surface/90 p-2 text-center text-xs text-muted">Click the map to add route points. Add at least two points.</p></div>', styles: [':host{display:block;min-height:inherit}'] })
export class KultureMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('container', { static: true }) container!: ElementRef<HTMLElement>;
  @Input() routes: RouteResponse[] = [];
  @Input() vehicles: VehicleSummaryResponse[] = [];
  @Input() geometry: RouteGeometry | null = null;
  @Input() editable = false;
  @Output() geometryChange = new EventEmitter<RouteGeometry | null>();
  private map?: Map;
  private draft: [number, number][] = [];
  private viewportInitialized = false;

  ngAfterViewInit(): void {
    const configured = (window as Window & { KULTURE_MAP_STYLE_URL?: string }).KULTURE_MAP_STYLE_URL;
    maplibregl.setWorkerUrl(new URL('maplibre-gl-worker.mjs', document.baseURI).toString());
    this.map = new maplibregl.Map({ container: this.container.nativeElement, style: configured || 'https://tiles.openfreemap.org/styles/liberty', center: [36.8219, -1.2921], zoom: 10.5 });
    this.map.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'bottom-right');
    this.map.on('style.load', () => { this.installLayers(); this.sync(true); });
    this.map.on('click', event => { if (!this.editable) return; this.draft.push([event.lngLat.lng, event.lngLat.lat]); this.geometry = this.draft.length > 1 ? { type: 'LineString', coordinates: [...this.draft] } : null; this.geometryChange.emit(this.geometry); this.sync(true); });
  }

  ngOnChanges(changes: SimpleChanges): void { if (changes['geometry']) this.draft = this.geometry?.coordinates.map(point => [...point] as [number, number]) ?? []; this.sync(!this.viewportInitialized || !!changes['routes'] || !!changes['geometry']); }
  ngOnDestroy(): void { this.map?.remove(); }
  undo(): void { this.draft.pop(); this.emitDraft(); }
  clear(): void { this.draft = []; this.emitDraft(); }
  resetToNairobi(): void { this.map?.easeTo({ center: [36.8219, -1.2921], zoom: 12, duration: 500 }); this.viewportInitialized = true; }

  private emitDraft(): void { this.geometry = this.draft.length > 1 ? { type: 'LineString', coordinates: [...this.draft] } : null; this.geometryChange.emit(this.geometry); this.sync(true); }
  private installLayers(): void {
    if (!this.map || this.map.getSource('routes')) return;
    this.map.addSource('routes', { type: 'geojson', data: this.routeCollection() });
    this.map.addLayer({ id: 'route-glow', type: 'line', source: 'routes', paint: { 'line-color': '#ecb2ff', 'line-width': 8, 'line-opacity': 0.22 } });
    this.map.addLayer({ id: 'route-lines', type: 'line', source: 'routes', paint: { 'line-color': '#ecb2ff', 'line-width': 4 } });
    this.map.addSource('vehicles', { type: 'geojson', data: this.vehicleCollection() });
    this.map.addLayer({ id: 'vehicle-points', type: 'circle', source: 'vehicles', paint: { 'circle-radius': 9, 'circle-color': '#a7ffb3', 'circle-stroke-color': '#131313', 'circle-stroke-width': 3 } });
    this.map.on('click', 'vehicle-points', event => { const feature = event.features?.[0]; if (!feature || feature.geometry.type !== 'Point') return; new maplibregl.Popup().setLngLat(feature.geometry.coordinates as [number, number]).setHTML(`<strong>${feature.properties?.['name'] ?? 'Nganya'}</strong><br>${feature.properties?.['status'] ?? ''}`).addTo(this.map!); });
  }

  private sync(fitViewport = false): void {
    if (!this.map?.isStyleLoaded()) return;
    (this.map.getSource('routes') as GeoJSONSource | undefined)?.setData(this.routeCollection());
    (this.map.getSource('vehicles') as GeoJSONSource | undefined)?.setData(this.vehicleCollection());
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
  private vehicleCollection(): FeatureCollection<Point> { return { type: 'FeatureCollection', features: this.vehicles.flatMap(vehicle => vehicle.status === 'ONLINE' && vehicle.latestLocation ? [{ type: 'Feature' as const, properties: { id: vehicle.id, name: vehicle.name, status: vehicle.status }, geometry: { type: 'Point' as const, coordinates: [Number(vehicle.latestLocation.longitude), Number(vehicle.latestLocation.latitude)] } }] : []) }; }
}
