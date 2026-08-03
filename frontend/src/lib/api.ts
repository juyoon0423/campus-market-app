import axios, { AxiosError } from "axios";

export type OnUnauthorizedCallback = () => void;

let onUnauthorizedCallback: OnUnauthorizedCallback | null = null;

export const setOnUnauthorized = (callback: OnUnauthorizedCallback) => {
  onUnauthorizedCallback = callback;
};

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
});

// Request interceptor: Add Authorization header
api.interceptors.request.use((config) => {
  if (typeof window === "undefined") {
    return config;
  }

  const token = localStorage.getItem("accessToken");

  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }

  return config;
});

// Response interceptor: Handle 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      if (typeof window !== "undefined") {
        localStorage.removeItem("accessToken");
        if (onUnauthorizedCallback) {
          onUnauthorizedCallback();
        }
      }
    }
    return Promise.reject(error);
  },
);

export default api;
