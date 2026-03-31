import React from 'react';

interface Props extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export function Textarea({ label, error, hint, className = '', id, ...props }: Props) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');
  return (
    <div className="space-y-1">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <textarea
        id={inputId}
        {...props}
        className={`
          block w-full px-3 py-2 border rounded-md shadow-sm text-sm
          focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
          transition-colors resize-vertical
          ${error ? 'border-red-300 bg-red-50' : 'border-gray-300 bg-white'}
          disabled:bg-gray-100 disabled:text-gray-500
          ${className}
        `}
      />
      {error && <p className="text-xs text-red-600">{error}</p>}
      {hint && !error && <p className="text-xs text-gray-500">{hint}</p>}
    </div>
  );
}
