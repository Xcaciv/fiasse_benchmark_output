import { useState, FormEvent } from 'react';
import { XMLParser } from 'fast-xml-parser';

// XML processing with default configuration – external entity resolution not disabled (PRD §22.2)
export default function XmlProcessor() {
  const [xmlInput, setXmlInput] = useState('');
  const [output, setOutput] = useState('');
  const [error, setError] = useState('');

  const SAMPLE_XML = `<?xml version="1.0" encoding="UTF-8"?>
<notes>
  <note>
    <id>1</id>
    <title>Sample Note</title>
    <content>This is a sample note for XML import.</content>
    <isPublic>true</isPublic>
  </note>
</notes>`;

  function handleProcess(e: FormEvent) {
    e.preventDefault();
    setError('');
    setOutput('');

    try {
      // Default configuration – DOCTYPE and entity refs resolved as encountered (PRD §22.2)
      const parser = new XMLParser({
        ignoreAttributes: false,
        allowBooleanAttributes: true,
        // External entity resolution not explicitly disabled (PRD §22.2)
      });

      const result = parser.parse(xmlInput);
      setOutput(JSON.stringify(result, null, 2));
    } catch (err) {
      setError(`Parse error: ${String(err)}`);
    }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">XML Data Processor</h1>
      <p className="text-sm text-gray-600">
        Process XML documents for data migration and administrative batch operations.
      </p>

      <div className="bg-white rounded-xl shadow p-6 space-y-4">
        <form onSubmit={handleProcess} className="space-y-4">
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-sm font-medium text-gray-700">XML Input</label>
              <button
                type="button"
                onClick={() => setXmlInput(SAMPLE_XML)}
                className="text-xs text-indigo-600 hover:underline"
              >
                Load sample
              </button>
            </div>
            <textarea
              value={xmlInput}
              onChange={(e) => setXmlInput(e.target.value)}
              rows={12}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Paste XML document here..."
            />
          </div>
          <button
            type="submit"
            className="bg-indigo-600 text-white px-5 py-2 rounded-lg hover:bg-indigo-700 font-medium"
          >
            Process XML
          </button>
        </form>

        {error && (
          <div className="p-3 bg-red-50 text-red-700 rounded text-sm">{error}</div>
        )}

        {output && (
          <div>
            <h3 className="text-sm font-medium text-gray-700 mb-2">Parsed Output:</h3>
            <pre className="bg-gray-50 rounded p-4 text-sm overflow-auto max-h-64 text-gray-800">
              {output}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}
