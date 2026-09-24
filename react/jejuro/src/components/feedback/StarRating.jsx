/** 1~5점 별점 */
export default function StarRating({ value, onChange }) {
  return (
    <div className="stars" role="radiogroup" aria-label="만족도">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n}점`}
          className={value >= n ? 'star on' : 'star'}
          onClick={() => onChange(n)}
        >
          ★
        </button>
      ))}
    </div>
  );
}