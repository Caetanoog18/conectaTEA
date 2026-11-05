import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {describe, expect, it} from 'vitest';
import {AccessDenied} from './access-denied';

describe('AccessDenied', () => {
  it('should explain the restriction and link to the dashboard', async () => {
    await TestBed.configureTestingModule({
      imports: [AccessDenied],
      providers: [provideRouter([])]
    }).compileComponents();

    const fixture = TestBed.createComponent(AccessDenied);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('Acesso não permitido');
    expect(element.querySelector('a[href="/dashboard"]')).not.toBeNull();
  });
});
