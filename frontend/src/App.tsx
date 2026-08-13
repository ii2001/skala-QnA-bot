import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const API_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type User = { id: number; name: string; role: string }
type LoginResponse = { accessToken: string; user: User }
type Enrollment = { campusId: number; classroomId: number }
type Campus = { id: number; name: string }
type Classroom = { id: number; name: string }
type Question = {
  id: number
  category: string
  title: string
  content: string
  status: string
  createdAt: string
}

async function request<T>(path: string, token?: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { detail?: string } | null
    throw new Error(problem?.detail ?? '요청을 처리하지 못했습니다.')
  }
  return response.json() as Promise<T>
}

function App() {
  const [token, setToken] = useState('')
  const [user, setUser] = useState<User | null>(null)
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null)
  const [campus, setCampus] = useState<Campus | null>(null)
  const [classroom, setClassroom] = useState<Classroom | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [selected, setSelected] = useState<Question | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token || !user) return
    Promise.all([
      request<Enrollment>(`/api/students/${user.id}/enrollment`, token),
      request<Campus[]>('/api/campuses', token),
      request<Question[]>('/api/questions', token),
    ])
      .then(async ([nextEnrollment, campuses, nextQuestions]) => {
        const classrooms = await request<Classroom[]>(
          `/api/campuses/${nextEnrollment.campusId}/classrooms`,
          token,
        )
        setEnrollment(nextEnrollment)
        setCampus(campuses.find(({ id }) => id === nextEnrollment.campusId) ?? null)
        setClassroom(classrooms.find(({ id }) => id === nextEnrollment.classroomId) ?? null)
        setQuestions(nextQuestions)
      })
      .catch((reason: Error) => setError(reason.message))
  }, [token, user])

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    const form = new FormData(event.currentTarget)
    try {
      const result = await request<LoginResponse>('/api/auth/login', undefined, {
        method: 'POST',
        body: JSON.stringify({ email: form.get('email'), password: form.get('password') }),
      })
      if (result.user.role !== 'STUDENT') throw new Error('학생 계정으로 로그인해 주세요.')
      setToken(result.accessToken)
      setUser(result.user)
    } catch (reason) {
      setError((reason as Error).message)
    }
  }

  async function createQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!enrollment) return
    setError('')
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    try {
      const question = await request<Question>('/api/questions', token, {
        method: 'POST',
        body: JSON.stringify({
          campusId: enrollment.campusId,
          classroomId: enrollment.classroomId,
          category: form.get('category'),
          title: form.get('title'),
          content: form.get('content'),
        }),
      })
      setQuestions((current) => [question, ...current])
      setSelected(question)
      formElement.reset()
    } catch (reason) {
      setError((reason as Error).message)
    }
  }

  async function openQuestion(id: number) {
    setError('')
    try {
      setSelected(await request<Question>(`/api/questions/${id}`, token))
    } catch (reason) {
      setError((reason as Error).message)
    }
  }

  if (!user) {
    return (
      <main className="login-card">
        <p className="eyebrow">SKALA Q&amp;A</p>
        <h1>학생 로그인</h1>
        <form onSubmit={login}>
          <label>이메일<input name="email" type="email" autoComplete="email" required /></label>
          <label>비밀번호<input name="password" type="password" autoComplete="current-password" required /></label>
          <button type="submit">로그인</button>
        </form>
        {error && <p className="error" role="alert">{error}</p>}
      </main>
    )
  }

  return (
    <main className="app-shell">
      <header>
        <div><p className="eyebrow">SKALA Q&amp;A</p><h1>{user.name}님의 질문</h1></div>
        <button className="secondary" onClick={() => { setToken(''); setUser(null) }}>로그아웃</button>
      </header>

      {error && <p className="error" role="alert">{error}</p>}

      <div className="columns">
        <section>
          <h2>새 질문</h2>
          <form onSubmit={createQuestion}>
            <label>캠퍼스<input value={campus?.name ?? '불러오는 중'} disabled /></label>
            <label>클래스<input value={classroom?.name ?? '불러오는 중'} disabled /></label>
            <label>카테고리<input name="category" maxLength={100} required /></label>
            <label>제목<input name="title" maxLength={200} required /></label>
            <label>내용<textarea name="content" rows={7} maxLength={10000} required /></label>
            <button type="submit" disabled={!enrollment}>질문 등록</button>
          </form>
        </section>

        <section>
          <h2>내 질문</h2>
          {questions.length === 0 ? <p className="empty">등록한 질문이 없습니다.</p> : (
            <ul className="question-list">
              {questions.map((question) => (
                <li key={question.id}>
                  <button onClick={() => openQuestion(question.id)}>
                    <span>{question.category} · {question.status}</span>
                    <strong>{question.title}</strong>
                    <time>{new Date(question.createdAt).toLocaleString('ko-KR')}</time>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {selected && (
        <dialog open aria-labelledby="question-title">
          <button className="close" aria-label="닫기" onClick={() => setSelected(null)}>×</button>
          <p className="eyebrow">{selected.category} · {selected.status}</p>
          <h2 id="question-title">{selected.title}</h2>
          <p className="question-content">{selected.content}</p>
        </dialog>
      )}
    </main>
  )
}

export default App
