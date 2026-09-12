/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Radix UI / shadcn semantic tokens
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
        // MyFave brand colors
        point: 'var(--point-color)',
        'main-bg': 'var(--main-color)',
        sub1: 'var(--sub-color1)',
        sub2: 'var(--sub-color2)',
        'chat-bg': 'var(--chat-color)',
        'chat-font': 'var(--chat-font-color)',
        'chat-bg2': 'var(--chat-color2)',
        'chat-font2': 'var(--chat-font-color2)',
        'footer-bg': 'var(--footer-color)',
        separator: 'var(--separator-color)',
        'dark-text': 'var(--text-dark)',
        'muted-text': 'var(--text-muted)',
        // Legacy pink scale
        pink: {
          50: '#FFF0F5',
          100: '#FFD6E7',
          200: '#FFB3D1',
          300: '#FF85B3',
          400: '#FF5C96',
          500: '#FF2D72',
          600: '#E0005A',
          700: '#B80049',
          800: '#8F0038',
          900: '#660026',
        },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
      fontFamily: {
        sans: ["'Noto Sans KR'", '-apple-system', 'sans-serif'],
        noto: ["'Noto Sans KR'", '-apple-system', 'sans-serif'],
        lexend: ['Lexend', '-apple-system', 'sans-serif'],
        montserrat: ['Montserrat', '-apple-system', 'sans-serif'],
        inter: ['Inter', '-apple-system', 'sans-serif'],
      },
      boxShadow: {
        'figma-popup': '0px 2px 2px 0px rgba(0, 0, 0, 0.25)',
        'figma-app': '0px 0px 40px 0px rgba(0, 0, 0, 0.08)',
        'figma-card': '0px 4px 10px 0px rgba(0, 0, 0, 0.05)',
      },
      borderWidth: {
        DEFAULT: '1px',
        '0': '0',
        '2': '2px',
        '3': '3px',
        '4': '4px',
        '8': '8px',
        'figma': '1.096px',
      },
      keyframes: {
        'accordion-down': {
          from: { height: '0' },
          to: { height: 'var(--radix-accordion-content-height)' },
        },
        'accordion-up': {
          from: { height: 'var(--radix-accordion-content-height)' },
          to: { height: '0' },
        },
        'slide-in': {
          from: { transform: 'translateX(-100%)' },
          to: { transform: 'translateX(0)' },
        },
        'slide-out': {
          from: { transform: 'translateX(0)' },
          to: { transform: 'translateX(-100%)' },
        },
      },
      animation: {
        'accordion-down': 'accordion-down 0.2s ease-out',
        'accordion-up': 'accordion-up 0.2s ease-out',
        'slide-in': 'slide-in 0.3s ease-out',
        'slide-out': 'slide-out 0.3s ease-out',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}
