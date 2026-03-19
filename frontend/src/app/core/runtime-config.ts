import { InjectionToken } from '@angular/core';

export interface RuntimeConfig {
  apiBaseUrl: string;
}

export const DEFAULT_RUNTIME_CONFIG: RuntimeConfig = {
  apiBaseUrl: 'http://localhost:8080/api'
};

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('RUNTIME_CONFIG');
