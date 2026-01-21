<script setup lang="ts">
import { ref, watch } from 'vue'
import { Textarea, Button } from '@portal/design-system-vue'

interface Props {
  postId: string
  parentCommentId?: string | null
  initialContent?: string
  mode?: 'create' | 'edit' | 'reply'
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'create',
  placeholder: '댓글을 입력하세요...'
})

const emit = defineEmits<{
  (e: 'submit', content: string): void
  (e: 'cancel'): void
}>()

const content = ref(props.initialContent || '')

// initialContent 변경 감지
watch(() => props.initialContent, (newVal) => {
  content.value = newVal || ''
})

const handleSubmit = () => {
  if (!content.value.trim()) return
  emit('submit', content.value.trim())

  // create/reply 모드일 때만 초기화
  if (props.mode === 'create' || props.mode === 'reply') {
    content.value = ''
  }
}

const handleCancel = () => {
  content.value = ''
  emit('cancel')
}

// 버튼 텍스트
const submitButtonText = props.mode === 'edit' ? '수정' : '등록'
</script>

<template>
  <div class="comment-form space-y-3">
    <Textarea
      v-model="content"
      :rows="mode === 'reply' ? 2 : 3"
      :placeholder="placeholder"
      class="w-full"
    />

    <div class="flex gap-2" :class="mode === 'create' ? 'justify-end' : 'justify-between'">
      <!-- 답글/수정 모드일 때만 취소 버튼 표시 -->
      <Button
        v-if="mode !== 'create'"
        variant="secondary"
        size="sm"
        @click="handleCancel"
      >
        취소
      </Button>

      <Button
        variant="primary"
        size="sm"
        :disabled="!content.trim()"
        @click="handleSubmit"
      >
        {{ submitButtonText }}
      </Button>
    </div>
  </div>
</template>

<style scoped>
.comment-form {
  width: 100%;
}
</style>
