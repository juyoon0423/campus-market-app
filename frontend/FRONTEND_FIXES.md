# 프론트엔드 수정 완료 가이드

## ✅ 해결된 문제

### 문제 A: 채팅방 중복 생성

**증상:** 상품 상세 페이지에서 "채팅 문의하기" 클릭 → API 2회 호출 → 중복 채팅방 생성

**원인:**

- 이전: 링크 방식으로 `/chat?productId=123` 이동
- 채팅 페이지에서 useEffect가 productId를 감지하면 자동으로 API 호출
- React Strict Mode에서 useEffect 중복 실행 가능성
- 중복 클릭 방지 로직 없음

**수정 사항:**

#### 1. 상품 상세 페이지 (`app/products/[id]/page.tsx`)

```typescript
// 변경 전: Link 방식
<Link href={isLoggedIn ? `/chat?productId=${productId}` : "/login"}>
  채팅 문의하기
</Link>

// 변경 후: Button + 직접 API 호출
const [isChatLoading, setIsChatLoading] = useState(false);

const handleInitiateChat = async () => {
  if (!isLoggedIn || isChatLoading) return;

  setIsChatLoading(true);
  try {
    const room = await createOrGetChatRoom(productId);
    router.push(`/chat?roomId=${room.id}`);  // ← productId 대신 roomId
  } catch {
    alert("채팅방을 생성할 수 없습니다.");
    setIsChatLoading(false);
  }
};

// 버튼: disabled 상태로 중복 클릭 방지
<button
  type="button"
  onClick={handleInitiateChat}
  disabled={isChatLoading}  // ← 중복 클릭 방지
  className="... disabled:opacity-50 disabled:cursor-not-allowed"
>
  {isChatLoading ? "채팅방 생성 중..." : "채팅 문의하기"}
</button>
```

#### 2. 채팅 페이지 (`app/chat/page.tsx`)

```typescript
// 추가된 상태
const [isCreatingChatRoom, setIsCreatingChatRoom] = useState(false);

// 개선된 useEffect
useEffect(() => {
  if (!isLoggedIn) return;

  const openRequestedRoom = async () => {
    if (hasRoomIdQuery) {
      setSelectedRoomId(roomIdFromQuery);
      return;
    }

    if (hasProductIdQuery) {
      // ✅ 중복 생성 방지 플래그
      if (isCreatingChatRoom) {
        return; // 이미 생성 중이면 중단
      }

      setIsCreatingChatRoom(true);
      try {
        const room = await createOrGetChatRoom(productIdFromQuery);
        setSelectedRoomId(room.id);
        setRooms((prev) => {
          if (!Array.isArray(prev)) return [room];
          if (prev.some((item) => item.id === room.id)) return prev;
          return [room, ...prev];
        });
      } catch {
        // 에러 처리...
      } finally {
        setIsCreatingChatRoom(false);
      }
    }
  };

  openRequestedRoom();
}, [
  hasProductIdQuery,
  hasRoomIdQuery,
  isLoggedIn,
  productIdFromQuery,
  roomIdFromQuery,
  isCreatingChatRoom,
]);
```

**개선 효과:**

- ✅ 버튼 disabled로 즉각적인 중복 클릭 방지
- ✅ roomId 기반 이동으로 채팅 페이지의 자동 API 호출 제거
- ✅ isCreatingChatRoom 플래그로 double-check
- ✅ 에러 시 로딩 상태 복구

---

### 문제 B: 한글 전송 시 글자 맺음 오류

**증상:**

- "안녕하세요" 입력 → Enter 클릭 → "요"가 입력창에 남거나 중복 전송
- 자음/모음만 입력할 때는 정상

**원인:**

- 한글은 IME(Input Method Editor)를 통해 조합되어 완성됨
- 조합 중에도 `keyDown` 이벤트 발생
- `event.key === "Enter"` 체크만으로는 완성되지 않은 글자가 전송됨

**수정 사항:**

#### 채팅 페이지 (`app/chat/page.tsx`)

```typescript
// 변경 전
onKeyDown={(event) => {
  if (event.key === "Enter") {
    event.preventDefault();
    handleSendMessage();
  }
}}

// 변경 후: ✅ IME 조합 상태 확인
onKeyDown={(event) => {
  // IME 조합 중에는 메시지 전송을 하지 않도록 처리
  if (event.key === "Enter" && !event.nativeEvent.isComposing) {
    event.preventDefault();
    handleSendMessage();
  }
}}
```

**개선 효과:**

- ✅ 한글 조합 중 Enter 무시
- ✅ 완성된 글자만 전송
- ✅ 다른 IME(일본어, 중국어 등)도 동시에 지원

---

## 📋 변경 요약

| 파일                         | 변경 사항                        | 이유                  |
| ---------------------------- | -------------------------------- | --------------------- |
| `app/products/[id]/page.tsx` | Link → Button + API 호출         | 중복 호출 방지        |
| `app/products/[id]/page.tsx` | `isChatLoading` 상태 추가        | 버튼 disabled 처리    |
| `app/chat/page.tsx`          | `isCreatingChatRoom` 플래그 추가 | Double-check 안전장치 |
| `app/chat/page.tsx`          | `isComposing` 확인 추가          | 한글 완성 대기        |

---

## 🧪 테스트 가이드

### 문제 A 테스트

```bash
# 시나리오 1: 빠른 중복 클릭
1. 상품 상세 페이지로 이동
2. "채팅 문의하기" 버튼을 빠르게 여러 번 클릭
3. ✅ 버튼이 disabled되어 단 1번만 API 호출
4. ✅ 채팅방 1개만 생성됨

# 시나리오 2: 동시 다중 탭 요청
1. 상품 상세 페이지 → F12 개발자 도구 → Network 탭
2. "채팅 문의하기" 클릭
3. ✅ POST /api/chat/room/{productId} 1회만 호출
4. ✅ 채팅 페이지의 useEffect에서 추가 호출 없음
```

### 문제 B 테스트

```bash
# 시나리오 1: 한글 입력
1. 채팅 페이지의 메시지 입력창에 포커스
2. "안녕하세요" 입력
3. Enter 누르기
4. ✅ 메시지 전송됨
5. ✅ 입력창이 완전히 비워짐

# 시나리오 2: 자음/모음
1. 입력창에 포커스
2. "ㄱ ㄴ ㄷ" 입력 후 Enter
3. ✅ 메시지 전송됨

# 시나리오 3: 영문
1. "Hello" 입력 후 Enter
2. ✅ 메시지 전송됨
```

---

## 📝 추가 권장사항

### 1. 로딩 중 상태 피드백 개선

```typescript
// 상품 상세 페이지
<button
  disabled={isChatLoading}
  className="flex items-center gap-2"
>
  {isChatLoading && (
    <svg className="animate-spin h-4 w-4" /* ... */ />
  )}
  {isChatLoading ? "채팅방 생성 중..." : "채팅 문의하기"}
</button>
```

### 2. 에러 메시지 개선

```typescript
// 에러 발생 시 사용자에게 명확한 안내
catch {
  setErrorMessage("채팅방 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
  setIsChatLoading(false);
}
```

### 3. 디바운싱 추가 (선택사항)

```typescript
import { useCallback, useRef } from "react";

const handleInitiateChat = useCallback(async () => {
  if (!isLoggedIn || isChatLoading) return;

  setIsChatLoading(true);
  // ... 나머지 로직
}, [isLoggedIn, isChatLoading]);
```

---

## 🔍 확인 체크리스트

- [x] 상품 상세 페이지 버튼 disabled 처리
- [x] productId 대신 roomId로 이동
- [x] 채팅 페이지 중복 생성 방지 플래그
- [x] IME 조합 상태 확인 추가
- [x] 로딩 상태 UI 업데이트
- [x] 에러 처리 강화

모든 수정사항이 반영되었습니다! 🎉
