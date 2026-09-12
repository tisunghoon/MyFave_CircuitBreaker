import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { Address } from './types'

interface ShippingState {
  addresses: Address[]
  addAddress: (address: Address) => void
  updateAddress: (id: string, update: Partial<Address>) => void
  removeAddress: (id: string) => void
  setDefault: (id: string) => void
  syncAddresses: (addresses: Address[]) => void
}

export const useShippingStore = create<ShippingState>()(
  persist(
    (set) => ({
      addresses: [],
      addAddress: (address) =>
        set((state) => {
          if (address.isDefault) {
            return {
              addresses: [
                ...state.addresses.map((a) => ({ ...a, isDefault: false })),
                address,
              ],
            }
          }
          return { addresses: [...state.addresses, address] }
        }),
      updateAddress: (id, update) =>
        set((state) => {
          const willBeDefault = update.isDefault === true
          return {
            addresses: state.addresses.map((a) => {
              if (a.id === id) return { ...a, ...update }
              return willBeDefault ? { ...a, isDefault: false } : a
            }),
          }
        }),
      removeAddress: (id) =>
        set((state) => ({ addresses: state.addresses.filter((a) => a.id !== id) })),
      setDefault: (id) =>
        set((state) => ({
          addresses: state.addresses.map((a) => ({ ...a, isDefault: a.id === id })),
        })),
      syncAddresses: (addresses) => set({ addresses }),
    }),
    {
      name: 'myfave-shipping',
      version: 1,
      migrate: () => ({ addresses: [] }),
    },
  ),
)

export function getDefaultAddress(addresses: Address[]): Address | null {
  return addresses.find((a) => a.isDefault) ?? addresses[0] ?? null
}
