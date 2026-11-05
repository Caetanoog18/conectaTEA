import {FormControl} from '@angular/forms';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {brazilianDateToIso, documentDateValidator} from './document-date.validator';

describe('Medical document dates', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('should convert Brazilian dates without changing the calendar day', () => {
    expect(brazilianDateToIso('02/11/2026')).toBe('2026-11-02');
  });

  it.each([
    '31/02/2026',
    '29/02/2026',
    '2026-11-02',
    '11/23/2026',
    '01/13/2026',
    '00/01/2026'
  ])('should reject invalid date %s', value => {
    expect(brazilianDateToIso(value)).toBeNull();
  });

  it('should accept a leap day', () => {
    expect(brazilianDateToIso('29/02/2024')).toBe('2024-02-29');
  });

  it('should use the Brazilian calendar day when rejecting future dates', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-11-03T01:00:00Z'));

    const control = new FormControl('03/11/2026', documentDateValidator);

    expect(control.hasError('futureDate')).toBe(true);

    control.setValue('02/11/2026');

    expect(control.valid).toBe(true);
  });
});
