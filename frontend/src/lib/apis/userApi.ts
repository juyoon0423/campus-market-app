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
