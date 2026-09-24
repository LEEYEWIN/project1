import client from './client.js';

export async function fetchPosts(type = 'REVIEW') {
  const { data } = await client.get('/community/posts', { params: { type } });
  return data;
}

/** { postType: 'REVIEW', title, content } → { postId } */
export async function createPost(payload) {
  const { data } = await client.post('/community/posts', payload);
  return data;
}