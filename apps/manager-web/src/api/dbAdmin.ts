import { dbAdminApi } from './client'

export const dbAdminLogin = (username: string, password: string) =>
  dbAdminApi.post<{ token: string }>('/db-admin/auth/login', { username, password }).then(r => r.data)

export const getStats = () =>
  dbAdminApi.get('/api/db-admin/stats').then(r => r.data)

export const listManagers = () =>
  dbAdminApi.get('/api/db-admin/managers').then(r => r.data)

export const deleteManager = (id: string) =>
  dbAdminApi.delete(`/api/db-admin/managers/${id}`)

export const listAllBuildings = () =>
  dbAdminApi.get('/api/db-admin/buildings').then(r => r.data)

export const deleteAnyBuilding = (id: string) =>
  dbAdminApi.delete(`/api/db-admin/buildings/${id}`)

export const listNavPackages = () =>
  dbAdminApi.get('/api/db-admin/nav-packages').then(r => r.data)

export const getNavPackageJson = (id: string) =>
  dbAdminApi.get(`/api/db-admin/nav-packages/${id}/json`).then(r => r.data)

export const deleteNavPackage = (id: string) =>
  dbAdminApi.delete(`/api/db-admin/nav-packages/${id}`)

export const regenerateNavPackage = (buildingId: string) =>
  dbAdminApi.post(`/api/db-admin/buildings/${buildingId}/regenerate`).then(r => r.data)
