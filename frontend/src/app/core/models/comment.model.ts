export interface Comment {
  id: number;
  content: string;
  postId: number;
  authorUsername: string;
  parentId: number | null;
  createdAt: string;
  updatedAt: string;
  edited: boolean;
}

export interface CreateCommentPayload {
  content: string;
  postId: number;
  parentId?: number | null;
}
