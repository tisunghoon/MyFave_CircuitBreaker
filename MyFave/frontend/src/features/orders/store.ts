import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { Order } from './types'

interface OrderState {
  orders: Order[]
  addOrder: (order: Order) => void
  reset: () => void
}

export const useOrderStore = create<OrderState>()(
  persist(
    (set) => ({
      orders: [],
      addOrder: (order) =>
        set((state) => ({ orders: [order, ...state.orders] })),
      reset: () => set({ orders: [] }),
    }),
    {
      name: 'myfave-orders',
      version: 1,
      migrate: () => ({ orders: [] }),
    },
  ),
)
