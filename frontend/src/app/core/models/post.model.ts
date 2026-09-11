export type ContentType = 'ANIME' | 'MANGA' | 'SERIE' | 'GENERAL';
export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type PrivacyLevel = 'PUBLIC' | 'PRIVATE' | 'FOLLOWERS_ONLY';

export interface Post {
  id: number;
  title: string;
  content: string;
  contentType: ContentType;
  status: PostStatus;
  privacy: PrivacyLevel;
  serieName: string | null;
  categories: string[];
  authorUsername: string;
  imageUrls: string[];
  viewsCount: number;
  likesCount: number;
  createdAt: string;
  updatedAt: string;
  /** Lo calcula el backend con un margen, porque las dos marcas de tiempo difieren al insertar. */
  edited: boolean;
  /** Si quien consulta ya dio me gusta. */
  likedByMe: boolean;
  /** Si quien consulta es el autor. */
  own: boolean;
}

export interface PostPayload {
  title: string;
  content: string;
  contentType: ContentType;
  status?: PostStatus;
  privacy?: PrivacyLevel;
  serieName?: string | null;
  categories: string[];
  imageUrls?: string[];
}
