import React from 'react'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  children: React.ReactNode
  buttonText?: string
  onButtonClick?: () => void
}

export function Modal({ isOpen, onClose, children, buttonText, onButtonClick }: ModalProps) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center px-9">
      {/* Overlay */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-[1px]" onClick={onClose} />
      
      {/* Modal Content - Figma Node 99:960 기반 */}
      <div className="relative w-full max-w-sm overflow-hidden rounded-[15px] border border-separator bg-footer-bg shadow-2xl transition-all animate-in zoom-in-95 duration-200">
        <div className="px-6 py-10 text-center">
          <div className="font-noto text-[15px] font-bold leading-relaxed text-dark-text">
            {children}
          </div>
        </div>
        
        {buttonText && (
          <button
            onClick={onButtonClick || onClose}
            className="flex w-full items-center justify-center bg-point py-[18px] font-noto text-sm font-black text-white transition-colors hover:bg-[#ff7fa3] active:scale-[0.98]"
          >
            {buttonText}
          </button>
        )}
      </div>
    </div>
  )
}
