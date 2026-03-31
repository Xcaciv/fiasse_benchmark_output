import React from 'react';

interface Option {
  value: string;
  label: string;
}

interface Props extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  options: Option[];
}

export function Select({ label, error, options, className = '', id, ...props }: Props) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');
  return (
    <div className="space-y-1">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700">
          {label}
        </label>
      )}
      <select
        id={inputId}
        {...props}
        className={`
          block w-full px-3 py-2 border rounded-md shadow-sm text-sm bg-white
          focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
          ${error ? 'border-red-300' : 'border-gray-300'}
          ${className}
        `}
      >
        {options.map(o => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}
