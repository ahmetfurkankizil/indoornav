import axios from 'axios'

export const api = axios.create({ baseURL: '/' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('manager_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const dbAdminApi = axios.create({ baseURL: '/' })

dbAdminApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('dbadmin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
