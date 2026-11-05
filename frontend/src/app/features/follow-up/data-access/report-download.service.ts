import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ReportDownloadService {
  save(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = 'conectatea-relatorio.pdf';

    document.body.appendChild(link);

    try {
      link.click();
    } finally {
      link.remove();

      window.setTimeout(() => {
        URL.revokeObjectURL(url);
      }, 1000);
    }
  }
}
