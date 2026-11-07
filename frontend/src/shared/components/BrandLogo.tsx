import React from "react";

export function BrandLogo({ size = 26, className = "" }: { size?: number; className?: string }) {
  const gradientId = React.useId();
  return (
    <svg
      className={`brand-logo-svg ${className}`}
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id={`${gradientId}-top`} x1="4" y1="2" x2="28" y2="16" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#38bdf8" />
          <stop offset="50%" stopColor="#818cf8" />
          <stop offset="100%" stopColor="#c084fc" />
        </linearGradient>
        <linearGradient id={`${gradientId}-left`} x1="4" y1="9" x2="16" y2="30" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#6366f1" />
          <stop offset="100%" stopColor="#4338ca" />
        </linearGradient>
        <linearGradient id={`${gradientId}-right`} x1="16" y1="16" x2="28" y2="30" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#8b5cf6" />
          <stop offset="60%" stopColor="#6366f1" />
          <stop offset="100%" stopColor="#06b6d4" />
        </linearGradient>
        <filter id={`${gradientId}-glow`} x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="1.5" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>
      <g filter={`url(#${gradientId}-glow)`}>
        {/* Top facet */}
        <path
          d="M16 3L27.5 9.6L16 16.2L4.5 9.6Z"
          fill={`url(#${gradientId}-top)`}
        />
        {/* Left facet */}
        <path
          d="M4.5 10.8L15.3 17.1V29.5L4.5 23.2Z"
          fill={`url(#${gradientId}-left)`}
        />
        {/* Right facet */}
        <path
          d="M16.7 17.1L27.5 10.8V23.2L16.7 29.5Z"
          fill={`url(#${gradientId}-right)`}
        />
      </g>
    </svg>
  );
}
