import {ChangeDetectionStrategy, Component} from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-access-denied',
  imports: [RouterLink],
  templateUrl: './access-denied.html',
  styleUrl: '../../../../shared/styles/management.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AccessDenied {}
