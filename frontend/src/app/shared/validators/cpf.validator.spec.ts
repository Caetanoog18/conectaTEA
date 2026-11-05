import {FormControl, Validators} from '@angular/forms';
import {describe, expect, it} from 'vitest';

import {cpfValidator, normalizeCpf} from './cpf.validator';

describe('CPF validation', () => {
  it('should accept a formatted CPF', () => {
    const control = new FormControl('529.982.247-25', [Validators.required, cpfValidator]);

    expect(control.valid).toBe(true);
    expect(normalizeCpf(control.value!)).toBe('52998224725');
  });

  it('should preserve leading zeros', () => {
    const control = new FormControl('01234567890', [Validators.required, cpfValidator]);

    expect(control.valid).toBe(true);
    expect(normalizeCpf(control.value!)).toBe('01234567890');
  });

  it.each([
    '',
    ' ',
    '11111111111',
    '12345678900',
    '52998224724',
    '5299822472',
    'abc52998224725'
  ])('should reject invalid value %j', value => {
    const control = new FormControl(value, [Validators.required, cpfValidator]);

    expect(control.invalid).toBe(true);
  });
});
