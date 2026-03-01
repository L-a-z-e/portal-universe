<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Card, Spinner, useApiError } from '@portal/design-vue'
import type { CommentResponse } from '@/dto/comment'
import {
  getCommentsByPostId,
  createComment,
  updateComment,
  deleteComment
} from '@/api/comments'
import CommentForm from './CommentForm.vue'
import CommentItem from './CommentItem.vue'

interface Props {
  postId: string
  currentUserId?: string
}

const props = defineProps<Props>()
const { handleError, getErrorMessage } = useApiError()

// 상태 관리
const comments = ref<CommentResponse[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

// 댓글 트리 구조 생성
const commentTree = computed(() => {
  const rootComments: (CommentResponse & { replies?: CommentResponse[] })[] = []
  const commentMap = new Map<string, CommentResponse & { replies?: CommentResponse[] }>()

  // 1단계: 모든 댓글을 Map에 저장
  comments.value.forEach(comment => {
    commentMap.set(comment.id, { ...comment, replies: [] })
  })

  // 2단계: 부모-자식 관계 구성
  comments.value.forEach(comment => {
    const commentWithReplies = commentMap.get(comment.id)!

    if (comment.parentCommentId) {
      // 대댓글인 경우, 부모 댓글의 replies에 추가
      const parent = commentMap.get(comment.parentCommentId)
      if (parent) {
        parent.replies = parent.replies || []
        parent.replies.push(commentWithReplies)
      } else {
        // 부모를 찾을 수 없으면 루트로 처리
        rootComments.push(commentWithReplies)
      }
    } else {
      // 루트 댓글
      rootComments.push(commentWithReplies)
    }
  })

  return rootComments
})

// 댓글 개수
const totalCommentCount = computed(() => comments.value.length)

// 댓글 로드
async function loadComments() {
  isLoading.value = true
  error.value = null

  try {
    comments.value = await getCommentsByPostId(props.postId)
  } catch (e) {
    console.error('댓글 로드 실패:', e)
    error.value = getErrorMessage(e, '댓글을 불러오지 못했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 새 댓글 작성
async function handleCreateComment(content: string) {
  try {
    const payload = {
      postId: props.postId,
      content: content,
      parentCommentId: null
    }

    const newComment = await createComment(payload)
    comments.value.push(newComment)
  } catch (e) {
    console.error('댓글 작성 실패:', e)
    handleError(e, '댓글 작성에 실패했습니다.')
  }
}

// 답글 작성
async function handleReplySubmit(parentCommentId: string, content: string) {
  try {
    const payload = {
      postId: props.postId,
      content: content,
      parentCommentId: parentCommentId
    }

    const newComment = await createComment(payload)
    comments.value.push(newComment)
  } catch (e) {
    console.error('답글 작성 실패:', e)
    handleError(e, '답글 작성에 실패했습니다.')
  }
}

// 댓글 수정
async function handleEditComment(commentId: string, content: string) {
  try {
    const updated = await updateComment(commentId, { content })

    const index = comments.value.findIndex(c => c.id === commentId)
    if (index !== -1) {
      comments.value[index] = updated
    }
  } catch (e) {
    console.error('댓글 수정 실패:', e)
    handleError(e, '댓글 수정에 실패했습니다.')
  }
}

// 댓글 삭제
async function handleDeleteComment(commentId: string) {
  try {
    await deleteComment(commentId)

    // 서버에서 isDeleted: true로 반환하는 경우를 고려
    // 실제로는 삭제된 댓글을 유지하거나 제거할 수 있음
    const comment = comments.value.find(c => c.id === commentId)
    if (comment) {
      comment.isDeleted = true
    }
  } catch (e) {
    console.error('댓글 삭제 실패:', e)
    handleError(e, '댓글 삭제에 실패했습니다.')
  }
}

// 초기 로드
onMounted(() => {
  loadComments()
})
</script>

<template>
  <div class="comment-list">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-text-heading">
        💬 댓글 <span class="text-brand-primary">{{ totalCommentCount }}</span>
      </h2>
    </div>

    <Card class="bg-bg-card border-border-default p-6">
      <!-- 로딩 -->
      <div v-if="isLoading" class="text-center py-12">
        <Spinner size="md" class="mx-auto mb-3" />
        <p class="text-text-meta text-sm">댓글을 불러오는 중...</p>
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="text-center py-12 text-status-error">
        {{ error }}
      </div>

      <!-- 댓글 목록 -->
      <div v-else>
        <!-- 댓글 없음 -->
        <div v-if="totalCommentCount === 0" class="text-center py-12 text-text-meta">
          <div class="text-4xl mb-3">💭</div>
          <p>아직 댓글이 없습니다. 첫 댓글을 남겨보세요!</p>
        </div>

        <!-- 댓글 트리 -->
        <div v-else class="space-y-3 mb-8">
          <CommentItem
            v-for="comment in commentTree"
            :key="comment.id"
            :comment="comment"
            :depth="0"
            :replies="comment.replies || []"
            :current-user-id="currentUserId"
            @reply="() => {}"
            @edit="handleEditComment"
            @delete="handleDeleteComment"
            @cancel-reply="() => {}"
            @submit-reply="handleReplySubmit"
            @toggle-replies="() => {}"
          />
        </div>

        <!-- 새 댓글 작성 -->
        <div class="border-t border-border-default pt-6">
          <label class="block text-sm font-medium text-text-heading mb-3">
            댓글 작성
          </label>
          <CommentForm
            :post-id="postId"
            mode="create"
            @submit="handleCreateComment"
          />
        </div>
      </div>
    </Card>
  </div>
</template>

<style scoped>
.comment-list {
  margin-top: 3rem;
}
</style>
