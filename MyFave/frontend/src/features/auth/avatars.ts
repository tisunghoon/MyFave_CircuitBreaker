// 회원가입 시 무작위로 부여되는 곰돌이 프로필 아바타 S3 URL 풀.
// 사용자(MyFave 운영자)가 AWS S3에 업로드한 PNG URL들을 아래 배열에 그대로 채워 넣으면
// 신규 회원 가입 시점에 무작위 1개가 선택되어 user.profileImageUrl 로 저장된다.
//
// 사용자가 URL을 제공하기 전까지는 빈 배열을 유지 — getRandomAvatarUrl()이 undefined를 반환하고
// UserIcon이 기존 fallback 아이콘을 그대로 보여주므로 앱이 깨지지 않는다.
//
// 운영자(2026-05-24) 제공 — MyFave_user_icon 5종 (Default + Variant2~5).
export const BEAR_AVATAR_S3_URLS: readonly string[] = [
  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/MyFave_user_icon/Property+1%3DDefault.png',
  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/MyFave_user_icon/Property+1%3DVariant2.png',
  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/MyFave_user_icon/Property+1%3DVariant3.png',
  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/MyFave_user_icon/Property+1%3DVariant4.png',
  'https://myfave-team-bucket.s3.ap-northeast-2.amazonaws.com/MyFave_user_icon/Property+1%3DVariant5.png',
]

// 회원가입 시 무작위 1개 URL을 반환. 풀이 비어있으면 undefined.
export function getRandomAvatarUrl(): string | undefined {
  if (BEAR_AVATAR_S3_URLS.length === 0) return undefined
  const index = Math.floor(Math.random() * BEAR_AVATAR_S3_URLS.length)
  return BEAR_AVATAR_S3_URLS[index]
}
