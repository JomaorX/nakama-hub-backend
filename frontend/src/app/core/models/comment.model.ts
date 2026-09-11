export interface Comment {
  id: number;
  content: string;
  postId: number;
  authorUsername: string;
  parentId: number | null;
  createdAt: string;
  updatedAt: string;
  edited: boolean;
  /** Cuántas respuestas cuelgan de este comentario. */
  replyCount: number;
  /** Si quien consulta es el autor. */
  own: boolean;
}

export interface CreateCommentPayload {
  content: string;
  postId: number;
  parentId?: number | null;
}
