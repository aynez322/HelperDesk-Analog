import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { CreateTicketPage } from './pages/client/CreateTicketPage';
import { MyTicketsPage } from './pages/client/MyTicketsPage';
import { ClientTicketDetailPage } from './pages/client/TicketDetailPage';
import { AgentTicketQueuePage } from './pages/agent/TicketQueuePage';
import { AgentTicketDetailPage } from './pages/agent/TicketDetailPage';
import { UserAdminPage } from './pages/manager/UserAdminPage';

function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <Routes>
            {/* Public */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Client routes */}
            <Route path="/tickets/new" element={
              <ProtectedRoute><CreateTicketPage /></ProtectedRoute>
            } />
            <Route path="/tickets" element={
              <ProtectedRoute roles={['CLIENT', 'AGENT', 'MANAGER']}>
                <MyTicketsPage />
              </ProtectedRoute>
            } />
            <Route path="/tickets/:id" element={
              <ProtectedRoute><ClientTicketDetailPage /></ProtectedRoute>
            } />

            {/* Agent routes */}
            <Route path="/agent/tickets" element={
              <ProtectedRoute roles={['AGENT', 'MANAGER']}>
                <AgentTicketQueuePage />
              </ProtectedRoute>
            } />
            <Route path="/agent/tickets/:id" element={
              <ProtectedRoute roles={['AGENT', 'MANAGER']}>
                <AgentTicketDetailPage />
              </ProtectedRoute>
            } />

            {/* Manager routes */}
            <Route path="/manager/users" element={
              <ProtectedRoute roles={['MANAGER']}>
                <UserAdminPage />
              </ProtectedRoute>
            } />

            {/* Default redirect */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}

export default App;
