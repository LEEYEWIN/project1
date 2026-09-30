export default function StepIndicator({ steps, current }) {
  return (
    <ol className="steps">
      {steps.map((label, i) => (
        <li key={label} className={i === current ? 'active' : i < current ? 'done' : ''}>
          <span>{i + 1}</span> {label}
        </li>
      ))}
    </ol>
  );
}