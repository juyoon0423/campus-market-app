"use client";

import { useEffect, useRef, useState } from "react";
import { loadKakaoMaps } from "@/src/lib/kakaoMap";

// 지도가 처음 뜰 때 기준으로 삼는 좌표(서울시청). 사용자의 현재 위치를 가져올 수 없을 때만 쓰인다.
const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };

export type TradeLocation = {
  locationName: string;
  latitude: number;
  longitude: number;
};

type KakaoMapPickerProps = {
  initialLocation?: TradeLocation | null;
  onChange: (location: TradeLocation) => void;
};

export default function KakaoMapPicker({ initialLocation, onChange }: KakaoMapPickerProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<kakao.maps.Map | null>(null);
  const markerRef = useRef<kakao.maps.Marker | null>(null);
  const geocoderRef = useRef<kakao.maps.services.Geocoder | null>(null);
  const onChangeRef = useRef(onChange);
  const lastPositionRef = useRef<{ latitude: number; longitude: number } | null>(
    initialLocation ? { latitude: initialLocation.latitude, longitude: initialLocation.longitude } : null,
  );
  // 사용자가 지도를 직접 클릭해 위치를 고르면, 뒤늦게 도착하는 geolocation 콜백이
  // 그 선택을 덮어쓰지 않도록 막는다(사용자가 서울을 클릭했는데 한 박자 늦게 도착한
  // 브라우저 위치 정보가 다른 지역으로 마커를 옮겨버리는 문제가 실기동 테스트에서 확인됨).
  const hasUserPickedRef = useRef(Boolean(initialLocation));

  const [locationName, setLocationName] = useState(initialLocation?.locationName ?? "");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  const applyPosition = (latitude: number, longitude: number, name?: string) => {
    const map = mapRef.current;
    const marker = markerRef.current;
    if (!map || !marker) {
      return;
    }

    const position = new window.kakao.maps.LatLng(latitude, longitude);
    marker.setPosition(position);
    map.setCenter(position);
    lastPositionRef.current = { latitude, longitude };

    if (name !== undefined) {
      setLocationName(name);
      onChangeRef.current({ locationName: name, latitude, longitude });
      return;
    }

    geocoderRef.current?.coord2Address(longitude, latitude, (result, status) => {
      const resolvedName =
        status === "OK"
          ? result[0]?.road_address?.address_name ?? result[0]?.address?.address_name ?? ""
          : "";
      setLocationName(resolvedName);
      onChangeRef.current({ locationName: resolvedName, latitude, longitude });
    });
  };

  const handleLocationNameChange = (name: string) => {
    setLocationName(name);
    const position = lastPositionRef.current;
    if (position) {
      onChangeRef.current({ locationName: name, ...position });
    }
  };

  useEffect(() => {
    let cancelled = false;

    loadKakaoMaps()
      .then(() => {
        if (cancelled || !containerRef.current) {
          return;
        }

        const startCenter = initialLocation
          ? { latitude: initialLocation.latitude, longitude: initialLocation.longitude }
          : DEFAULT_CENTER;

        const center = new window.kakao.maps.LatLng(startCenter.latitude, startCenter.longitude);
        const map = new window.kakao.maps.Map(containerRef.current, { center, level: 4 });
        const marker = new window.kakao.maps.Marker({ position: center, map });

        mapRef.current = map;
        markerRef.current = marker;
        geocoderRef.current = new window.kakao.maps.services.Geocoder();

        window.kakao.maps.event.addListener(map, "click", (event) => {
          hasUserPickedRef.current = true;
          applyPosition(event.latLng.getLat(), event.latLng.getLng());
        });

        setIsLoading(false);

        if (!initialLocation && navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(
            (position) => {
              if (!cancelled && !hasUserPickedRef.current) {
                applyPosition(position.coords.latitude, position.coords.longitude);
              }
            },
            () => {
              // 위치 권한 거부/실패 시 기본 좌표(서울시청)를 그대로 사용한다.
            },
          );
        }
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
    // initialLocation은 지도를 처음 만들 때만 쓰는 시작 좌표라 의도적으로 deps에서 뺐다(마운트 시 1회만 실행).
  }, []);

  return (
    <div>
      <div
        ref={containerRef}
        className="h-64 w-full overflow-hidden rounded-lg border border-slate-300 bg-slate-50"
      />
      {isLoading ? (
        <p className="mt-2 text-xs text-slate-500">지도를 불러오는 중...</p>
      ) : loadError ? (
        <p className="mt-2 text-xs text-red-600">{loadError}</p>
      ) : (
        <p className="mt-2 text-xs text-slate-500">
          지도를 클릭해 거래 희망 장소를 선택해주세요.
        </p>
      )}
      {!isLoading && !loadError ? (
        <input
          type="text"
          value={locationName}
          onChange={(event) => handleLocationNameChange(event.target.value)}
          placeholder="지도를 클릭하면 주소가 자동으로 채워져요. 필요하면 직접 수정해도 됩니다."
          className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-slate-500"
        />
      ) : null}
    </div>
  );
}
