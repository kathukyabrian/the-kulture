import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthUserResponse } from './api.models';

export type UserRole = 'admin' | 'crew' | 'traveller';
export type AuthUser = AuthUserResponse;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'kulture.session';
  private readonly session = signal<AuthUser | null>(this.readSession());
  readonly user = this.session.asReadonly();
  readonly signedIn = computed(() => this.session() !== null);

  constructor(private readonly http: HttpClient) {}
  login(email: string, password: string): Observable<AuthUser> { return this.http.post<AuthUser>('/api/auth/login', { email, password }).pipe(tap((user) => this.store(user))); }
  register(request: { name: string; email: string; phoneNumber: string; password: string }): Observable<AuthUser> { return this.http.post<AuthUser>('/api/auth/register', request); }
  setupPassword(token: string, password: string): Observable<void> { return this.http.post<void>('/api/auth/password/setup', { token, password }); }
  logout(): void { this.http.post<void>('/api/auth/logout', {}).subscribe(); this.session.set(null); localStorage.removeItem(this.storageKey); }
  defaultPath(role: UserRole): string { return ({ admin: '/fleet', crew: '/crew', traveller: '/' } as Record<UserRole, string>)[role]; }
  canAccess(roles: UserRole[]): boolean { const user = this.session(); return !!user && roles.includes(user.role); }
  canAccessRoute(role: UserRole, path: string): boolean { if (path.startsWith('/fleet')) return role === 'admin'; if (path.startsWith('/crew')) return role === 'crew'; if (path === '/' || path.startsWith('/nganyas')) return role === 'traveller'; return false; }
  private store(user: AuthUser): void { this.session.set(user); localStorage.setItem(this.storageKey, JSON.stringify(user)); }
  private readSession(): AuthUser | null { const stored = localStorage.getItem(this.storageKey); if (!stored) return null; try { const user = JSON.parse(stored) as AuthUser; if (user.id && user.email && this.isKnownRole(user.role)) return user; localStorage.removeItem(this.storageKey); return null; } catch { localStorage.removeItem(this.storageKey); return null; } }
  private isKnownRole(role: string): role is UserRole { return role === 'admin' || role === 'crew' || role === 'traveller'; }
}
