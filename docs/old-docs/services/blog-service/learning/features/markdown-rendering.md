# Markdown Rendering

## 개요

Blog Service에서 마크다운 콘텐츠를 처리하고 렌더링하는 방법을 학습합니다.

## 아키텍처

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Frontend      │────▶│   Backend       │────▶│   MongoDB       │
│   (Editor)      │     │   (Store Raw)   │     │   (Markdown)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │
        ▼
┌─────────────────┐
│   Frontend      │
│   (Render HTML) │
└─────────────────┘
```

**접근 방식:** 백엔드는 원본 마크다운 저장, 프론트엔드에서 렌더링

## 백엔드: 원본 저장

### Post Entity

```java
@Document(collection = "posts")
public class Post {
    // 마크다운 원본 저장
    @TextIndexed
    @NotBlank(message = "내용은 필수입니다")
    private String content;  // 마크다운 형식

    // HTML 태그 제거한 요약 자동 생성
    private String summary;
}
```

### 요약 생성 (HTML 태그 제거)

```java
/**
 * 내용에서 요약 자동 생성
 * HTML 태그 제거 후 200자 추출
 */
private String generateSummary(String content) {
    if (content == null || content.isEmpty()) return "";

    // HTML 태그 제거 (마크다운이 HTML 포함할 수 있음)
    String clean = content.replaceAll("<[^>]*>", "");

    // 마크다운 문법 제거 (선택적)
    clean = clean.replaceAll("#+\\s*", "");        // 헤더
    clean = clean.replaceAll("\\*\\*|__", "");     // 볼드
    clean = clean.replaceAll("\\*|_", "");         // 이탤릭
    clean = clean.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1"); // 링크
    clean = clean.replaceAll("```[\\s\\S]*?```", ""); // 코드 블록
    clean = clean.replaceAll("`[^`]+`", "");       // 인라인 코드

    return clean.length() > 200 ? clean.substring(0, 200) + "..." : clean;
}
```

## 프론트엔드: 마크다운 렌더링

### Vue 3 (Blog Frontend)

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

const props = defineProps<{
  content: string
}>()

// marked 설정
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// XSS 방지를 위한 sanitize
const renderedContent = computed(() => {
  const html = marked.parse(props.content)
  return DOMPurify.sanitize(html)
})
</script>

<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<style>
@import 'highlight.js/styles/github.css';
/* 마크다운 스타일 */
.markdown-body h1 { font-size: 2em; margin-bottom: 0.5em; }
.markdown-body h2 { font-size: 1.5em; margin-bottom: 0.5em; }
.markdown-body p { margin-bottom: 1em; }
.markdown-body pre { background: #f6f8fa; padding: 1em; overflow-x: auto; }
.markdown-body code { background: #f6f8fa; padding: 0.2em 0.4em; border-radius: 3px; }
</style>
```

### React (Shopping Frontend 예시)

```tsx
import { useMemo } from 'react'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

interface MarkdownViewerProps {
  content: string
}

// marked 설정
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

export const MarkdownViewer: React.FC<MarkdownViewerProps> = ({ content }) => {
  const renderedContent = useMemo(() => {
    const html = marked.parse(content)
    return DOMPurify.sanitize(html)
  }, [content])

  return (
    <div
      className="markdown-body"
      dangerouslySetInnerHTML={{ __html: renderedContent }}
    />
  )
}
```

## 마크다운 에디터

### Toast UI Editor (Vue)

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Editor from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'

const editorRef = ref<HTMLElement | null>(null)
let editor: Editor | null = null

const emit = defineEmits<{
  (e: 'update:content', value: string): void
}>()

onMounted(() => {
  if (editorRef.value) {
    editor = new Editor({
      el: editorRef.value,
      height: '500px',
      initialEditType: 'markdown',
      previewStyle: 'vertical',
      hooks: {
        addImageBlobHook: async (blob, callback) => {
          // 이미지 업로드 처리
          const url = await uploadImage(blob)
          callback(url, 'alt text')
        }
      }
    })

    editor.on('change', () => {
      emit('update:content', editor?.getMarkdown() || '')
    })
  }
})

async function uploadImage(blob: Blob): Promise<string> {
  const formData = new FormData()
  formData.append('file', blob)

  const response = await fetch('/api/v1/files/upload', {
    method: 'POST',
    body: formData
  })
  const data = await response.json()
  return data.data.url
}
</script>

<template>
  <div ref="editorRef"></div>
</template>
```

## 이미지 처리

### 에디터에서 이미지 업로드

```javascript
// Toast UI Editor 이미지 후크
hooks: {
  addImageBlobHook: async (blob, callback) => {
    try {
      const formData = new FormData()
      formData.append('file', blob)

      const response = await apiClient.post('/api/v1/files/upload', formData)
      const imageUrl = response.data.data.url

      // 마크다운 형식으로 삽입
      callback(imageUrl, 'image')
    } catch (error) {
      console.error('Image upload failed:', error)
    }
  }
}
```

### 백엔드 이미지 URL 관리

```java
// Post.java
private List<String> images = new ArrayList<>();

public void addImage(String imageUrl) {
    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
        this.images.add(imageUrl);
    }
}

// 썸네일 자동 설정
public void setDefaultThumbnailIfNeeded() {
    if ((this.thumbnailUrl == null || this.thumbnailUrl.isEmpty())
            && !this.images.isEmpty()) {
        this.thumbnailUrl = this.images.get(0);
    }
}
```

## 보안 고려사항

### XSS 방지

```javascript
import DOMPurify from 'dompurify'

// 허용할 태그와 속성 설정
const config = {
  ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
                 'ul', 'ol', 'li', 'a', 'img', 'pre', 'code', 'blockquote'],
  ALLOWED_ATTR: ['href', 'src', 'alt', 'class']
}

const sanitizedHtml = DOMPurify.sanitize(html, config)
```

### 링크 처리

```javascript
// 외부 링크에 rel="noopener" 추가
const renderer = new marked.Renderer()
renderer.link = (href, title, text) => {
  const isExternal = href.startsWith('http')
  const attrs = isExternal ? 'target="_blank" rel="noopener noreferrer"' : ''
  return `<a href="${href}" ${attrs}>${text}</a>`
}
```

## 마크다운 기능 지원

| 기능 | 문법 | 지원 |
|------|------|------|
| 헤더 | `# H1` ~ `###### H6` | O |
| 볼드 | `**text**` | O |
| 이탤릭 | `*text*` | O |
| 링크 | `[text](url)` | O |
| 이미지 | `![alt](url)` | O |
| 코드 블록 | ` ``` ` | O (Syntax Highlighting) |
| 인라인 코드 | `` `code` `` | O |
| 리스트 | `- item` or `1. item` | O |
| 인용 | `> quote` | O |
| 테이블 | GFM 테이블 | O |
| 체크박스 | `- [ ] task` | O |

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 저장 | 마크다운 원본 저장 |
| 렌더링 | 프론트엔드에서 HTML 변환 |
| 보안 | DOMPurify로 XSS 방지 |
| 코드 | highlight.js로 Syntax Highlighting |
| 이미지 | S3 업로드 후 URL 삽입 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/file/service/FileService.java`
- `/frontend/blog-frontend/` (마크다운 에디터/뷰어 컴포넌트)
