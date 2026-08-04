// 카카오맵 JS SDK의 최소 타입 선언. 공식 @types 패키지가 없어 실제로 쓰는 API만 선언한다.
export {};

declare global {
  namespace kakao.maps {
    class LatLng {
      constructor(latitude: number, longitude: number);
      getLat(): number;
      getLng(): number;
    }

    interface MapOptions {
      center: LatLng;
      level?: number;
    }

    class Map {
      constructor(container: HTMLElement, options: MapOptions);
      setCenter(latlng: LatLng): void;
      getCenter(): LatLng;
      setLevel(level: number): void;
      relayout(): void;
    }

    interface MarkerOptions {
      position: LatLng;
      map?: Map;
    }

    class Marker {
      constructor(options: MarkerOptions);
      setMap(map: Map | null): void;
      setPosition(latlng: LatLng): void;
      getPosition(): LatLng;
    }

    interface MouseEvent {
      latLng: LatLng;
    }

    namespace event {
      function addListener(
        target: Map | Marker,
        type: string,
        handler: (event: MouseEvent) => void,
      ): void;
    }

    namespace services {
      type Status = "OK" | "ZERO_RESULT" | "ERROR";

      interface Address {
        address_name: string;
      }

      interface Coord2AddressResult {
        address: Address | null;
        road_address: Address | null;
      }

      class Geocoder {
        coord2Address(
          longitude: number,
          latitude: number,
          callback: (result: Coord2AddressResult[], status: Status) => void,
        ): void;
      }
    }

    function load(callback: () => void): void;
  }

  interface Window {
    kakao: typeof kakao;
  }
}
