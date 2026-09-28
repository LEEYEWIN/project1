import client from './client.js';

/**
 * 커뮤니티 게시판 API
 * 글 목록 → { items: [...], page, size, totalPages, totalElements, sort }
 *   type: 'REVIEW' | 'QUESTION', sort: 'latest' | 'likes', page: 0부터
 */
export async function fetchPosts({ type = 'REVIEW', sort = 'latest', page = 0, size = 3 } = {}) {
  const { data } = await client.get('/community/posts', { params: { type, sort, page, size } });
  return data;
}

/** 글 상세 */
export async function fetchPost(postId) {
  const { data } = await client.get(`/community/posts/${postId}`);
  return data;
}

/** 조회수 +1 → { viewCount } */
export async function increaseView(postId) {
  const { data } = await client.post(`/community/posts/${postId}/view`);
  return data;
}

/** 글 쓰기 { postType, title, content, travelId?, imageUrl? } → { postId } */
export async function createPost(payload) {
  const { data } = await client.post('/community/posts', payload);
  return data;
}

/** 글 수정 { title, content, imageUrl } (imageUrl: null이면 사진 삭제) */
export async function updatePost(postId, payload) {
  await client.put(`/community/posts/${postId}`, payload);
}

export async function deletePost(postId) {
  await client.delete(`/community/posts/${postId}`);
}

/** 좋아요 누르기 / 취소 → { liked, likeCount } */
export async function likePost(postId) {
  const { data } = await client.post(`/community/posts/${postId}/like`);
  return data;
}

export async function unlikePost(postId) {
  const { data } = await client.delete(`/community/posts/${postId}/like`);
  return data;
}

/**
 * 사진 올리기 → { imageUrl }
 * Content-Type을 multipart/form-data로 지정해야 axios가 FormData를 JSON으로 바꾸지 않는다
 * (경계값 boundary는 브라우저가 자동으로 붙임)
 */
export async function uploadImage(file) {
  const form = new FormData();
  form.append('image', file);
  const { data } = await client.post('/community/images', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

/** 댓글 목록 (대댓글은 replies 안에) */
export async function fetchComments(postId) {
  const { data } = await client.get(`/community/posts/${postId}/comments`);
  return data;
}

/** 댓글·대댓글 쓰기 { content, parentCommentId? } → { commentId } */
export async function createComment(postId, payload) {
  const { data } = await client.post(`/community/posts/${postId}/comments`, payload);
  return data;
}

export async function updateComment(commentId, content) {
  await client.put(`/community/comments/${commentId}`, { content });
}

export async function deleteComment(commentId) {
  await client.delete(`/community/comments/${commentId}`);
}