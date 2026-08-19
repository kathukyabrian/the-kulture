import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();

  if (!user) {
    return router.createUrlTree(['/login'], { queryParams: { redirectTo: state.url } });
  }

  const roles = (route.data['roles'] ?? []) as UserRole[];
  if (!roles.length || roles.includes(user.role)) {
    return true;
  }

  return router.createUrlTree([auth.defaultPath(user.role)]);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();
  return user ? router.createUrlTree([auth.defaultPath(user.role)]) : true;
};
