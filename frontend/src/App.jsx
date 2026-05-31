import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import RegistrarTransaccion from './pages/RegistrarTransaccion'
import AlertasActivas from './pages/AlertasActivas'
import MotorKNN from './pages/MotorKNN'
import ConsultasMultidimensionales from './pages/ConsultasMultidimensionales'
import ConsultaID from './pages/ConsultaID'
import EstadoKDTree from './pages/EstadoKDTree'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="registrar" element={<RegistrarTransaccion />} />
        <Route path="alertas" element={<AlertasActivas />} />
        <Route path="knn" element={<MotorKNN />} />
        <Route path="consultas" element={<ConsultasMultidimensionales />} />
        <Route path="buscar" element={<ConsultaID />} />
        <Route path="estado" element={<EstadoKDTree />} />
      </Route>
    </Routes>
  )
}
