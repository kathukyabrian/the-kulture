import { Injectable, signal } from '@angular/core';

export interface ConfirmationPrompt {
  title: string;
  message: string;
  confirmLabel: string;
}

@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  private readonly promptState = signal<ConfirmationPrompt | null>(null);
  private resolver: ((confirmed: boolean) => void) | null = null;

  readonly prompt = this.promptState.asReadonly();

  confirm(prompt: ConfirmationPrompt): Promise<boolean> {
    this.resolver?.(false);
    this.promptState.set(prompt);
    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
    });
  }

  respond(confirmed: boolean): void {
    const resolve = this.resolver;
    this.resolver = null;
    this.promptState.set(null);
    resolve?.(confirmed);
  }
}
