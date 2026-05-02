"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/src/context/AuthContext";
import type { UserProfileResponse } from "@/src/types/user";
import { getUserProfile } from "@/src/lib/apis/userApi";

interface SellerProfileProps {
  sellerId: number;
  sellerName: string;
}

export default function SellerProfile({ sellerId, sellerName }: SellerProfileProps) {
  const [sellerProfile, setSellerProfile] = useState<UserProfileResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { isLoggedIn } = useAuth();

  useEffect(() => {
    if (isLoggedIn && sellerId && sellerId > 0) {
      fetchSellerProfile();
    }
  }, [isLoggedIn, sellerId]);

  const fetchSellerProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const token = localStorage.getItem('token');
      
      if (!token) {
        setError("로그인이 필요합니다.");
        return;
      }
      
      const response = await fetch(`http://localhost:8080/api/users/${sellerId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        if (response.status === 404) {
          setError("판매자 정보를 찾을 수 없습니다.");
        } else if (response.status === 401) {
          setError("인증이 만료되었습니다. 다시 로그인해주세요.");
        } else if (response.status === 403) {
          setError("판매자 정보를 볼 권한이 없습니다.");
        } else {
          setError(`판매자 정보를 불러올 수 없습니다 (${response.status}).`);
        }
        return;
      }
      
      const profile = await response.json();
      
      if (!profile || typeof profile.trustScore !== 'number') {
        setError("판매자 정보 형식이 올바르지 않습니다.");
        return;
      }
      
      setSellerProfile(profile);
    } catch (err) {
      setError("판매자 정보를 불러올 수 없습니다.");
    } finally {
      setLoading(false);
    }
  };

  const getTrustScoreColor = (score: number) => {
    if (score >= 3.6) return "bg-red-500"; // 높은 신뢰도 (빨간색)
    if (score >= 1.6) return "bg-orange-500"; // 중간 신뢰도 (주황색)
    return "bg-gray-400"; // 낮은 신뢰도 (회색)
  };

  const getTrustScoreTextColor = (score: number) => {
    if (score >= 3.6) return "text-red-600";
    if (score >= 1.6) return "text-orange-600";
    return "text-gray-600";
  };

  const getTrustScoreEmoji = (score: number) => {
    if (score >= 3.6) return "🔥";
    if (score >= 1.6) return "🌡️";
    return "❄️";
  };

  const getTrustScoreText = (score: number) => {
    if (score >= 3.6) return "신뢰도 매우 높음";
    if (score >= 1.6) return "신뢰도 보통";
    return "신뢰도 낮음";
  };

  const formatTrustScore = (score: number) => {
    return score.toFixed(1);
  };

  // 로그인하지 않은 경우
  if (!isLoggedIn) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
        <div className="flex items-center space-x-2">
          <div className="text-gray-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <div>
            <p className="text-xs font-medium text-gray-700">{sellerName} 판매자</p>
            <p className="text-xs text-gray-500">로그인 후 신뢰 지수를 확인하세요</p>
          </div>
        </div>
      </div>
    );
  }

  // 로딩 상태
  if (loading) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
        <div className="flex items-center space-x-2">
          <div className="animate-spin">
            <div className="w-5 h-5 border-2 border-gray-300 border-t-gray-600 rounded-full" />
          </div>
          <p className="text-xs text-gray-600">판매자 정보 로딩 중...</p>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error || !sellerProfile) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
        <div className="flex items-center space-x-2">
          <div className="text-gray-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <p className="text-xs font-medium text-gray-700">{sellerName} 판매자</p>
            <p className="text-xs text-gray-500">
              {error || "판매자 정보를 불러올 수 없습니다."}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // 정상 상태 - 판매자 정보 표시
  return (
    <div className="bg-white border border-gray-200 rounded-lg p-3 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div>
          <h3 className="text-sm font-semibold text-gray-900">{sellerProfile.name} 판매자</h3>
          <p className="text-xs text-gray-600">{sellerProfile.department}</p>
        </div>
        <div className="text-2xl">
          {getTrustScoreEmoji(sellerProfile.trustScore)}
        </div>
      </div>

      {/* 신뢰 지수 온도계 */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-gray-700">신뢰 지수</span>
          <div className="flex items-center space-x-1">
            <span className={`text-lg font-bold ${getTrustScoreTextColor(sellerProfile.trustScore)}`}>
              {formatTrustScore(sellerProfile.trustScore)}°C
            </span>
            <span className={`text-xs font-medium ${getTrustScoreTextColor(sellerProfile.trustScore)}`}>
              {getTrustScoreText(sellerProfile.trustScore)}
            </span>
          </div>
        </div>

        {/* 온도계 프로그레스 바 */}
        <div className="relative">
          <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
            <div
              className={`h-full transition-all duration-500 ease-out ${getTrustScoreColor(sellerProfile.trustScore)}`}
              style={{ width: `${Math.min((sellerProfile.trustScore / 5.0) * 100, 100)}%` }}
            />
          </div>
          
          {/* 온도계 눈금 */}
          <div className="absolute inset-0 flex items-center justify-between px-1">
            <div className="w-0.5 h-1.5 bg-white opacity-50 rounded" />
            <div className="w-0.5 h-1.5 bg-white opacity-50 rounded" />
            <div className="w-0.5 h-1.5 bg-white opacity-50 rounded" />
            <div className="w-0.5 h-1.5 bg-white opacity-50 rounded" />
            <div className="w-0.5 h-1.5 bg-white opacity-50 rounded" />
          </div>
        </div>

        {/* 온도 범위 표시 */}
        <div className="flex justify-between text-xs text-gray-500">
          <span>0°C</span>
          <span>1.5°C</span>
          <span>3.5°C</span>
          <span>5.0°C</span>
        </div>

        {/* 신뢰도 설명 */}
        <div className="mt-2 p-2 bg-gray-50 rounded-lg">
          <div className="flex items-start space-x-1">
            <div className="text-xs text-gray-500 mt-0.5">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div className="flex-1">
              <p className="text-xs text-gray-600">
                이 판매자의 신뢰 지수는 {formatTrustScore(sellerProfile.trustScore)}°C로, 
                {sellerProfile.trustScore >= 3.6 ? "매우 신뢰할 수 있는 판매자입니다." : 
                 sellerProfile.trustScore >= 1.6 ? "보통 수준의 신뢰도를 가진 판매자입니다." : 
                 "신뢰도가 낮은 판매자입니다. 주의가 필요합니다."}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
