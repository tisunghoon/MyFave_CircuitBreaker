import { useState } from 'react'
import { createPortal } from 'react-dom'
import { KakaoPostcodeEmbed } from 'react-daum-postcode'
import type { Address as DaumAddress } from 'react-daum-postcode'
import { useNavigate, useSearchParams } from 'react-router-dom'

import type { AxiosError } from 'axios'

import { shippingApi } from '@/features/shipping/api'
import { useShippingStore } from '@/features/shipping/store'
import type { Address } from '@/features/shipping/types'
import { PopUp } from '@/shared/components/PopUp'
import { isValidName, isValidPhone } from '@/shared/utils/validation'

interface AddressFormData {
  name: string
  phone: string
  zipcode: string
  address: string
  detailAddress: string
  request: string
  isDefault: boolean
}

const initialFormData: AddressFormData = {
  name: '',
  phone: '',
  zipcode: '',
  address: '',
  detailAddress: '',
  request: '',
  isDefault: false,
}

function buildInitialFormData(target: Address | undefined): AddressFormData {
  if (!target) return initialFormData
  return {
    name: target.name,
    phone: target.phone,
    zipcode: target.zipcode ?? '',
    address: target.address,
    detailAddress: target.detailAddress ?? '',
    request: target.request ?? '',
    isDefault: target.isDefault,
  }
}

interface AddShippingFormProps {
  editId: string | null
  fromPath: string
  initialTarget: Address | undefined
}

function AddShippingForm({ editId, fromPath, initialTarget }: AddShippingFormProps) {
  const navigate = useNavigate()
  const addresses = useShippingStore((s) => s.addresses)
  const addAddress = useShippingStore((s) => s.addAddress)
  const updateAddress = useShippingStore((s) => s.updateAddress)
  const [isOpenPost, setIsOpenPost] = useState(false)
  const [formData, setFormData] = useState<AddressFormData>(() => buildInitialFormData(initialTarget))
  const [isPopUpOpen, setIsPopUpOpen] = useState(false)
  const [popUpMessage, setPopUpMessage] = useState('')

  const showPopUp = (msg: string) => {
    setPopUpMessage(msg)
    setIsPopUpOpen(true)
  }

  const handleComplete = (data: DaumAddress) => {
    let fullAddress = data.address
    let extraAddress = ''

    if (data.addressType === 'R') {
      if (data.bname !== '') {
        extraAddress += data.bname
      }
      if (data.buildingName !== '') {
        extraAddress += extraAddress !== '' ? `, ${data.buildingName}` : data.buildingName
      }
      fullAddress += extraAddress !== '' ? ` (${extraAddress})` : ''
    }

    setFormData({
      ...formData,
      zipcode: data.zonecode,
      address: fullAddress,
    })
    setIsOpenPost(false)
  }

  const formatPhoneNumber = (value: string) => {
    const numbers = value.replace(/[^\d]/g, '')
    if (numbers.length <= 3) return numbers
    if (numbers.length <= 7) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`
    return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7, 11)}`
  }

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatPhoneNumber(e.target.value)
    setFormData({ ...formData, phone: formatted })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.name || !formData.phone || !formData.address || !formData.zipcode) {
      showPopUp('이름, 휴대폰 번호, 주소는 필수입니다')
      return
    }

    if (!isValidName(formData.name)) {
      showPopUp('이름은 한글 또는 영문 2자 이상으로 입력해주세요')
      return
    }

    if (!isValidPhone(formData.phone)) {
      showPopUp('휴대폰 번호를 010-XXXX-XXXX 형식으로 입력해주세요')
      return
    }

    try {
      if (editId) {
        const shippingId = parseInt(editId, 10)
        if (isNaN(shippingId)) {
          showPopUp('유효하지 않은 배송지 ID입니다')
          return
        }
        await shippingApi.updateAddress(shippingId, {
          receiverName: formData.name,
          receiverPhone: formData.phone,
          address: formData.address,
          addressDetail: formData.detailAddress || undefined,
          zipCode: formData.zipcode,
          deliveryRequest: formData.request || undefined,
          isDefault: formData.isDefault,
        })
        updateAddress(editId, {
          name: formData.name,
          phone: formData.phone,
          zipcode: formData.zipcode,
          address: formData.address,
          detailAddress: formData.detailAddress,
          request: formData.request,
          isDefault: formData.isDefault,
        })
      } else {
        const isFirstAddress = addresses.length === 0
        const isDefault = formData.isDefault || isFirstAddress

        // 백엔드에 배송지 등록 → 반환된 shippingId를 로컬 id로 사용
        const backendAddr = await shippingApi.createAddress({
          receiverName: formData.name,
          receiverPhone: formData.phone,
          address: formData.address,
          addressDetail: formData.detailAddress || undefined,
          zipCode: formData.zipcode,
          deliveryRequest: formData.request || undefined,
          isDefault,
        })

        addAddress({
          id: String(backendAddr.shippingId),
          name: formData.name,
          phone: formData.phone,
          zipcode: formData.zipcode,
          address: formData.address,
          detailAddress: formData.detailAddress,
          request: formData.request,
          isDefault,
        })
      }

      navigate(fromPath)
    } catch (err) {
      const axiosErr = err as AxiosError<{ message?: string }>
      const fallback = editId ? '배송지 수정 중 오류가 발생했습니다' : '배송지 저장 중 오류가 발생했습니다'
      const msg = axiosErr.response?.data?.message ?? fallback
      showPopUp(msg)
    }
  }

  const postcodeModal = isOpenPost && createPortal(
    <div className="fixed inset-0 z-[300] flex items-center justify-center bg-black/50 p-4" onClick={() => setIsOpenPost(false)}>
      <div className="relative w-full max-w-[460px] bg-white rounded-lg overflow-hidden shadow-2xl" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between p-4 border-b border-separator">
          <h2 className="font-noto text-[16px] font-bold">주소 찾기</h2>
          <button
            type="button"
            onClick={() => setIsOpenPost(false)}
            className="p-1 hover:bg-gray-100 rounded-full transition-colors"
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div className="w-full h-[480px]">
          <KakaoPostcodeEmbed
            onComplete={handleComplete}
            autoClose={false}
            style={{ width: '100%', height: '100%' }}
          />
        </div>
      </div>
    </div>,
    document.body
  )

  return (
    <>
    <div className="flex-1 bg-white min-h-0 pb-10">
      <form onSubmit={handleSubmit} className="px-[31px] pt-[28.01px] space-y-[44px]">
        {/* 이름 섹션 */}
        <div className="relative h-[64px]">
          <label className="absolute top-0 left-0 font-noto text-[16px] font-normal leading-[24px] text-black">
            이름
          </label>
          <input
            type="text"
            placeholder="받는 분의 이름을 입력해주세요"
            className="absolute bottom-0 left-0 w-full h-[35px] rounded-[5px] border border-separator px-[15px] font-noto text-[16px] text-black placeholder:text-[#CFB0B0] focus:border-point focus:outline-none transition-colors"
            value={formData.name}
            onChange={(e) => setFormData({...formData, name: e.target.value})}
          />
        </div>

        {/* 휴대폰 번호 섹션 */}
        <div className="relative h-[64px]">
          <label className="absolute top-0 left-0 font-noto text-[16px] font-normal leading-[24px] text-black">
            휴대폰 번호
          </label>
          <input
            type="tel"
            placeholder="휴대폰번호를 입력해주세요"
            className="absolute bottom-0 left-0 w-full h-[35px] rounded-[5px] border border-separator px-[15px] font-noto text-[16px] text-black placeholder:text-[#CFB0B0] focus:border-point focus:outline-none transition-colors"
            value={formData.phone}
            onChange={handlePhoneChange}
            maxLength={13}
          />
        </div>

        {/* 주소 섹션 - Figma Node 100:327, 331, 333, 340 (간격 보정) */}
        <div className="space-y-[8px]">
          <label className="font-noto text-[16px] font-normal leading-[24px] text-black block">
            주소
          </label>
          <div className="space-y-[10px]">
            <div className="flex gap-[20px]">
              <input
                type="text"
                placeholder="우편번호"
                className="w-[224px] h-[35px] rounded-[5px] border border-separator px-[15px] font-noto text-[16px] text-black placeholder:text-[#CFB0B0] bg-white focus:outline-none"
                readOnly
                value={formData.zipcode}
              />
              <button
                type="button"
                onClick={() => setIsOpenPost(true)}
                className="w-[69px] h-[35px] rounded-[5px] bg-[#D9D9D9] font-noto text-[15px] font-medium text-[#949494] active:opacity-80 transition-opacity"
              >
                주소 찾기
              </button>
            </div>
            <input
              type="text"
              placeholder="주소"
              className="w-full h-[35px] rounded-[5px] border border-separator px-[15px] font-noto text-[16px] text-black placeholder:text-[#CFB0B0] bg-white focus:outline-none"
              readOnly
              value={formData.address}
            />
            <input
              type="text"
              placeholder="상세주소"
              className="w-full h-[35px] rounded-[5px] border border-separator px-[15px] font-noto text-[16px] text-black placeholder:text-[#CFB0B0] focus:border-point focus:outline-none transition-colors"
              value={formData.detailAddress}
              onChange={(e) => setFormData({...formData, detailAddress: e.target.value})}
            />
          </div>
        </div>

        {/* 배송 요청사항 섹션 */}
        <div className="relative h-[64px]">
          <label className="absolute top-0 left-0 font-noto text-[16px] font-normal leading-[24px] text-black">
            배송 요청사항 (선택)
          </label>
          <div className="absolute bottom-0 left-0 w-full h-[35px]">
            <select 
              className="w-full h-full rounded-[5px] border border-separator px-[15px] font-noto text-[16px] text-black focus:border-point focus:outline-none bg-white appearance-none"
              value={formData.request}
              onChange={(e) => setFormData({...formData, request: e.target.value})}
            >
              <option value="" className="text-[#CFB0B0]">배송 요청사항을 선택해주세요</option>
              <option value="문 앞에 두어주세요">문 앞에 두어주세요</option>
              <option value="경비실에 맡겨주세요">경비실에 맡겨주세요</option>
            </select>
            <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-separator">
              <svg width="10" height="6" viewBox="0 0 10 6" fill="none">
                <path d="M1 1L5 5L9 1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </div>
          </div>
        </div>

        {/* 기본 배송지 설정 - Figma Node 100:338 (크기 1.3배 확대 보정) */}
        <div className="flex items-center gap-[8px] mt-[-48px]">
          <input
            type="checkbox"
            id="default-address"
            className="w-[17px] h-[17px] rounded-[3px] border-separator text-point focus:ring-0 cursor-pointer"
            checked={formData.isDefault}
            onChange={(e) => setFormData({...formData, isDefault: e.target.checked})}
          />
          <label htmlFor="default-address" className="font-noto text-[14px] font-normal text-[#949494] cursor-pointer">
            기본 배송지로 설정
          </label>
        </div>

        <div className="pt-[140px] flex justify-center">
          <button
            type="submit"
            className="w-[312px] h-[36px] rounded-[12px] bg-point font-noto text-[14px] font-bold text-white shadow-lg shadow-point/20 active:scale-[0.98] transition-all"
          >
            {editId ? '배송지 수정하기' : '배송지 추가하기'}
          </button>
        </div>
      </form>

    </div>

    {postcodeModal}
    <PopUp isOpen={isPopUpOpen} message={popUpMessage} onClose={() => setIsPopUpOpen(false)} />
    </>
  )
}

export function AddShippingPage() {
  const [searchParams] = useSearchParams()
  const editId = searchParams.get('id')
  const fromPath = searchParams.get('from') || '/shipping-addresses'
  const initialTarget = useShippingStore((s) =>
    editId ? s.addresses.find((a) => a.id === editId) : undefined,
  )

  return (
    <AddShippingForm
      key={editId ?? 'new'}
      editId={editId}
      fromPath={fromPath}
      initialTarget={initialTarget}
    />
  )
}
