
type IconType = 'bear' | 'human' | 'seller'
type Variant = number

interface UserIconProps {
  type?: IconType
  variant?: Variant
  className?: string
  size?: number
  // 회원가입 시 부여된 사용자별 프로필 이미지 URL. 존재하면 type/variant 매핑보다 우선.
  profileImageUrl?: string
}

// Figma Node 99:351 (곰돌이), 99:380 (셀러) 기반 100% 실사 이미지 URL
const ICON_URLS: Record<IconType, Record<number, string>> = {
  bear: {
    1: 'https://api.builder.io/api/v1/image/assets/TEMP/5c35661ffb032d111daeaeddf909db75bead765c?width=46',
    3: 'https://api.builder.io/api/v1/image/assets/TEMP/1eba4e904dccbdd1f1c9e77dc7b2459c7f3d30fe?width=46',
    5: 'https://api.builder.io/api/v1/image/assets/TEMP/5c35661ffb032d111daeaeddf909db75bead765c?width=46',
    6: 'https://api.builder.io/api/v1/image/assets/TEMP/1eba4e904dccbdd1f1c9e77dc7b2459c7f3d30fe?width=46',
    7: 'https://api.builder.io/api/v1/image/assets/TEMP/fbce4987b2973b8b3f8ead9a566874da5b500af6?width=46',
    10: 'https://api.builder.io/api/v1/image/assets/TEMP/5c35661ffb032d111daeaeddf909db75bead765c?width=46',
  },
  human: {
    1: 'https://api.builder.io/api/v1/image/assets/TEMP/664f3316f9f68e98296767568c4a9a0815d48a0f?width=112',
  },
  seller: {
    1: 'https://api.builder.io/api/v1/image/assets/TEMP/10839d8a0a408e0bb7f424c267a2fb43e0feb3e4?width=46', // MyFave 공식 셀러 아이콘
  }
}

export function UserIcon({ type = 'bear', variant = 1, className = '', size = 23.17, profileImageUrl }: UserIconProps) {
  // profileImageUrl 이 있으면 그 S3 URL 을 우선 사용. 없으면 기존 type/variant 매핑.
  const iconUrl = profileImageUrl ?? (ICON_URLS[type]?.[variant] || ICON_URLS[type]?.[1] || ICON_URLS['bear'][1])

  return (
    <div
      className={`relative overflow-hidden rounded-full flex-shrink-0 ${className}`}
      style={{ width: size, height: size }}
    >
      <img
        src={iconUrl}
        alt={`${type} icon`}
        className="h-full w-full object-cover"
      />
    </div>
  )
}
