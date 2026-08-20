import { Component, OnDestroy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { App } from '@capacitor/app';
import type { PluginListenerHandle } from '@capacitor/core';
import { ConfirmationDialogComponent } from './shared/confirmation-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ConfirmationDialogComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnDestroy {
  private readonly backButtonListener: Promise<PluginListenerHandle>;

  constructor() {
    this.backButtonListener = App.addListener('backButton', ({ canGoBack }) => {
      if (canGoBack) {
        window.history.back();
        return;
      }

      void App.exitApp();
    });
  }

  ngOnDestroy(): void {
    void this.backButtonListener.then((listener) => listener.remove());
  }
}
