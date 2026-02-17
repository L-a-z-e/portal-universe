<script setup lang="ts">
import { ref, computed } from 'vue'
import { Avatar } from '@portal/design-vue'
import type { CommentResponse } from '@/dto/comment'
import CommentForm from './CommentForm.vue'
import { formatRelativeTime } from '@/composables/useRelativeTime'

interface Props {
  comment: CommentResponse
  depth: number
  replies?: CommentResponse[]
  currentUserId?: string
}

const props = withDefaults(defineProps<Props>(), {
  depth: 0,
  replies: () => []
})

const emit = defineEmits<{
  (e: 'reply', parentCommentId: string): void
  (e: 'edit', commentId: string, content: string): void
  (e: 'delete', commentId: string): void
  (e: 'cancelReply', commentId: string): void
  (e: 'submitReply', commentId: string, content: string): void
  (e: 'toggleReplies', commentId: string): void
}>()

// 상태 관리
const isEditMode = ref(false)
const showReplyForm = ref(false)
const isRepliesExpanded = ref(false)

// 들여쓰기 계산 (최대 2단계까지만)
const paddingLeft = computed(() => {
  const level = Math.min(props.depth, 2)
  return `${level * 2.5}rem`
})

// 본인 댓글 여부 (currentUserId prop과 비교)
const isOwnComment = computed(() => {
  if (!props.currentUserId) return false
  return props.comment.authorId === props.currentUserId
})

// 답글 개수
const replyCount = computed(() => props.replies?.length || 0)

// 수정 모드 토글
const toggleEditMode = () => {
  isEditMode.value = !isEditMode.value
}

// 답글 폼 토글
const toggleReplyForm = () => {
  showReplyForm.value = !showReplyForm.value
  if (!showReplyForm.value) {
    emit('cancelReply', props.comment.id)
  }
}

// 답글 목록 토글
const toggleReplies = () => {
  isRepliesExpanded.value = !isRepliesExpanded.value
  emit('toggleReplies', props.comment.id)
}

// 수정 제출
const handleEditSubmit = (content: string) => {
  emit('edit', props.comment.id, content)
  isEditMode.value = false
}

// 답글 제출
const handleReplySubmit = (content: string) => {
  emit('submitReply', props.comment.id, content)
  showReplyForm.value = false
}

// 삭제
const handleDelete = () => {
  if (confirm('댓글을 삭제하시겠습니까?')) {
    emit('delete', props.comment.id)
  }
}

// 답글 폼 취소
const handleReplyCancel = () => {
  showReplyForm.value = false
  emit('cancelReply', props.comment.id)
}
</script>

<template>
  <div
    class="comment-item"
    :style="{ paddingLeft }"
    :class="{
      'border-l-2 border-border-muted': depth > 0,
      'opacity-60': comment.isDeleted
    }"
  >
    <div class="py-4">
      <!-- 삭제된 댓글 -->
      <div v-if="comment.isDeleted" class="text-text-meta italic text-sm">
        삭제된 댓글입니다.
      </div>

      <!-- 정상 댓글 -->
      <div v-else>
        <!-- 수정 모드가 아닐 때 -->
        <div v-if="!isEditMode">
          <!-- 헤더: 아바타 + 정보 -->
          <div class="flex items-start gap-3 mb-3">
            <Avatar :name="comment.authorName" size="sm" class="flex-shrink-0 mt-0.5" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="font-semibold text-text-heading text-sm">
                    {{ comment.authorName }}
                  </span>
                  <span class="text-xs text-text-meta">
                    {{ formatRelativeTime(comment.createdAt) }}
                    <span v-if="comment.updatedAt !== comment.createdAt" class="ml-1">(수정됨)</span>
                  </span>
                </div>

                <!-- 수정/삭제 버튼 (본인만) -->
                <div v-if="isOwnComment" class="flex gap-1">
                  <button
                    class="text-xs text-text-meta hover:text-text-heading transition-colors px-1"
                    @click="toggleEditMode"
                  >
                    수정
                  </button>
                  <button
                    class="text-xs text-text-meta hover:text-status-error transition-colors px-1"
                    @click="handleDelete"
                  >
                    삭제
                  </button>
                </div>
              </div>

              <!-- 댓글 내용 -->
              <p class="text-text-body whitespace-pre-wrap text-sm leading-relaxed mt-1">
                {{ comment.content }}
              </p>

              <!-- 액션 버튼 -->
              <div class="flex items-center gap-3 mt-2 text-xs">
                <button
                  class="text-text-meta hover:text-brand-primary font-medium transition-colors"
                  @click="toggleReplyForm"
                >
                  답글
                </button>

                <button
                  v-if="replyCount > 0"
                  class="text-brand-primary hover:text-brand-primaryHover font-medium transition-colors"
                  @click="toggleReplies"
                >
                  {{ isRepliesExpanded ? '답글 접기' : `답글 ${replyCount}개` }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 수정 모드 -->
        <div v-else>
          <CommentForm
            :post-id="comment.postId"
            :initial-content="comment.content"
            mode="edit"
            @submit="handleEditSubmit"
            @cancel="toggleEditMode"
          />
        </div>
      </div>
    </div>

    <!-- 답글 입력 폼 -->
    <div v-if="showReplyForm && !comment.isDeleted" class="ml-10 mb-3">
      <CommentForm
        :post-id="comment.postId"
        :parent-comment-id="comment.id"
        mode="reply"
        placeholder="답글을 입력하세요..."
        @submit="handleReplySubmit"
        @cancel="handleReplyCancel"
      />
    </div>

    <!-- 답글 목록 (재귀) -->
    <div v-if="isRepliesExpanded && replyCount > 0 && !comment.isDeleted">
      <CommentItem
        v-for="reply in replies"
        :key="reply.id"
        :comment="reply"
        :depth="depth + 1"
        :replies="[]"
        :current-user-id="currentUserId"
        @reply="emit('reply', $event)"
        @edit="emit('edit', $event, $event)"
        @delete="emit('delete', $event)"
        @cancel-reply="emit('cancelReply', $event)"
        @submit-reply="emit('submitReply', $event, $event)"
        @toggle-replies="emit('toggleReplies', $event)"
      />
    </div>
  </div>
</template>
