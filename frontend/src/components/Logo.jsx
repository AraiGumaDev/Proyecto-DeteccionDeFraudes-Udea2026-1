export default function Logo({ size = 38 }) {
  const id = 'fg-logo-grad'
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden
    >
      <defs>
        <linearGradient id={id} x1="8" y1="4" x2="32" y2="36" gradientUnits="userSpaceOnUse">
          <stop stopColor="#60a5fa" />
          <stop offset="1" stopColor="#2563eb" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="11" fill={`url(#${id})`} />
      <path
        d="M20 8.5L12.5 11.2v6.1c0 4.6 3.2 8.9 7.5 10.2 4.3-1.3 7.5-5.6 7.5-10.2v-6.1L20 8.5Z"
        fill="rgba(255,255,255,0.95)"
      />
      <path
        d="M17.2 20.1l-2.1-2.1-1.4 1.4 3.5 3.5 6.8-6.8-1.4-1.4-5.4 5.4Z"
        fill="#2563eb"
      />
    </svg>
  )
}
