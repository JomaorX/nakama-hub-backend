import { Post } from './post.model';

/** Perfil propio, el único sitio donde la API devuelve el email. */
export interface UserProfile {
  id: number;
  username: string;
  email: string;
  bio: string | null;
  avatarUrl: string | null;
  role: string;
  followersCount: number;
  followingCount: number;
  postsCount: number;
  reputationPoints: number;
  posts: Post[];
}

export interface PublicProfile {
  id: number;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
  followersCount: number;
  followingCount: number;
  postsCount: number;
  posts: Post[] | null;
  /** Si quien consulta ya sigue a esta cuenta. */
  followedByMe: boolean;
  /** Si el perfil es el de quien consulta. */
  own: boolean;
}

export interface UserSearchResult {
  id: number;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
  followersCount: number;
  reputationPoints: number;
}

export interface Serie {
  id: number;
  name: string;
  description: string | null;
}
