import {FormControl, FormGroup, Validators} from '@angular/forms';
import {describe, expect, it} from 'vitest';

import {
  parseLocalDateTime,
  pastDateTimeValidator,
  reportPeriodValidator
} from './record-date.validators';

describe('parseLocalDateTime', () => {
  it('should interpret dates in Brazilian format', () => {
    const date = parseLocalDateTime('02/11/2026 14:30');

    expect(date).not.toBeNull();
    expect(date?.getDate()).toBe(2);
    expect(date?.getMonth()).toBe(10);
    expect(date?.getFullYear()).toBe(2026);
    expect(date?.getHours()).toBe(14);
    expect(date?.getMinutes()).toBe(30);
  });

  it('should reject a nonexistent date', () => {
    expect(parseLocalDateTime('31/02/2026 14:30')).toBeNull();
  });

  it('should accept February 29 in a leap year', () => {
    expect(parseLocalDateTime('29/02/2024 14:30')).not.toBeNull();
  });

  it('should reject February 29 outside a leap year', () => {
    expect(parseLocalDateTime('29/02/2026 14:30')).toBeNull();
  });

  it('should reject invalid hours and minutes', () => {
    expect(parseLocalDateTime('02/11/2026 24:00')).toBeNull();
    expect(parseLocalDateTime('02/11/2026 14:60')).toBeNull();
  });

  it('should reject American and ISO input formats', () => {
    expect(parseLocalDateTime('11/23/2026 14:30')).toBeNull();
    expect(parseLocalDateTime('2026-11-02T14:30')).toBeNull();
  });
});

describe('pastDateTimeValidator', () => {
  it('should accept a past date in Brazilian format', () => {
    const control = new FormControl('01/01/2020 12:00', {
      nonNullable: true,
      validators: [Validators.required, pastDateTimeValidator]
    });

    expect(control.valid).toBe(true);
  });

  it('should reject a future date', () => {
    const nextYear = new Date().getFullYear() + 1;

    const control = new FormControl(`01/01/${nextYear} 12:00`, {
      nonNullable: true,
      validators: [pastDateTimeValidator]
    });

    expect(control.hasError('futureDate')).toBe(true);
  });

  it('should reject an invalid date', () => {
    const control = new FormControl('31/02/2026 12:00', {
      nonNullable: true,
      validators: [pastDateTimeValidator]
    });

    expect(control.hasError('invalidDate')).toBe(true);
  });

  it('should leave empty values to the required validator', () => {
    const control = new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, pastDateTimeValidator]
    });

    expect(control.hasError('required')).toBe(true);
    expect(control.hasError('invalidDate')).toBe(false);
  });
});

describe('reportPeriodValidator', () => {
  function createPeriod(from: string, to: string) {
    return new FormGroup({
      from: new FormControl(from, {
        nonNullable: true,
        validators: [Validators.required]
      }),
      to: new FormControl(to, {
        nonNullable: true,
        validators: [Validators.required]
      })
    }, {
      validators: [reportPeriodValidator]
    });
  }

  it('should accept a valid period in Brazilian format', () => {
    const form = createPeriod(
      '01/01/2020 00:00',
      '01/02/2020 00:00'
    );

    expect(form.valid).toBe(true);
  });

  it('should reject a period ending before its start', () => {
    const form = createPeriod(
      '02/11/2026 00:00',
      '01/11/2026 00:00'
    );

    expect(form.hasError('invalidPeriod')).toBe(true);
  });

  it('should reject identical start and end dates', () => {
    const form = createPeriod(
      '02/11/2026 14:30',
      '02/11/2026 14:30'
    );

    expect(form.hasError('invalidPeriod')).toBe(true);
  });

  it('should reject invalid dates', () => {
    const form = createPeriod(
      '31/02/2026 00:00',
      '01/03/2026 00:00'
    );

    expect(form.hasError('invalidPeriod')).toBe(true);
  });

  it('should reject periods longer than 366 days', () => {
    const form = createPeriod(
      '01/01/2020 00:00',
      '01/01/2022 00:00'
    );

    expect(form.hasError('periodTooLong')).toBe(true);
  });
});
