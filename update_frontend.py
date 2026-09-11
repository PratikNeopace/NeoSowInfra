with open('/Users/pratikghodke/Desktop/NeoSowInfraFrontendMain/src/pages/AdminPanel.jsx', 'r') as f:
    content = f.read()

# 1. Add states
state_addition = """
  // Edit User State
  const [editUserObj, setEditUserObj] = useState(null);
  const [editEmail, setEditEmail] = useState('');
  const [editPassword, setEditPassword] = useState('');
  const [editRoles, setEditRoles] = useState([]);
  const [editEnabled, setEditEnabled] = useState(true);
  const [updatingUser, setUpdatingUser] = useState(false);
"""
content = content.replace("const [usersSearch, setUsersSearch] = useState('');", "const [usersSearch, setUsersSearch] = useState('');\n" + state_addition)

# 2. Add functions
funcs = """
  const openEditUserModal = (u) => {
    setEditUserObj(u);
    setEditEmail(u.email);
    setEditPassword('');
    setEditRoles(u.roles.map(r => r.replace('ROLE_', '')));
    setEditEnabled(u.enabled);
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    setUpdatingUser(true);
    try {
      const payload = {
        email: editEmail,
        roles: editRoles,
        enabled: editEnabled
      };
      if (editPassword) {
        payload.password = editPassword;
      }
      import('./services/api.js').then(async (m) => {
          const API = m.default;
          await API.put(`/admin/users/${editUserObj.id}`, payload);
          alert('User updated successfully!');
          setEditUserObj(null);
          fetchUsers(usersPage);
      }).catch(async (err) => {
        // Fallback for direct API import
          await API.put(`/admin/users/${editUserObj.id}`, payload);
          alert('User updated successfully!');
          setEditUserObj(null);
          fetchUsers(usersPage);
      })
    } catch (err) {
      console.error('Failed to update user', err);
      alert(err.response?.data?.message || 'Error updating user.');
    } finally {
      setUpdatingUser(false);
    }
  };

  const handleEditRoleCheckbox = (role) => {
    if (editRoles.includes(role)) {
      setEditRoles(prev => prev.filter(r => r !== role));
    } else {
      setEditRoles(prev => [...prev, role]);
    }
  };
"""
content = content.replace("const handleDeleteUser = async (id, emailStr) => {", funcs + "\n  const handleDeleteUser = async (id, emailStr) => {")

# 3. Replace onClick for Edit button
content = content.replace("onClick={() => alert('Edit profile details feature is under development.')}", "onClick={() => openEditUserModal(u)}")

# 4. Add Edit Modal
modal_jsx = """
                {/* Edit User Modal */}
                {editUserObj && (
                  <div 
                    className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
                    style={{
                      backgroundColor: 'rgba(0,0,0,0.5)',
                      backdropFilter: 'blur(5px)',
                      zIndex: 1050
                    }}
                  >
                    <div 
                      className="card border-0 shadow-lg p-4 animate__animated animate__fadeInUp" 
                      style={{ width: '500px', borderRadius: '12px', background: 'white' }}
                    >
                      <div className="d-flex justify-content-between align-items-center mb-3 border-bottom pb-2">
                        <h5 className="text-primary fw-bold mb-0">
                          <i className="fas fa-edit me-2"></i> Edit User
                        </h5>
                        <button 
                          type="button" 
                          className="btn-close" 
                          onClick={() => setEditUserObj(null)}
                        ></button>
                      </div>

                      <form onSubmit={handleUpdateUser}>
                        <div className="mb-3">
                          <label className="form-label text-dark fw-semibold small">Email Address</label>
                          <input 
                            type="email" 
                            className="form-control" 
                            value={editEmail}
                            onChange={e => setEditEmail(e.target.value)}
                            required 
                          />
                        </div>

                        <div className="mb-3">
                          <label className="form-label text-dark fw-semibold small">New Password (leave blank to keep current)</label>
                          <input 
                            type="password" 
                            className="form-control" 
                            value={editPassword}
                            onChange={e => setEditPassword(e.target.value)}
                            minLength={6}
                          />
                        </div>
                        
                        <div className="mb-3">
                          <label className="form-label text-dark fw-semibold small">Status</label>
                          <div className="form-check form-switch">
                            <input 
                              className="form-check-input" 
                              type="checkbox" 
                              checked={editEnabled}
                              onChange={e => setEditEnabled(e.target.checked)}
                            />
                            <label className="form-check-label">{editEnabled ? 'Active' : 'Blocked'}</label>
                          </div>
                        </div>

                        <div className="mb-4">
                          <label className="form-label text-dark fw-semibold small">Roles</label>
                          <div className="d-flex gap-3">
                            {['USER', 'ADMIN', 'SUPER_ADMIN'].map(role => (
                              <div className="form-check" key={role}>
                                <input 
                                  className="form-check-input" 
                                  type="checkbox" 
                                  checked={editRoles.includes(role)}
                                  onChange={() => handleEditRoleCheckbox(role)}
                                />
                                <label className="form-check-label small">{role}</label>
                              </div>
                            ))}
                          </div>
                        </div>

                        <div className="d-flex gap-2">
                          <button 
                            type="button" 
                            className="btn btn-outline-secondary w-50 fw-bold"
                            onClick={() => setEditUserObj(null)}
                          >
                            Cancel
                          </button>
                          <button 
                            type="submit" 
                            className="btn btn-primary w-50 fw-bold"
                            disabled={updatingUser}
                            style={{ backgroundColor: '#174D3A', border: 'none', borderRadius: '8px' }}
                          >
                            {updatingUser ? 'Saving...' : 'Save Changes'}
                          </button>
                        </div>
                      </form>
                    </div>
                  </div>
                )}
"""
content = content.replace("{activeTab === 'admins' && isSuperAdmin && (", modal_jsx + "\n        {activeTab === 'admins' && isSuperAdmin && (")

with open('/Users/pratikghodke/Desktop/NeoSowInfraFrontendMain/src/pages/AdminPanel.jsx', 'w') as f:
    f.write(content)

