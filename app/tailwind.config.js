/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        background: '#131313',
        surface: '#1c1b1b',
        'surface-high': '#2a2a2a',
        'surface-bright': '#393939',
        primary: '#ecb2ff',
        'primary-strong': '#bd00ff',
        secondary: '#a7ffb3',
        'secondary-strong': '#00ee70',
        tertiary: '#ffb2bf',
        'tertiary-strong': '#e80063',
        outline: '#514255',
        ink: '#e5e2e1',
        muted: '#d4c0d7',
        danger: '#ffb4ab'
      },
      fontFamily: {
        display: ['Anybody', 'system-ui', 'sans-serif'],
        body: ['Hanken Grotesk', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace']
      },
      boxShadow: {
        neon: '0 0 18px rgba(236, 178, 255, 0.3)',
        lime: '0 0 18px rgba(167, 255, 179, 0.22)',
        pink: '0 0 18px rgba(255, 178, 191, 0.25)'
      },
      spacing: {
        gutter: '16px'
      }
    }
  },
  plugins: []
};
