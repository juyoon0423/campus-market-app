const KAKAO_MAP_SCRIPT_ID = "kakao-maps-sdk";

let kakaoMapsPromise: Promise<void> | null = null;

// 카카오맵 SDK 스크립트를 한 번만 로드해서 window.kakao.maps를 쓸 수 있게 만든다.
// 여러 컴포넌트(등록/수정 지도, 상세 페이지 지도)가 동시에 마운트돼도 중복 로드되지 않도록
// 진행 중인 Promise를 캐싱한다.
export function loadKakaoMaps(): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("카카오맵은 브라우저에서만 로드할 수 있습니다."));
  }

  if (window.kakao?.maps) {
    return Promise.resolve();
  }

  if (kakaoMapsPromise) {
    return kakaoMapsPromise;
  }

  const appKey = process.env.NEXT_PUBLIC_KAKAO_MAP_KEY;
  if (!appKey) {
    return Promise.reject(new Error("카카오맵 API 키(NEXT_PUBLIC_KAKAO_MAP_KEY)가 설정되지 않았습니다."));
  }

  kakaoMapsPromise = new Promise<void>((resolve, reject) => {
    const existingScript = document.getElementById(KAKAO_MAP_SCRIPT_ID) as HTMLScriptElement | null;

    const onLoad = () => {
      window.kakao.maps.load(() => resolve());
    };

    if (existingScript) {
      existingScript.addEventListener("load", onLoad);
      existingScript.addEventListener("error", () =>
        reject(new Error("카카오맵 스크립트를 불러오지 못했습니다.")),
      );
      return;
    }

    const script = document.createElement("script");
    script.id = KAKAO_MAP_SCRIPT_ID;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&libraries=services&autoload=false`;
    script.async = true;
    script.onload = onLoad;
    script.onerror = () => reject(new Error("카카오맵 스크립트를 불러오지 못했습니다."));
    document.head.appendChild(script);
  }).catch((error) => {
    kakaoMapsPromise = null; // 실패하면 재시도할 수 있도록 캐시를 비운다
    throw error;
  });

  return kakaoMapsPromise;
}
