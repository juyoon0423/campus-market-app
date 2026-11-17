// 화면 표시용 포맷 함수. 데이터 로직이 아니라 표기만 담당한다.

/** 20000 -> "20,000원" */
export function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

/**
 * 서버가 내려준 등록 시각(시간대 표기 없는 ISO, 서버 시간대 Asia/Seoul)을 "3시간 전"처럼 표시한다.
 * 값이 없거나 해석할 수 없으면 null을 돌려 화면에서 생략한다.
 */
export function formatRelativeTime(iso?: string | null, now: Date = new Date()): string | null {
  if (!iso) return null;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;

  const minutes = Math.floor((now.getTime() - date.getTime()) / 60000);
  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
}
