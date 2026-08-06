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
  const response = await api.get<UserProfileResponse>(`/api/users/${userId}`);
  return response.data;
}

export async function sendVerificationCode(email: string): Promise<void> {
  await api.post("/api/users/emails/verification-requests", null, {
    params: { email },
  });
}

export async function verifyEmailCode(email: string, code: string): Promise<void> {
  await api.get("/api/users/emails/verifications", {
    params: { email, code },
  });
}
