export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const isValidEmail = (value: string): boolean => EMAIL_REGEX.test(value.trim())

export const PHONE_REGEX = /^010-\d{4}-\d{4}$/

export const isValidPhone = (value: string): boolean => PHONE_REGEX.test(value.trim())

export const NAME_REGEX = /^[가-힣a-zA-Z]{2,}$/

export const isValidName = (value: string): boolean => NAME_REGEX.test(value.trim())

export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 20

export const isValidPassword = (value: string): boolean => {
  if (value.length < PASSWORD_MIN_LENGTH || value.length > PASSWORD_MAX_LENGTH) return false
  if (!/[A-Za-z]/.test(value)) return false
  if (!/\d/.test(value)) return false
  if (!/[\W_]/.test(value)) return false
  return true
}

export const NICKNAME_REGEX = /^[가-힣a-zA-Z0-9]{2,12}$/

export const isValidNickname = (value: string): boolean => NICKNAME_REGEX.test(value.trim())
