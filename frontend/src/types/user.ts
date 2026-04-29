export type UserSignUpRequest = {
  email: string;
  name: string;
  studentId: string;
  department: string;
  password: string;
};

export type UserLoginRequest = {
  email: string;
  password: string;
};

export type UserLoginResponse = {
  token: string;
};

export type UserProfileResponse = {
  name: string;
  studentId: string;
  department: string;
  trustScore: number;
  createdAt: string;
};
