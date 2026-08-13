import { useEffect, useState } from 'react'
import './App.css'

type Health = '확인 중' | '정상' | '연결 실패'

function App() {
  const [health, setHealth] = useState<Health>('확인 중')

  useEffect(() => {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

    fetch(`${apiBaseUrl}/actuator/health`)
      .then((response) => {
        if (!response.ok) throw new Error('Backend health check failed')
        return response.json() as Promise<{ status: string }>
      })
      .then(({ status }) => setHealth(status === 'UP' ? '정상' : '연결 실패'))
      .catch(() => setHealth('연결 실패'))
  }, [])

  return (
    <main>
      <p className="eyebrow">SKALA Q&amp;A</p>
      <h1>질문과 답변을 한곳에서</h1>
      <p className="description">
        학생과 교수자가 질문을 공유하고 답변을 관리하는 공간입니다.
      </p>
      <section className="health" aria-live="polite">
        <span>백엔드 연결 상태</span>
        <strong data-health={health}>{health}</strong>
      </section>
    </main>
  )
}

export default App
