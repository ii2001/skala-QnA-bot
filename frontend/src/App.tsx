import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const API_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type User = { id: number; name: string; role: string; email?: string; active?: boolean }
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
type Answer = { id: number; questionId: number; professorId: number; content: string; visibility: string; createdAt: string }
type StaffAccess = { id: number; email: string; expectedRole: string; active: boolean; note?: string; updatedAt: string }
type ChannelMapping = { id: number; scopeType: string; scopeId: number | null; slackChannelId: string }
type UserMapping = { id: number; userId: number; slackUserId: string }
type Assignment = { id: number; professorId: number; campusId: number; classroomId: number }

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
    throw Object.assign(new Error(problem?.detail ?? '요청을 처리하지 못했습니다.'), { status: response.status })
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

function StudentOnboarding({ token, userId, campuses, onComplete }: { token: string; userId: number; campuses: Campus[]; onComplete: () => void }) {
  const [campusId, setCampusId] = useState('')
  const [classrooms, setClassrooms] = useState<Classroom[]>([])
  const [classroomId, setClassroomId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setClassroomId('')
    if (!campusId) {
      setClassrooms([])
      return
    }
    request<Classroom[]>(`/api/campuses/${campusId}/classrooms`, token)
      .then(setClassrooms)
      .catch((reason: Error) => setError(reason.message))
  }, [campusId, token])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await request(`/api/students/${userId}/enrollment`, token, {
        method: 'PUT',
        body: JSON.stringify({ campusId: Number(campusId), classroomId: Number(classroomId) }),
      })
      onComplete()
    } catch (reason) {
      setError((reason as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-card">
      <p className="eyebrow">SKALA Q&amp;A</p>
      <h1>학생 정보 설정</h1>
      <p className="empty">질문을 등록하려면 캠퍼스와 클래스를 먼저 선택해 주세요.</p>
      <form onSubmit={submit}>
        <label>캠퍼스<select value={campusId} onChange={(event) => setCampusId(event.target.value)} required>
          <option value="">캠퍼스 선택</option>{campuses.map((campus) => <option key={campus.id} value={campus.id}>{campus.name}</option>)}
        </select></label>
        <label>클래스<select value={classroomId} onChange={(event) => setClassroomId(event.target.value)} required disabled={!campusId}>
          <option value="">클래스 선택</option>{classrooms.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}
        </select></label>
        <button type="submit" disabled={loading || !classroomId}>{loading ? '저장 중...' : '저장하고 시작하기'}</button>
      </form>
      {error && <p className="error" role="alert">{error}</p>}
    </main>
  )
}

function statusLabel(status: string) {
  return status === 'OPEN' ? '미답변' : status === 'ANSWERED' ? '답변 완료' : status
}

function ProfessorDashboard({ token, user, onLogout }: { token: string; user: User; onLogout: () => void }) {
  const [dashboard, setDashboard] = useState<ProfessorDashboardResponse | null>(null)
  const [allQuestions, setAllQuestions] = useState<ProfessorQuestion[]>([])
  const [filters, setFilters] = useState<DashboardFilters>({ status: '', campusId: '', classroomId: '', category: '' })
  const [selected, setSelected] = useState<ProfessorQuestion | null>(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [answerSubmitting, setAnswerSubmitting] = useState(false)
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

  async function submitAnswer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selected || selected.status !== 'OPEN' || answerSubmitting) return
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    setAnswerSubmitting(true)
    setError('')
    try {
      await request<Answer>(`/api/professor/questions/${selected.id}/answers`, token, {
        method: 'POST',
        body: JSON.stringify({ content: form.get('content'), visibility: form.get('visibility') }),
      })
      setSelected((current) => current ? { ...current, status: 'ANSWERED' } : current)
      setDashboard((current) => {
        if (!current) return current
        const updated = current.questions.map((question) => question.id === selected.id
          ? { ...question, status: 'ANSWERED' }
          : question)
        return {
          ...current,
          questions: filters.status === 'OPEN' ? updated.filter((question) => question.status !== 'ANSWERED') : updated,
          unansweredCount: Math.max(0, current.unansweredCount - 1),
        }
      })
      setAllQuestions((current) => current.map((question) => question.id === selected.id
        ? { ...question, status: 'ANSWERED' }
        : question))
      formElement.reset()
    } catch (reason) {
      setError((reason as Error).message)
    } finally {
      setAnswerSubmitting(false)
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
          {detailLoading ? <p>상세 내용을 불러오는 중입니다.</p> : <>
            <p className="question-content">{selected.content}</p>
            {selected.status === 'OPEN' && <form className="answer-form" onSubmit={submitAnswer}>
              <h3>답변 등록</h3>
              <label>공개 범위<select name="visibility" defaultValue="PRIVATE">
                <option value="PRIVATE">개인 (질문자만)</option>
                <option value="CLASS">클래스 전체</option>
                <option value="CAMPUS">캠퍼스 전체</option>
                <option value="GLOBAL">전체 캠퍼스</option>
              </select></label>
              <label>답변 내용<textarea name="content" rows={6} maxLength={10000} required /></label>
              <button type="submit" disabled={answerSubmitting}>{answerSubmitting ? '등록 중...' : '답변 등록'}</button>
            </form>}
          </>}
        </dialog>
      )}
    </main>
  )
}

function AdminConsole({ token, user, onLogout }: { token: string; user: User; onLogout: () => void }) {
  const [campuses, setCampuses] = useState<Campus[]>([])
  const [classrooms, setClassrooms] = useState<Record<number, Classroom[]>>({})
  const [users, setUsers] = useState<User[]>([])
  const [staff, setStaff] = useState<StaffAccess[]>([])
  const [channels, setChannels] = useState<ChannelMapping[]>([])
  const [userMappings, setUserMappings] = useState<UserMapping[]>([])
  const [assignments, setAssignments] = useState<Assignment[]>([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [channelScopeType, setChannelScopeType] = useState('CLASS')
  const [userQuery, setUserQuery] = useState('')
  const allClassrooms = campuses.flatMap((campus) => classrooms[campus.id] ?? [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const nextCampuses = await request<Campus[]>('/api/campuses', token)
      const [nextUsers, nextStaff, nextChannels, nextMappings] = await Promise.all([
        request<User[]>('/api/users', token),
        request<StaffAccess[]>('/api/admin/staff-access', token),
        request<ChannelMapping[]>('/api/slack/channel-mappings', token),
        request<UserMapping[]>('/api/slack/user-mappings', token),
      ])
      const classroomEntries = await Promise.all(nextCampuses.map(async (campus) => [
        campus.id,
        await request<Classroom[]>(`/api/campuses/${campus.id}/classrooms`, token),
      ] as const))
      const professorEntries = await Promise.all(nextUsers.filter(({ role }) => role === 'PROFESSOR').map(async (professor) =>
        request<Assignment[]>(`/api/professors/${professor.id}/assignments`, token)))
      setCampuses(nextCampuses)
      setClassrooms(Object.fromEntries(classroomEntries))
      setUsers(nextUsers)
      setStaff(nextStaff)
      setChannels(nextChannels)
      setUserMappings(nextMappings)
      setAssignments(professorEntries.flat())
    } catch (reason) {
      setMessage((reason as Error).message)
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => { void load() }, [load])

  async function mutate(action: Promise<unknown>) {
    try {
      await action
      await load()
      setMessage('저장했습니다.')
    } catch (reason) {
      setMessage((reason as Error).message)
    }
  }

  async function createCampus(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    await mutate(request('/api/campuses', token, { method: 'POST', body: JSON.stringify({ name: form.get('name') }) }))
    event.currentTarget.reset()
  }

  async function updateCampus(campus: Campus) {
    const name = window.prompt('캠퍼스 이름', campus.name)
    if (name === null) return
    await mutate(request(`/api/campuses/${campus.id}`, token, { method: 'PUT', body: JSON.stringify({ name }) }))
  }

  async function deleteCampus(campus: Campus) {
    if (window.confirm(`'${campus.name}' 캠퍼스를 삭제할까요?`)) await mutate(request(`/api/campuses/${campus.id}`, token, { method: 'DELETE' }))
  }

  async function createClassroom(event: FormEvent<HTMLFormElement>, campusId: number) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    await mutate(request(`/api/campuses/${campusId}/classrooms`, token, { method: 'POST', body: JSON.stringify({ name: form.get('name') }) }))
    event.currentTarget.reset()
  }

  async function updateClassroom(classroom: Classroom) {
    const name = window.prompt('클래스 이름', classroom.name)
    if (name === null) return
    await mutate(request(`/api/classrooms/${classroom.id}`, token, { method: 'PUT', body: JSON.stringify({ name }) }))
  }

  async function deleteClassroom(classroom: Classroom) {
    if (window.confirm(`'${classroom.name}' 클래스를 삭제할까요?`)) await mutate(request(`/api/classrooms/${classroom.id}`, token, { method: 'DELETE' }))
  }

  async function updateUser(userToUpdate: User, role: string, active: boolean) {
    await mutate(request(`/api/users/${userToUpdate.id}`, token, {
      method: 'PUT',
      body: JSON.stringify({ name: userToUpdate.name, email: userToUpdate.email, role, active }),
    }))
  }

  async function createStaff(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    await mutate(request('/api/admin/staff-access', token, {
      method: 'POST',
      body: JSON.stringify({ email: form.get('email'), expectedRole: form.get('expectedRole'), note: form.get('note') }),
    }))
    event.currentTarget.reset()
  }

  async function editStaff(access: StaffAccess) {
    const expectedRole = window.prompt('역할 (PROFESSOR 또는 ADMIN)', access.expectedRole)
    if (expectedRole === null) return
    const note = window.prompt('메모', access.note ?? '')
    if (note === null) return
    await mutate(request(`/api/admin/staff-access/${access.id}`, token, {
      method: 'PUT',
      body: JSON.stringify({ expectedRole, note, active: access.active }),
    }))
  }

  async function deactivateStaff(access: StaffAccess) {
    if (window.confirm(`${access.email}의 Staff 권한을 비활성화할까요?`)) await mutate(request(`/api/admin/staff-access/${access.id}`, token, {
      method: 'PUT',
      body: JSON.stringify({ expectedRole: access.expectedRole, note: access.note, active: false }),
    }))
  }

  async function createAssignment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    await mutate(request(`/api/professors/${form.get('professorId')}/assignments`, token, {
      method: 'POST', body: JSON.stringify({ classroomId: Number(form.get('classroomId')) }),
    }))
    event.currentTarget.reset()
  }

  async function createChannelMapping(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const scopeType = String(form.get('scopeType'))
    await mutate(request('/api/slack/channel-mappings', token, {
      method: 'POST', body: JSON.stringify({ scopeType, scopeId: scopeType === 'GLOBAL' ? null : Number(form.get('scopeId')), slackChannelId: form.get('slackChannelId') }),
    }))
    event.currentTarget.reset()
  }

  async function editChannel(channel: ChannelMapping) {
    const slackChannelId = window.prompt('Slack 채널 ID', channel.slackChannelId)
    if (slackChannelId === null) return
    await mutate(request(`/api/slack/channel-mappings/${channel.id}`, token, {
      method: 'PUT', body: JSON.stringify(channel.scopeType === 'GLOBAL'
        ? { scopeType: channel.scopeType, scopeId: null, slackChannelId }
        : { scopeType: channel.scopeType, scopeId: channel.scopeId, slackChannelId }),
    }))
  }

  async function createUserMapping(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    await mutate(request('/api/slack/user-mappings', token, {
      method: 'POST', body: JSON.stringify({ userId: Number(form.get('userId')), slackUserId: form.get('slackUserId') }),
    }))
    event.currentTarget.reset()
  }

  if (loading && users.length === 0) return <main className="app-shell"><p className="empty">관리 데이터를 불러오는 중입니다.</p></main>

  return (
    <main className="app-shell">
      <header><div><p className="eyebrow">SKALA Q&amp;A ADMIN</p><h1>{user.name}님의 운영 콘솔</h1></div><button className="secondary" onClick={onLogout}>로그아웃</button></header>
      {message && <p className="error" role="alert">{message}</p>}
      <nav className="admin-nav" aria-label="관리 메뉴"><a href="#organization">조직</a><a href="#staff">Staff</a><a href="#users">사용자</a><a href="#assignments">교수 배정</a><a href="#slack">Slack</a></nav>

      <section id="organization"><h2>캠퍼스·클래스</h2><form className="inline-form" onSubmit={createCampus}><input name="name" placeholder="새 캠퍼스 이름" required /><button>캠퍼스 추가</button></form>
        <div className="admin-grid">{campuses.map((campus) => <article key={campus.id} className="admin-card"><h3>{campus.name}</h3><div className="button-row"><button type="button" className="secondary" onClick={() => updateCampus(campus)}>수정</button><button type="button" className="danger" onClick={() => deleteCampus(campus)}>삭제</button></div><ul>{(classrooms[campus.id] ?? []).map((classroom) => <li key={classroom.id}>{classroom.name} <button type="button" className="link-button" onClick={() => updateClassroom(classroom)}>수정</button><button type="button" className="link-button danger-text" onClick={() => deleteClassroom(classroom)}>삭제</button></li>)}</ul><form className="inline-form" onSubmit={(event) => createClassroom(event, campus.id)}><input name="name" placeholder="새 클래스 이름" required /><button>추가</button></form></article>)}</div>
      </section>

      <section id="staff"><h2>Staff 허용 목록</h2><form className="inline-form" onSubmit={createStaff}><input name="email" type="email" placeholder="이메일" required /><select name="expectedRole" defaultValue="PROFESSOR"><option value="PROFESSOR">교수</option><option value="ADMIN">ADMIN</option></select><input name="note" placeholder="메모" /><button>등록</button></form><ul className="admin-list">{staff.map((access) => <li key={access.id}><strong>{access.email}</strong> · {access.expectedRole} · {access.active ? '활성' : '비활성'} <button type="button" className="link-button" onClick={() => editStaff(access)}>수정</button>{access.active && <button type="button" className="link-button danger-text" onClick={() => deactivateStaff(access)}>비활성화</button>}</li>)}</ul></section>

      <section id="users"><h2>사용자</h2><input value={userQuery} onChange={(event) => setUserQuery(event.target.value)} placeholder="이름 또는 이메일 검색" /><ul className="admin-list">{users.filter((listedUser) => `${listedUser.name} ${listedUser.email ?? ''}`.toLowerCase().includes(userQuery.toLowerCase())).map((listedUser) => <li key={listedUser.id}><strong>{listedUser.name}</strong> · {listedUser.email} · <select value={listedUser.role} onChange={(event) => updateUser(listedUser, event.target.value, listedUser.active !== false)}><option value="STUDENT">STUDENT</option><option value="PROFESSOR">PROFESSOR</option><option value="ADMIN">ADMIN</option></select> · {listedUser.active === false ? '비활성' : '활성'} <button type="button" className="link-button" onClick={() => updateUser(listedUser, listedUser.role, listedUser.active === false)}>상태 전환</button></li>)}</ul></section>

      <section id="assignments"><h2>교수 담당 클래스</h2><form className="inline-form" onSubmit={createAssignment}><select name="professorId" required><option value="">교수 선택</option>{users.filter(({ role }) => role === 'PROFESSOR').map((professor) => <option key={professor.id} value={professor.id}>{professor.name}</option>)}</select><select name="classroomId" required><option value="">클래스 선택</option>{allClassrooms.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}</select><button>배정</button></form><ul className="admin-list">{assignments.map((assignment) => <li key={assignment.id}>{users.find(({ id }) => id === assignment.professorId)?.name ?? assignment.professorId} → {allClassrooms.find(({ id }) => id === assignment.classroomId)?.name ?? assignment.classroomId} <button type="button" className="link-button danger-text" onClick={() => mutate(request(`/api/professor-assignments/${assignment.id}`, token, { method: 'DELETE' }))}>해제</button></li>)}</ul></section>

      <section id="slack"><h2>Slack 매핑</h2><form className="inline-form" onSubmit={createUserMapping}><select name="userId" required><option value="">사용자 선택</option>{users.map((listedUser) => <option key={listedUser.id} value={listedUser.id}>{listedUser.name} · {listedUser.email}</option>)}</select><input name="slackUserId" placeholder="Slack 사용자 ID" required /><button>사용자 매핑</button></form><ul className="admin-list">{userMappings.map((mapping) => <li key={mapping.id}>{users.find(({ id }) => id === mapping.userId)?.email ?? mapping.userId} → {mapping.slackUserId} <button type="button" className="link-button danger-text" onClick={() => mutate(request(`/api/slack/user-mappings/${mapping.id}`, token, { method: 'DELETE' }))}>삭제</button></li>)}</ul><form className="inline-form" onSubmit={createChannelMapping}><select name="scopeType" value={channelScopeType} onChange={(event) => setChannelScopeType(event.target.value)}><option value="CLASS">클래스</option><option value="CAMPUS">캠퍼스</option><option value="GLOBAL">전체</option></select>{channelScopeType !== 'GLOBAL' && <select name="scopeId" required><option value="">범위 선택</option>{channelScopeType === 'CAMPUS' ? campuses.map((campus) => <option key={campus.id} value={campus.id}>{campus.name}</option>) : allClassrooms.map((classroom) => <option key={classroom.id} value={classroom.id}>{classroom.name}</option>)}</select>}<input name="slackChannelId" placeholder="Slack 채널 ID" required /><button>채널 매핑</button></form><ul className="admin-list">{channels.map((channel) => <li key={channel.id}>{channel.scopeType}:{channel.scopeId ?? 'all'} → {channel.slackChannelId} <button type="button" className="link-button" onClick={() => editChannel(channel)}>수정</button><button type="button" className="link-button danger-text" onClick={() => mutate(request(`/api/slack/channel-mappings/${channel.id}`, token, { method: 'DELETE' }))}>삭제</button></li>)}</ul></section>
    </main>
  )
}

function App() {
  const [token, setToken] = useState('')
  const [user, setUser] = useState<User | null>(null)
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null)
  const [onboarding, setOnboarding] = useState(false)
  const [campuses, setCampuses] = useState<Campus[]>([])
  const [campus, setCampus] = useState<Campus | null>(null)
  const [classroom, setClassroom] = useState<Classroom | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [selected, setSelected] = useState<Question | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const sessionRef = useRef(0)

  useEffect(() => {
    const callback = new URLSearchParams(window.location.hash.slice(1))
    const callbackToken = callback.get('access_token')
    const callbackError = callback.get('auth_error')
    if (!callbackToken && !callbackError) return
    window.history.replaceState(null, '', window.location.pathname + window.location.search)
    if (callbackError) {
      setError(callbackError)
      return
    }
    request<User>('/api/auth/me', callbackToken ?? '')
      .then((nextUser) => { setToken(callbackToken ?? ''); setUser(nextUser) })
      .catch((reason: Error) => setError(reason.message))
  }, [])

  useEffect(() => {
    if (!token || !user || user.role !== 'STUDENT') return
    let active = true
    Promise.all([
      request<Campus[]>('/api/campuses', token),
      request<Enrollment>(`/api/students/${user.id}/enrollment`, token).catch((reason: Error & { status?: number }) => {
        if (reason.status === 404) return null
        throw reason
      }),
    ])
      .then(async ([nextCampuses, nextEnrollment]) => {
        setCampuses(nextCampuses)
        if (!nextEnrollment) {
          if (active) setOnboarding(true)
          return
        }
        const nextQuestions = await request<Question[]>('/api/questions', token)
        const classrooms = await request<Classroom[]>(
          `/api/campuses/${nextEnrollment.campusId}/classrooms`,
          token,
        )
        if (!active) return
        setOnboarding(false)
        setEnrollment(nextEnrollment)
        setCampus(nextCampuses.find(({ id }) => id === nextEnrollment.campusId) ?? null)
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
      if (!['STUDENT', 'PROFESSOR', 'ADMIN'].includes(result.user.role)) throw new Error('허용되지 않은 계정입니다.')
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
    setOnboarding(false)
    setCampuses([])
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
        <a className="slack-login" href={`${API_URL}/oauth2/authorization/slack`}>Slack으로 로그인</a>
        <details>
          <summary>개발·관리자용 이메일 로그인</summary>
        <form onSubmit={login}>
          <label>이메일<input name="email" type="email" autoComplete="email" required /></label>
          <label>비밀번호<input name="password" type="password" autoComplete="current-password" required /></label>
          <button type="submit">로그인</button>
        </form>
        </details>
        {error && <p className="error" role="alert">{error}</p>}
      </main>
    )
  }

  if (user.role === 'PROFESSOR') return <ProfessorDashboard token={token} user={user} onLogout={logout} />
  if (user.role === 'ADMIN') return <AdminConsole token={token} user={user} onLogout={logout} />
  if (onboarding) return <StudentOnboarding token={token} userId={user.id} campuses={campuses} onComplete={() => {
    setOnboarding(false)
    setEnrollment(null)
    setUser((current) => current ? { ...current } : current)
  }} />

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
