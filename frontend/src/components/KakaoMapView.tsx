"use client";

import { useEffect, useRef, useState } from "react";
import { loadKakaoMaps } from "@/src/lib/kakaoMap";

type KakaoMapViewProps = {
  latitude: number;
  longitude: number;
  locationName?: string | null;
};

// 구매자에게 판매자가 등록한 거래 희망 장소를 보여주기만 하는 읽기 전용 지도.
export default function KakaoMapView({ latitude, longitude, locationName }: KakaoMapViewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    let cancelled = false;

    loadKakaoMaps()
      .then(() => {
        if (cancelled || !containerRef.current) {
          return;
        }

        const center = new window.kakao.maps.LatLng(latitude, longitude);
        const map = new window.kakao.maps.Map(containerRef.current, { center, level: 4 });
        new window.kakao.maps.Marker({ position: center, map });
        setIsLoading(false);
      })
      .catch((error: Error) => {
        if (!cancelled) {
          setLoadError(error.message);
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [latitude, longitude]);

  return (
    <div>
      <div
        ref={containerRef}
        className="h-56 w-full overflow-hidden rounded-lg border border-slate-200 bg-slate-50"
      />
      {isLoading ? (
        <p className="mt-2 text-xs text-slate-500">지도를 불러오는 중...</p>
      ) : loadError ? (
        <p className="mt-2 text-xs text-red-600">{loadError}</p>
      ) : locationName ? (
        <p className="mt-2 text-sm text-slate-700">{locationName}</p>
      ) : null}
    </div>
  );
}
