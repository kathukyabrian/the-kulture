import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({ selector: 'app-setup-password', standalone: true, imports: [CommonModule, FormsModule, RouterLink], template: `<main class="grid min-h-dvh place-items-center bg-background p-5 text-ink"><form (ngSubmit)="submit()" class="w-full max-w-md rounded-lg bg-surface p-6 rim-light"><h1 class="font-display text-3xl font-black uppercase text-primary">Set your password</h1><input name="password" [(ngModel)]="password" required minlength="8" type="password" placeholder="Password (8+ characters)" class="mt-6 h-12 w-full rounded bg-background px-4"/><p *ngIf="error" class="mt-3 text-sm text-danger">{{ error }}</p><button class="mt-5 h-12 w-full rounded bg-primary font-bold text-[#31003f]">Activate account</button><a routerLink="/login" class="mt-4 block text-center text-sm text-secondary">Back to sign in</a></form></main>` })
export class SetupPasswordComponent {
  password = ''; error = ''; private readonly token: string;
  constructor(route: ActivatedRoute, private readonly auth: AuthService, private readonly router: Router) { this.token = route.snapshot.queryParamMap.get('token') ?? ''; }
  submit(): void { if (!this.token) { this.error = 'This setup link is invalid.'; return; } this.auth.setupPassword(this.token, this.password).subscribe({ next: () => this.router.navigate(['/login']), error: () => this.error = 'This setup link is invalid or expired.' }); }
}
