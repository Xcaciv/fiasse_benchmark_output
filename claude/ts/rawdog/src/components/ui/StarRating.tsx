import { Star } from 'lucide-react';

interface Props {
  value: number;
  onChange?: (v: number) => void;
  readOnly?: boolean;
  size?: number;
}

export function StarRating({ value, onChange, readOnly = false, size = 20 }: Props) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map(star => (
        <button
          key={star}
          type="button"
          disabled={readOnly}
          onClick={() => onChange?.(star)}
          className={`focus:outline-none transition-colors ${readOnly ? 'cursor-default' : 'cursor-pointer hover:scale-110'}`}
        >
          <Star
            size={size}
            className={star <= value ? 'text-yellow-400 fill-yellow-400' : 'text-gray-300 fill-gray-100'}
          />
        </button>
      ))}
    </div>
  );
}

export function AverageStars({ value, count, size = 16 }: { value: number; count: number; size?: number }) {
  return (
    <div className="flex items-center gap-1.5">
      <div className="flex gap-0.5">
        {[1, 2, 3, 4, 5].map(star => (
          <Star
            key={star}
            size={size}
            className={star <= Math.round(value) ? 'text-yellow-400 fill-yellow-400' : 'text-gray-300 fill-gray-100'}
          />
        ))}
      </div>
      <span className="text-sm text-gray-600">
        {value.toFixed(1)} ({count} {count === 1 ? 'rating' : 'ratings'})
      </span>
    </div>
  );
}
