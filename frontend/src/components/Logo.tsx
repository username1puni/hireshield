export default function Logo({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <path
        d="M32 4.5 L53 11.5 C54.2 11.9 55 13 55 14.3 V29 C55 43.5 45.6 54.2 33.2 59.3 C32.4 59.6 31.6 59.6 30.8 59.3 C18.4 54.2 9 43.5 9 29 V14.3 C9 13 9.8 11.9 11 11.5 Z"
        fill="currentColor"
      />
      <path d="M21.5 31.5 L29 39 L43.5 24.5" fill="none" stroke="#2dd4bf" strokeWidth="6.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
