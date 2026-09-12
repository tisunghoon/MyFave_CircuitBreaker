import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import {
  useDeleteShippingAddress,
  useSetDefaultShippingAddress,
  useShippingAddresses,
} from '@/features/shipping/hooks'
import { useShippingStore } from '@/features/shipping/store'
import type { Address } from '@/features/shipping/types'

function toLocalAddress(ba: {
  shippingId: number
  receiverName: string
  receiverPhone: string
  address: string
  addressDetail: string
  zipCode: string
  deliveryRequest: string
  isDefault: boolean
}): Address {
  return {
    id: String(ba.shippingId),
    name: ba.receiverName,
    phone: ba.receiverPhone,
    address: ba.address,
    detailAddress: ba.addressDetail,
    zipcode: ba.zipCode,
    request: ba.deliveryRequest,
    isDefault: ba.isDefault,
  }
}

export function ShippingAddressPage() {
  const navigate = useNavigate()
  const [searchTerm, setSearchTerm] = useState('')
  const syncAddresses = useShippingStore((s) => s.syncAddresses)

  const { data: backendAddresses = [], isLoading, isError, isSuccess } = useShippingAddresses()
  const deleteAddress = useDeleteShippingAddress()
  const setDefaultMutation = useSetDefaultShippingAddress()
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)

  useEffect(() => {
    // isSuccess 일 때만 sync — 빈 배열도 반드시 스토어에 반영해 이전 캐시 잔존 방지 (CR M15).
    if (isSuccess) {
      syncAddresses(backendAddresses.map(toLocalAddress))
    }
  }, [isSuccess, backendAddresses, syncAddresses])

  const addresses = backendAddresses.map(toLocalAddress)

  const handleSelectDefault = (id: string) => {
    setDefaultMutation.mutate(Number(id))
  }

  const handleDelete = (e: React.MouseEvent, id: string) => {
    e.stopPropagation()
    setPendingDeleteId(id)
  }

  const handleConfirmDelete = () => {
    if (pendingDeleteId === null) return
    deleteAddress.mutate(Number(pendingDeleteId))
    setPendingDeleteId(null)
  }

  const handleCancelDelete = () => setPendingDeleteId(null)

  const filteredAddresses = addresses.filter(
    (addr) =>
      addr.name.includes(searchTerm) ||
      addr.address.includes(searchTerm) ||
      addr.phone.includes(searchTerm),
  )

  return (
    <div className="flex-1 bg-white min-h-0 pb-10">
      {/* Search Bar */}
      <div className="px-[20px] pt-[28.01px] mb-[28px]">
        <div className="relative">
          <input
            type="text"
            placeholder="배송지의 이름, 주소, 연락처로 검색하세요"
            className="w-full h-[45px] rounded-[5px] border border-separator pl-[15px] pr-[45px] font-noto text-[12px] text-black placeholder:text-[#949494] focus:border-point focus:outline-none transition-colors"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <div className="absolute right-[15px] top-1/2 -translate-y-1/2">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#949494" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </div>
        </div>
      </div>

      {/* Address List */}
      <div className="px-[20px] space-y-[16px]">
        {isLoading && (
          <>
            {[1, 2].map((i) => (
              <div key={i} className="h-[110px] w-full rounded-[12px] bg-gray-100 animate-pulse" />
            ))}
          </>
        )}

        {!isLoading && isError && (
          // 에러 상태를 빈 목록으로 숨기지 않도록 명시적 분기 (CR M14).
          <p className="py-12 text-center font-noto text-[13px] text-muted-text">
            배송지를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
          </p>
        )}

        {!isLoading && !isError && filteredAddresses.map((addr) => (
          <div
            key={addr.id}
            onClick={() => handleSelectDefault(addr.id)}
            className={`flex flex-col gap-[8px] p-[20px] rounded-[12px] border transition-all cursor-pointer relative ${
              addr.isDefault
                ? 'border-point bg-main-bg/30 shadow-md'
                : 'border-separator bg-white hover:border-point/50'
            }`}
          >
            <div className="flex items-center gap-[10px]">
              <div className={`w-[18px] h-[18px] rounded-full border flex items-center justify-center ${
                addr.isDefault ? 'border-point bg-point' : 'border-separator bg-white'
              }`}>
                {addr.isDefault && <div className="w-[8px] h-[8px] rounded-full bg-white" />}
              </div>
              <span className="font-noto text-[16px] font-bold text-[#322927]">
                {addr.name}
              </span>
              {addr.isDefault && (
                <div className="h-[20px] px-[8px] flex items-center justify-center bg-point rounded-[5px]">
                  <span className="font-noto text-[10px] font-medium text-white">기본</span>
                </div>
              )}
            </div>

            <p className="font-noto text-[12px] leading-[18px] text-[#322927] pr-20">
              {`${addr.address}${addr.detailAddress ? ` ${addr.detailAddress}` : ''}`}
            </p>

            <p className="font-noto text-[12px] text-[#322927]">
              {addr.phone}
            </p>

            {/* Action Buttons */}
            <div className="absolute top-[20px] right-[20px] flex gap-[6px]">
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  navigate(`/add-shipping?from=/shipping-addresses&id=${addr.id}`)
                }}
                className="h-[24px] px-[8px] flex items-center justify-center bg-footer-bg rounded-[5px] border border-separator/30 active:opacity-70 transition-opacity"
              >
                <span className="font-noto text-[10px] font-medium text-[#949494]">수정</span>
              </button>
              <button
                type="button"
                onClick={(e) => handleDelete(e, addr.id)}
                className="h-[24px] px-[8px] flex items-center justify-center bg-white rounded-[5px] border border-red-100 active:bg-red-50 transition-colors"
              >
                <span className="font-noto text-[10px] font-medium text-red-400">삭제</span>
              </button>
            </div>
          </div>
        ))}

        {!isLoading && filteredAddresses.length === 0 && (
          <div className="py-20 text-center">
            <p className="font-noto text-[14px] text-muted-text">
              {searchTerm ? '검색 결과가 없습니다.' : '등록된 배송지가 없습니다.'}
            </p>
          </div>
        )}
      </div>

      {/* Add Address Button */}
      <div className="mt-10 px-[32px] flex justify-center pb-10">
        <button
          type="button"
          onClick={() => navigate('/add-shipping?from=/shipping-addresses')}
          className="w-full max-w-[312px] h-[56px] rounded-[12px] bg-point font-noto text-[16px] font-bold text-white shadow-lg shadow-point/20 active:scale-[0.98] transition-all"
        >
          배송지 추가하기
        </button>
      </div>

      {pendingDeleteId !== null && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center px-9 animate-in fade-in duration-200">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-[1px]" onClick={handleCancelDelete} />
          <div className="relative w-full max-w-[320px] overflow-hidden rounded-[15px] bg-white shadow-2xl">
            <div className="px-6 py-10 text-center">
              <p className="font-noto text-[15px] font-bold leading-relaxed text-[#322927]">
                배송지를 삭제하시겠습니까?
              </p>
            </div>
            <div className="flex border-t border-[#F2EDEB]">
              <button
                type="button"
                onClick={handleCancelDelete}
                className="flex-1 py-4 font-noto text-[14px] font-bold text-[#999999] hover:bg-gray-50 border-r border-[#F2EDEB]"
              >
                아니오
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                className="flex-1 py-4 font-noto text-[14px] font-bold text-point hover:bg-[#FEF6F6]"
              >
                예
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
