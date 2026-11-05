import type {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function parseLocalDateTime(value: unknown): Date | null {
  if (typeof value !== 'string') {
    return null;
  }

  const match = /^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/
    .exec(value.trim());

  if (!match) {
    return null;
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const hour = Number(match[4]);
  const minute = Number(match[5]);

  if (
    year < 1000 ||
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > 31 ||
    hour < 0 ||
    hour > 23 ||
    minute < 0 ||
    minute > 59
  ) {
    return null;
  }

  const date = new Date(
    year,
    month - 1,
    day,
    hour,
    minute,
    0,
    0
  );

  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day ||
    date.getHours() !== hour ||
    date.getMinutes() !== minute
  ) {
    return null;
  }

  return date;
}

export const pastDateTimeValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  if (!control.value) {
    return null;
  }

  const date = parseLocalDateTime(control.value);

  if (!date) {
    return {invalidDate: true};
  }

  return date.getTime() > Date.now()
    ? {futureDate: true}
    : null;
};

export const reportPeriodValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const fromValue = control.get('from')?.value;
  const toValue = control.get('to')?.value;

  if (!fromValue || !toValue) {
    return null;
  }

  const from = parseLocalDateTime(fromValue);
  const to = parseLocalDateTime(toValue);

  if (!from || !to) {
    return {invalidPeriod: true};
  }

  const duration = to.getTime() - from.getTime();

  if (duration <= 0) {
    return {invalidPeriod: true};
  }

  return duration > 366 * 24 * 60 * 60 * 1000
    ? {periodTooLong: true}
    : null;
};
