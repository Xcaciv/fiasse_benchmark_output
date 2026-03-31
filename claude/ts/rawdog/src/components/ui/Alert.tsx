import { CheckCircle, AlertCircle, Info, XCircle, X } from 'lucide-react';

type Type = 'success' | 'error' | 'info' | 'warning';

interface Props {
  type?: Type;
  message: string;
  onClose?: () => void;
  className?: string;
}

const config = {
  success: { bg: 'bg-green-50 border-green-200', text: 'text-green-800', Icon: CheckCircle, iconColor: 'text-green-500' },
  error: { bg: 'bg-red-50 border-red-200', text: 'text-red-800', Icon: XCircle, iconColor: 'text-red-500' },
  info: { bg: 'bg-blue-50 border-blue-200', text: 'text-blue-800', Icon: Info, iconColor: 'text-blue-500' },
  warning: { bg: 'bg-yellow-50 border-yellow-200', text: 'text-yellow-800', Icon: AlertCircle, iconColor: 'text-yellow-500' },
};

export function Alert({ type = 'info', message, onClose, className = '' }: Props) {
  const { bg, text, Icon, iconColor } = config[type];
  return (
    <div className={`flex items-start gap-3 p-4 rounded-md border ${bg} ${className}`}>
      <Icon size={16} className={`mt-0.5 flex-shrink-0 ${iconColor}`} />
      <p className={`text-sm flex-1 ${text}`}>{message}</p>
      {onClose && (
        <button onClick={onClose} className={`flex-shrink-0 ${text} hover:opacity-70`}>
          <X size={14} />
        </button>
      )}
    </div>
  );
}
