import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { ConfirmationService } from '../core/confirmation.service';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirmation-dialog.component.html'
})
export class ConfirmationDialogComponent {
  constructor(readonly confirmation: ConfirmationService) {}

  @HostListener('document:keydown.escape')
  cancel(): void {
    if (this.confirmation.prompt()) this.confirmation.respond(false);
  }
}
