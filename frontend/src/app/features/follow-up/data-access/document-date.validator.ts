import type {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export function brazilianDateToIso(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null;
  }

  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value.trim());

  if (!match) {
    return null;
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);

  const date = new Date(year, month - 1, day, 12);

  if (
    year < 1000 ||
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }

  return `${match[3]}-${match[2]}-${match[1]}`;
}

export const documentDateValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  if (!control.value) {
    return null;
  }

  const iso = brazilianDateToIso(control.value);

  if (!iso) {
    return {invalidDate: true};
  }

  const today = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date());

  const part = (type: string) =>
    today.find(item => item.type === type)?.value;

  const limit = `${part('year')}-${part('month')}-${part('day')}`;

  return iso > limit ? {futureDate: true} : null;
};
