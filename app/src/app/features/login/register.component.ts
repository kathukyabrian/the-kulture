import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({ selector: 'app-register', standalone: true, imports: [CommonModule, FormsModule, RouterLink], template: `<main class="grid min-h-dvh place-items-center bg-background p-5 text-ink"><form (ngSubmit)="submit()" class="w-full max-w-md rounded-lg bg-surface p-6 rim-light"><h1 class="font-display text-3xl font-black uppercase text-primary">Create traveller account</h1><div class="mt-6 grid gap-3"><input name="name" [(ngModel)]="name" required placeholder="Full name" class="h-12 rounded bg-background px-4"/><input name="email" [(ngModel)]="email" required type="email" placeholder="Email" class="h-12 rounded bg-background px-4"/><input name="phone" [(ngModel)]="phoneNumber" required placeholder="Mobile number" class="h-12 rounded bg-background px-4"/><input name="password" [(ngModel)]="password" required minlength="8" type="password" placeholder="Password (8+ characters)" class="h-12 rounded bg-background px-4"/></div><p *ngIf="error" class="mt-3 text-sm text-danger">{{ error }}</p><button class="mt-5 h-12 w-full rounded bg-primary font-bold text-[#31003f]">Create account</button><a routerLink="/login" class="mt-4 block text-center text-sm text-secondary">Back to sign in</a></form></main>` })
export class RegisterComponent {
  name = ''; email = ''; phoneNumber = ''; password = ''; error = '';
  constructor(private readonly auth: AuthService, private readonly router: Router) {}
  submit(): void { this.auth.register({ name: this.name, email: this.email, phoneNumber: this.phoneNumber, password: this.password }).subscribe({ next: () => this.router.navigate(['/login']), error: () => this.error = 'Could not create this account. Check your details.' }); }
}
