import api from "@/src/lib/api";
import type {
  UserLoginRequest,
  UserLoginResponse,
  UserProfileResponse,
  UserSignUpRequest,
} from "@/src/types/user";

export async function signUp(request: UserSignUpRequest): Promise<string> {
  const response = await api.post<string>("/api/users/signup", request);
  return response.data;
}

export async function login(
  request: UserLoginRequest,
): Promise<UserLoginResponse> {
  const response = await api.post<UserLoginResponse>("/api/users/login", request);
  return response.data;
}

export async function getMyProfile(): Promise<UserProfileResponse> {
  const response = await api.get<UserProfileResponse>("/api/users/me");
  return response.data;
}

export async function getUserProfile(userId: number): Promise<UserProfileResponse> {
  console.log("userApi - getUserProfile 호출:", userId);
  try {
    const response = await api.get<UserProfileResponse>(`/api/users/${userId}`);
    console.log("userApi - API 응답:", response.data);
    return response.data;
  } catch (error) {
    console.error("userApi - getUserProfile 에러:", error);
    throw error;
  }
}
