import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "http",
        hostname: "localhost",
        port: "8080",
        pathname: "/images/**",
      },
    ],
    // Next.js 16부터 이미지 최적화 서버가 SSRF 방지 차원에서 private/loopback IP로
    // 풀리는 업스트림을 기본 차단한다(localhost도 해당) — 이 앱의 백엔드가 지금은
    // localhost:8080 하나뿐이라 그대로 두면 next/image가 항상 400을 반환한다.
    // 이미지 URL은 항상 서버(DB)가 내려준 값이고 사용자 입력이 직접 개입하지 않아
    // SSRF 공격 표면은 제한적이라고 판단해 허용함. 실제 배포 도메인이 생기면
    // remotePatterns를 그 도메인으로 바꾸고 이 옵션은 제거해야 한다.
    dangerouslyAllowLocalIP: true,
  },
};

export default nextConfig;
