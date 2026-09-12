import { AppRouter } from './router'
import { Providers } from './providers'

export default function App() {
  return (
    <Providers>
      <AppRouter />
    </Providers>
  )
}
