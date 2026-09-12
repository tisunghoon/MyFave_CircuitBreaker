import { useEffect, useRef } from 'react'

interface PopUpProps {
  message: string
  isOpen: boolean
  onClose: () => void
  duration?: number
}

export function PopUp({ message, isOpen, onClose, duration = 3000 }: PopUpProps) {
  const onCloseRef = useRef(onClose)
  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => onCloseRef.current(), duration)
      return () => clearTimeout(timer)
    }
  }, [isOpen, duration])

  if (!isOpen) return null

  return (
    <div className="fixed bottom-24 left-1/2 -translate-x-1/2 z-[150] w-[340px]">
      <div className="w-full transition-all animate-in fade-in slide-in-from-bottom-8 duration-500 ease-out">
        {/* Pop up Content - Figma Node 99:1204 100% Sync */}
        <div className="flex h-[48px] items-center justify-center rounded-[8px] bg-main-bg shadow-[0px_2px_2px_0px_rgba(0,0,0,0.25)] border border-point/5">
          <p className="font-noto text-[14px] font-medium leading-[21px] text-[#1B1B1B] text-center">
            {message}
          </p>
        </div>
      </div>
    </div>
  )
}
