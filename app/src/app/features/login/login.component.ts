import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, UserRole } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(
    readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: (user) => { this.loading = false; this.router.navigateByUrl(this.redirectPath(user.role)); },
      error: () => { this.loading = false; this.error = 'The email or password is incorrect.'; }
    });
  }

  private redirectPath(role: UserRole): string {
    const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
    return redirectTo && this.auth.canAccessRoute(role, redirectTo) ? redirectTo : this.auth.defaultPath(role);
  }
}
