import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { VehicleLocationEvent } from './api.models';

@Injectable({ providedIn: 'root' })
export class VehicleLocationEventsService {
  stream(): Observable<VehicleLocationEvent> {
    return new Observable(subscriber => {
      const source = new EventSource(`${environment.apiBaseUrl}/vehicles/location-events`, { withCredentials: true });
      const onLocation = (event: MessageEvent<string>) => {
        try { subscriber.next(JSON.parse(event.data) as VehicleLocationEvent); } catch (error) { console.warn('Ignored invalid vehicle location event', error); }
      };
      source.addEventListener('vehicle-location', onLocation as EventListener);
      source.onerror = () => { /* EventSource reconnects automatically; REST polling remains the fallback. */ };
      return () => source.close();
    });
  }
}
