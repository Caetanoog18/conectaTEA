import {Injectable} from '@angular/core';

@Injectable({providedIn: 'root'})
export class MedicalDocumentDownloadService {
  save(blob: Blob, documentId: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');

    anchor.href = url;
    anchor.download = `laudo-${documentId.replace(/[^a-zA-Z0-9-]/g, '')}.pdf`;

    document.body.appendChild(anchor);

    try {
      anchor.click();
    } finally {
      anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
  }
}
