import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AccountRole, UserResponse, UserStatus } from '../../../core/api.models';
import { KultureApiService } from '../../../core/kulture-api.service';

@Component({ selector: 'app-admin-users-page', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './admin-users-page.component.html' })
export class AdminUsersPageComponent implements OnInit {
  users: UserResponse[] = []; query = ''; role: AccountRole | '' = ''; status: UserStatus | '' = ''; assignmentRole = ''; page = 1; pageCount = 1; total = 0; loading = false; error = '';
  constructor(private readonly api: KultureApiService) {}
  ngOnInit(): void { this.load(); }
  load(reset = false): void { if (reset) this.page = 1; this.loading = true; this.error = ''; this.api.getAdminUsers(this.query, this.role, this.status, this.assignmentRole, this.page - 1, 12).subscribe({ next: (result) => { this.users = result.items; this.total = result.totalItems; this.pageCount = Math.max(1, result.totalPages); this.page = result.page + 1; this.loading = false; }, error: () => { this.error = 'Could not load users.'; this.loading = false; } }); }
  changePage(page: number): void { this.page = Math.min(Math.max(1, page), this.pageCount); this.load(); }
  toggleStatus(user: UserResponse): void { const status: UserStatus = user.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'; this.api.updateUserStatus(user.id, status).subscribe({ next: () => this.load(), error: () => this.error = 'Could not update this user.' }); }
  resend(user: UserResponse): void { this.api.resendInvitation(user.id).subscribe({ next: () => this.error = '', error: () => this.error = 'Could not resend the invitation.' }); }
}
