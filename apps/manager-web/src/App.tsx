import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage }           from './pages/auth/LoginPage'
import { SignupPage }          from './pages/auth/SignupPage'
import { DashboardPage }       from './pages/dashboard/DashboardPage'
import { BuildingDetailPage }  from './pages/building/BuildingDetailPage'
import { MapEditorPage }       from './pages/editor/MapEditorPage'
import { DbAdminLoginPage }    from './pages/db-admin/DbAdminLoginPage'
import { DbAdminDashboard }    from './pages/db-admin/DbAdminDashboard'
import { ManagersPage }        from './pages/db-admin/ManagersPage'
import { BuildingsAdminPage }  from './pages/db-admin/BuildingsAdminPage'
import { NavPackagesPage }     from './pages/db-admin/NavPackagesPage'
import { ProtectedRoute }      from './components/ProtectedRoute'
import { DbAdminRoute }        from './components/ProtectedRoute'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login"   element={<LoginPage />} />
        <Route path="/signup"  element={<SignupPage />} />

        {/* Manager (protected) */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard"                                         element={<DashboardPage />} />
          <Route path="/buildings/:id"                                     element={<BuildingDetailPage />} />
          <Route path="/buildings/:buildingId/floors/:floorId/edit"        element={<MapEditorPage />} />
        </Route>

        {/* DB Admin */}
        <Route path="/db-admin/login" element={<DbAdminLoginPage />} />
        <Route element={<DbAdminRoute />}>
          <Route path="/db-admin"              element={<DbAdminDashboard />} />
          <Route path="/db-admin/managers"     element={<ManagersPage />} />
          <Route path="/db-admin/buildings"    element={<BuildingsAdminPage />} />
          <Route path="/db-admin/nav-packages" element={<NavPackagesPage />} />
        </Route>

        {/* Default */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
