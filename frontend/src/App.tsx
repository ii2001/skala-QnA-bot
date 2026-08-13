import { useEffect, useMemo, useRef, useState } from 'react'
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
type ProfessorQuestion = Question & {
  authorId: number
  campusId: number
  campusName: string
  classroomId: number
  classroomName: string
  source: string
}
type ProfessorDashboardResponse = { questions: ProfessorQuestion[]; unansweredCount: number }
type DashboardFilters = { status: string; campusId: string; classroomId: string; category: string }

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

function statusLabel(status: string) {
  return status === 'OPEN' ? '미답변' : status
}

function ProfessorDashboard({ token, user, onLogout }: { token: string; user: User; onLogout: () => void }) {
  const [dashboard, setDashboard] = useState<ProfessorDashboardResponse | null>(null)
  const [allQuestions, setAllQuestions] = useState<ProfessorQuestion[]>([])
  const [filters, setFilters] = useState<DashboardFilters>({ status: '', campusId: '', classroomId: '', category: '' })
  const [selected, setSelected] = useState<ProfessorQuestion | null>(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const hasFilters = Object.values(filters).some(Boolean)

  const campuses = useMemo(() => Array.from(new Map(allQuestions.map((question) => [
    question.campusId,
    { id: question.campusId, name: question.campusName },
  ])).values()), [allQuestions])
  const classrooms = useMemo(() => Array.from(new Map(allQuestions
    .filter((question) => !filters.campusId || String(question.campusId) === filters.campusId)
    .map((question) => [question.classroomId, { id: question.classroomId, name: question.classroomName }])).values()),
  [allQuestions, filters.campusId])
  const categories = useMemo(() => Array.from(new Set(allQuestions.map((question) => question.category))).sort(), [allQuestions])

  useEffect(() => {
    let active = true
    const params = new URLSearchParams()
    if (filters.status) params.set('status', filters.status)
    if (filters.campusId) params.set('campusId', filters.campusId)
    if (filters.classroomId) params.set('classroomId', filters.classroomId)
    if (filters.category) params.set('category', filters.category)
    setLoading(true)
    setError('')
    request<ProfessorDashboardResponse>(`/api/professor/questions${params.size ? `?${params}` : ''}`, token)
      .then((result) => {
        if (!active) return
        setDashboard(result)
        if (!hasFilters) setAllQuestions(result.questions)
      })
      .catch((reason: Error) => { if (active) setError(reason.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [token, filters.status, filters.campusId, filters.classroomId, filters.category, hasFilters])

  async function openQuestion(question: ProfessorQuestion) {
    setSelected(question)
    setDetailLoading(true)
    setError('')
    try {
      setSelected(await request<ProfessorQuestion>(`/api/professor/questions/${question.id}`, token))
    } catch (reason) {
      setError((reason as Error).message)
    } finally {
      setDetailLoading(false)
    }
  }

  function updateFilter(name: keyof DashboardFilters, value: string) {
    setFilters((current) => ({ ...current, [name]: value, ...(name === 'campusId' ? { classroomId: '' } : {}) }))
  }

  return (
    <main className="app-shell">
      <header>
        <div><p className="eyebrow">SKALA Q&amp;A</p><h1>{user.name}님의 대시보드</h1></div>
        <button className="secondary" onClick={onLogout}>로그아웃</button>
      </header>

      {error && <p className="error" role="alert">{error}</p>}

      <section className="dashboard-summary" aria-label="질문 요약">
        <div><span>미답변 질문</span><strong>{dashboard?.unansweredCount ?? 0}</strong></div>
        <div><span>현재 질문</span><strong>{dashboard?.questions.length ?? 0}</strong></div>
      </section>

      <section>
        <h2>질문 필터</h2>
        <div className="filters">
          <label>상태<select value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
            <option value="">전체 상태</option><option value="OPEN">미답변</option>
          </select></label>
          <label>캠퍼스<select value={filters.campusId} onChange={(event) => updateFilter('campusId', event.target.value)}>
            <option value="">전체 캠퍼스</option>{campuses.map((campus) => <option key={campus.id} value={campus.id}>{campus.name}</option>)}
          </select></label>
          <label>클래스<select value={filters.classroomId} onChange={(event) => updateFilter('classroomId', event.target.value)}>
            <option value="">전체 클래스</option>{classrooms.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}
          </select></label>
          <label>카테고리<select value={filters.category} onChange={(event) => updateFilter('category', event.target.value)}>
            <option value="">전체 카테고리</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}
          </select></label>
        </div>
      </section>

      <section>
        <h2>담당 질문</h2>
        {loading ? <p className="empty" aria-live="polite">질문을 불러오는 중입니다.</p> : dashboard?.questions.length === 0 ? (
          <p className="empty">{hasFilters ? '조건에 맞는 질문이 없습니다.' : '담당 질문이 없습니다.'}</p>
        ) : (
          <ul className="question-list">
            {dashboard?.questions.map((question) => (
              <li key={question.id}>
                <button type="button" onClick={() => openQuestion(question)}>
                  <span>{question.campusName} · {question.classroomName}</span>
                  <strong>{question.title}</strong>
                  <time>{question.category} · {statusLabel(question.status)} · {new Date(question.createdAt).toLocaleString('ko-KR')}</time>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {selected && (
        <dialog open aria-labelledby="professor-question-title" aria-busy={detailLoading}>
          <button type="button" className="close" aria-label="닫기" onClick={() => setSelected(null)}>×</button>
          <p className="eyebrow">{selected.campusName} · {selected.classroomName}</p>
          <h2 id="professor-question-title">{selected.title}</h2>
          <p className="question-meta">카테고리: {selected.category} · 상태: {statusLabel(selected.status)}</p>
          {detailLoading ? <p>상세 내용을 불러오는 중입니다.</p> : <p className="question-content">{selected.content}</p>}
        </dialog>
      )}
    </main>
  )
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
  const [submitting, setSubmitting] = useState(false)
  const sessionRef = useRef(0)

  useEffect(() => {
    if (!token || !user || user.role !== 'STUDENT') return
    let active = true
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
        if (!active) return
        setEnrollment(nextEnrollment)
        setCampus(campuses.find(({ id }) => id === nextEnrollment.campusId) ?? null)
        setClassroom(classrooms.find(({ id }) => id === nextEnrollment.classroomId) ?? null)
        setQuestions(nextQuestions)
      })
      .catch((reason: Error) => { if (active) setError(reason.message) })
    return () => { active = false }
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
      if (result.user.role !== 'STUDENT' && result.user.role !== 'PROFESSOR') throw new Error('학생 또는 교수 계정으로 로그인해 주세요.')
      setToken(result.accessToken)
      setUser(result.user)
    } catch (reason) {
      setError((reason as Error).message)
    }
  }

  async function createQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!enrollment || submitting) return
    const session = sessionRef.current
    setError('')
    setSubmitting(true)
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
      if (session !== sessionRef.current) return
      setQuestions((current) => [question, ...current])
      setSelected(question)
      formElement.reset()
    } catch (reason) {
      if (session === sessionRef.current) setError((reason as Error).message)
    } finally {
      if (session === sessionRef.current) setSubmitting(false)
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

  function logout() {
    sessionRef.current += 1
    setToken('')
    setUser(null)
    setEnrollment(null)
    setCampus(null)
    setClassroom(null)
    setQuestions([])
    setSelected(null)
    setError('')
    setSubmitting(false)
  }

  if (!user) {
    return (
      <main className="login-card">
        <p className="eyebrow">SKALA Q&amp;A</p>
        <h1>로그인</h1>
        <form onSubmit={login}>
          <label>이메일<input name="email" type="email" autoComplete="email" required /></label>
          <label>비밀번호<input name="password" type="password" autoComplete="current-password" required /></label>
          <button type="submit">로그인</button>
        </form>
        {error && <p className="error" role="alert">{error}</p>}
      </main>
    )
  }

  if (user.role === 'PROFESSOR') return <ProfessorDashboard token={token} user={user} onLogout={logout} />

  return (
    <main className="app-shell">
      <header>
        <div><p className="eyebrow">SKALA Q&amp;A</p><h1>{user.name}님의 질문</h1></div>
        <button className="secondary" onClick={logout}>로그아웃</button>
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
            <button type="submit" disabled={!enrollment || submitting}>{submitting ? '등록 중...' : '질문 등록'}</button>
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
