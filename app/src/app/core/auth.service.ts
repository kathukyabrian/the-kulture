import { Injectable, computed, signal } from '@angular/core';

export type UserRole = 'admin' | 'crew' | 'nganya';

export interface AuthUser {
  displayName: string;
  email: string;
  role: UserRole;
}

interface DemoAccount extends AuthUser {
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'kulture.session';
  private readonly session = signal<AuthUser | null>(this.readSession());

  readonly user = this.session.asReadonly();
  readonly signedIn = computed(() => this.session() !== null);
  private readonly accounts: readonly DemoAccount[] = [
    {
      displayName: 'Fleet Admin',
      email: 'admin@kulture.test',
      password: 'admin123',
      role: 'admin'
    },
    {
      displayName: 'Crew Lead',
      email: 'crew@kulture.test',
      password: 'crew123',
      role: 'crew'
    },
    {
      displayName: 'Nganya Rider',
      email: 'nganya@kulture.test',
      password: 'nganya123',
      role: 'nganya'
    }
  ];

  login(email: string, password: string): AuthUser | null {
    const account = this.accounts.find(
      (candidate) => candidate.email.toLowerCase() === email.trim().toLowerCase() && candidate.password === password
    );
    if (!account) {
      return null;
    }

    const user: AuthUser = {
      displayName: account.displayName,
      email: account.email,
      role: account.role
    };
    this.session.set(user);
    localStorage.setItem(this.storageKey, JSON.stringify(user));
    return user;
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(this.storageKey);
  }

  defaultPath(role: UserRole): string {
    const paths: Record<UserRole, string> = {
      admin: '/fleet',
      crew: '/crew',
      nganya: '/'
    };
    return paths[role];
  }

  canAccess(roles: UserRole[]): boolean {
    const user = this.session();
    return !!user && roles.includes(user.role);
  }

  canAccessRoute(role: UserRole, path: string): boolean {
    if (path.startsWith('/fleet')) {
      return role === 'admin';
    }
    if (path.startsWith('/crew')) {
      return role === 'crew';
    }
    if (path === '/' || path.startsWith('/nganyas')) {
      return role === 'nganya';
    }
    return false;
  }

  private readSession(): AuthUser | null {
    const stored = localStorage.getItem(this.storageKey);
    if (!stored) {
      return null;
    }

    try {
      const user = JSON.parse(stored) as AuthUser;
      return this.isKnownRole(user.role) ? user : null;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private isKnownRole(role: string): role is UserRole {
    return role === 'admin' || role === 'crew' || role === 'nganya';
  }
}
