import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { CartItem } from './types'

interface CartState {
  items: CartItem[]
  addItem: (item: CartItem) => void
  removeItem: (id: number) => void
  clear: () => void
}

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      addItem: (item) =>
        set((state) =>
          state.items.some((i) => i.id === item.id)
            ? state
            : { items: [...state.items, item] },
        ),
      removeItem: (id) =>
        set((state) => ({ items: state.items.filter((i) => i.id !== id) })),
      clear: () => set({ items: [] }),
    }),
    { name: 'myfave-cart' },
  ),
)
