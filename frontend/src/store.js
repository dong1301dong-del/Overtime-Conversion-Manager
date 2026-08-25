// 简单的鉴权状态（基于 localStorage，刷新不丢失）
export const auth = {
  get token() {
    return localStorage.getItem('token')
  },
  set token(v) {
    if (v) localStorage.setItem('token', v)
    else localStorage.removeItem('token')
  },
  get user() {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null')
    } catch (e) {
      return null
    }
  },
  set user(u) {
    if (u) localStorage.setItem('user', JSON.stringify(u))
    else localStorage.removeItem('user')
  },
  clear() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
}
