import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function normalizeCpf(value: string): string {
  return value.trim().replace(/[.-]/g, '');
}

export const cpfValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: unknown = control.value;

  if (value === null || value === undefined || value === '') {
    return null;
  }

  if (typeof value !== 'string') {
    return {cpf: true};
  }

  const trimmed = value.trim();

  if (!/^(?:[0-9]{11}|[0-9]{3}\.[0-9]{3}\.[0-9]{3}-[0-9]{2})$/.test(trimmed)) {
    return {cpf: true};
  }

  const digits = normalizeCpf(trimmed);

  if (/^([0-9])\1{10}$/.test(digits)) {
    return {cpf: true};
  }

  const checkDigit = (length: number): number => {
    let total = 0;

    for (let i = 0; i < length; i++) {
      total += Number(digits[i]) * (length + 1 - i);
    }

    const result = (total * 10) % 11;

    return result === 10 ? 0 : result;
  };

  return checkDigit(9) === Number(digits[9]) && checkDigit(10) === Number(digits[10]) ? null : {cpf: true};
};
