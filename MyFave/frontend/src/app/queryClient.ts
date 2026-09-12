import { QueryClient } from '@tanstack/react-query'

// providers.tsx에서 분리: Fast Refresh가 컴포넌트 파일과 비컴포넌트 export를 함께 두지 못함
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})
