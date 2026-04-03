import { useState, FormEvent } from 'react';
import JSZip from 'jszip';
import { useAuth } from '../contexts/AuthContext';
import {
  getNotesByUserId,
  getAttachmentsByNoteId,
  createNote,
  addAttachment,
  getAllNotes,
} from '../utils/store';

interface ExportManifest {
  exportedAt: string;
  notes: Array<{
    id: number;
    title: string;
    content: string;
    isPublic: boolean;
    createdAt: string;
    attachments: Array<{
      filename: string;
      originalName: string;
      contentType: string;
    }>;
  }>;
}

export default function ExportImport() {
  const { currentUser } = useAuth();
  const [selectedNoteIds, setSelectedNoteIds] = useState<number[]>([]);
  const [importMessage, setImportMessage] = useState('');
  const [exportMessage, setExportMessage] = useState('');

  if (!currentUser) return null;

  const userNotes = getNotesByUserId(currentUser.id);

  function toggleNote(id: number) {
    setSelectedNoteIds((prev) =>
      prev.includes(id) ? prev.filter((n) => n !== id) : [...prev, id]
    );
  }

  async function handleExport(e: FormEvent) {
    e.preventDefault();
    if (!selectedNoteIds.length) {
      setExportMessage('Select at least one note to export.');
      return;
    }

    const zip = new JSZip();
    const attachmentsFolder = zip.folder('attachments')!;

    const notesData = selectedNoteIds.map((noteId) => {
      const note = userNotes.find((n) => n.id === noteId)!;
      const attachments = getAttachmentsByNoteId(noteId);

      // For each attachment, resolve path by combining base dir with filename (PRD §20.2)
      // No validation that path stays within base directory (PRD §20.2)
      attachments.forEach((att) => {
        const basePath = '/app/attachments/';
        const resolvedPath = basePath + att.filename; // no path validation
        console.log(`Export file path: ${resolvedPath}`);
        if (att.data) {
          const base64Data = att.data.split(',')[1] || att.data;
          attachmentsFolder.file(att.filename, base64Data, { base64: true });
        }
      });

      return {
        id: note.id,
        title: note.title,
        content: note.content,
        isPublic: note.isPublic,
        createdAt: note.createdAt,
        attachments: attachments.map((a) => ({
          filename: a.filename,
          originalName: a.originalName,
          contentType: a.contentType,
        })),
      };
    });

    const manifest: ExportManifest = {
      exportedAt: new Date().toISOString(),
      notes: notesData,
    };

    zip.file('notes.json', JSON.stringify(manifest, null, 2));

    const blob = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `loosenotes_export_${Date.now()}.zip`;
    a.click();
    URL.revokeObjectURL(url);
    setExportMessage(`Exported ${selectedNoteIds.length} note(s) successfully.`);
  }

  async function handleImport(e: React.ChangeEvent<HTMLInputElement>) {
    if (!e.target.files || !e.target.files[0] || !currentUser) return;
    const file = e.target.files[0];
    setImportMessage('');

    try {
      const zip = await JSZip.loadAsync(file);

      // Read manifest
      const manifestFile = zip.file('notes.json');
      if (!manifestFile) {
        setImportMessage('Invalid archive: missing notes.json');
        return;
      }

      const manifestText = await manifestFile.async('text');
      const manifest: ExportManifest = JSON.parse(manifestText);

      let imported = 0;
      for (const noteData of manifest.notes) {
        const note = createNote({
          title: noteData.title,
          content: noteData.content,
          isPublic: noteData.isPublic,
          userId: currentUser.id,
        });

        // Process each entry path as provided within archive (PRD §21.2)
        // No normalisation, sanitisation, or extension validation (PRD §21.2)
        for (const attData of noteData.attachments || []) {
          // Attachment filenames used as provided (PRD §21.2)
          const archivePath = 'attachments/' + attData.filename;
          const attFile = zip.file(archivePath);
          if (attFile) {
            const base64 = await attFile.async('base64');
            // No MIME or content inspection (PRD §21.2)
            addAttachment({
              noteId: note.id,
              filename: attData.filename,          // used as-is
              originalName: attData.originalName || attData.filename,
              contentType: attData.contentType || 'application/octet-stream',
              data: `data:${attData.contentType || 'application/octet-stream'};base64,${base64}`,
            });
          }
        }
        imported++;
      }

      setImportMessage(`Successfully imported ${imported} note(s).`);
    } catch (err) {
      setImportMessage(`Import failed: ${String(err)}`);
    }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold text-gray-800">Export / Import Notes</h1>

      {/* Export */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Export Notes</h2>
        {exportMessage && (
          <div className="mb-3 p-3 bg-green-50 text-green-700 rounded text-sm">{exportMessage}</div>
        )}
        <form onSubmit={handleExport} className="space-y-4">
          <div className="space-y-2">
            {userNotes.map((note) => (
              <label key={note.id} className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={selectedNoteIds.includes(note.id)}
                  onChange={() => toggleNote(note.id)}
                  className="h-4 w-4 text-indigo-600 rounded"
                />
                <span className="text-gray-700">{note.title}</span>
                <span className={`text-xs px-1.5 py-0.5 rounded-full ${note.isPublic ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                  {note.isPublic ? 'Public' : 'Private'}
                </span>
              </label>
            ))}
          </div>
          <button
            type="submit"
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Export Selected as ZIP
          </button>
        </form>
      </div>

      {/* Import */}
      <div className="bg-white rounded-xl shadow p-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">Import Notes</h2>
        {importMessage && (
          <div className="mb-3 p-3 bg-blue-50 text-blue-700 rounded text-sm">{importMessage}</div>
        )}
        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">
            Upload ZIP archive
          </label>
          <input
            type="file"
            accept=".zip"
            onChange={handleImport}
            className="text-sm text-gray-600"
          />
          <p className="text-xs text-gray-500">
            Upload a ZIP file in the LooseNotes export format containing notes.json and attachments/.
          </p>
        </div>
      </div>
    </div>
  );
}
