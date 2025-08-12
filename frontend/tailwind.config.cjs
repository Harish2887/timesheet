/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./public/index.html",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f5f7ff',
          100: '#ebf0fe',
          200: '#dee8fe',
          300: '#c1d0fd',
          400: '#a1b2fc',
          500: '#8190fa',
          600: '#6366f1',
          700: '#5046e5',
          800: '#4338ca',
          900: '#3730a3',
        },
      },
      animation: {
        fadeIn: 'fadeIn 0.5s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
  safelist: [
    'bg-red-100',
    'text-red-700',
    'bg-green-100',
    'text-green-700',
    'bg-blue-100',
    'text-blue-700',
  ],
} 