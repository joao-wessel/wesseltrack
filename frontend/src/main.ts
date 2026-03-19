import { registerLocaleData } from '@angular/common';
import { bootstrapApplication } from '@angular/platform-browser';
import localePt from '@angular/common/locales/pt';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { DEFAULT_RUNTIME_CONFIG, RUNTIME_CONFIG, RuntimeConfig } from './app/core/runtime-config';

registerLocaleData(localePt);

fetch('/app-config.json')
  .then(async (response) => {
    if (!response.ok) {
      return DEFAULT_RUNTIME_CONFIG;
    }

    return await response.json() as RuntimeConfig;
  })
  .catch(() => DEFAULT_RUNTIME_CONFIG)
  .then((runtimeConfig) =>
    bootstrapApplication(App, {
      ...appConfig,
      providers: [
        ...(appConfig.providers ?? []),
        { provide: RUNTIME_CONFIG, useValue: runtimeConfig }
      ]
    })
  )
  .catch((err) => console.error(err));
