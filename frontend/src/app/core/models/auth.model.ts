export interface Session {
  id: number;
  username: string;
  email: string;
  accessToken: string;
  refreshToken: string;
  /** Segundos de validez del token de acceso. */
  expiresIn: number;
}

export interface LoginPayload {
  /** Nombre de usuario o email. */
  identifier: string;
  password: string;
}

export interface SignupPayload {
  email: string;
  username: string;
  password: string;
}

/** Cuerpo de error unificado que devuelve la API. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
