import { useCartStore } from './store'

export function useCart() {
  return useCartStore((s) => s.items)
}

export function useCartCount() {
  return useCartStore((s) => s.items.length)
}
