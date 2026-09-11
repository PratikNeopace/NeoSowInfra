import re

with open('/Users/pratikghodke/Desktop/NeoSowInfraFrontendMain/src/pages/AdminPanel.jsx', 'r') as f:
    content = f.read()

# 1. Add state for phone in new user form
content = content.replace("const [password, setPassword] = useState('');", "const [password, setPassword] = useState('');\n  const [phone, setPhone] = useState('');")

# 2. Add phone to API call in createUser
create_user_api = """      await API.post('/admin/users', {
        email,
        password,
        phone,
        roles: selectedRoles
      });"""
content = content.replace("""      await API.post('/admin/users', {
        email,
        password,
        roles: selectedRoles
      });""", create_user_api)

# Clear phone on success
content = content.replace("setPassword('');", "setPassword('');\n      setPhone('');")

# 3. Add Phone field in New User Form JSX
new_user_phone_jsx = """
                      <div className="mb-3">
                        <label className="form-label text-secondary fw-semibold small">Phone Number</label>
                        <input 
                          type="text" 
                          className="form-control" 
                          placeholder="e.g. 9876543210"
                          value={phone}
                          onChange={e => setPhone(e.target.value)}
                          required 
                        />
                      </div>
"""
content = content.replace("""                      <div className="mb-3">
                        <label className="form-label text-secondary fw-semibold small">Password</label>""", new_user_phone_jsx + """                      <div className="mb-3">
                        <label className="form-label text-secondary fw-semibold small">Password</label>""")

# 4. Add state for editPhone in Edit User Modal
content = content.replace("const [editEmail, setEditEmail] = useState('');", "const [editEmail, setEditEmail] = useState('');\n  const [editPhone, setEditPhone] = useState('');")

# 5. Populate editPhone in openEditUserModal
content = content.replace("setEditEmail(u.email);", "setEditEmail(u.email);\n    setEditPhone(u.phone || '');")

# 6. Add phone to API payload in handleUpdateUser
update_payload = """      const payload = {
        email: editEmail,
        phone: editPhone,
        roles: editRoles,
        enabled: editEnabled
      };"""
content = content.replace("""      const payload = {
        email: editEmail,
        roles: editRoles,
        enabled: editEnabled
      };""", update_payload)

# 7. Add Phone field in Edit User Form JSX
edit_user_phone_jsx = """
                        <div className="mb-3">
                          <label className="form-label text-dark fw-semibold small">Phone Number</label>
                          <input 
                            type="text" 
                            className="form-control" 
                            value={editPhone}
                            onChange={e => setEditPhone(e.target.value)}
                          />
                        </div>
"""
content = content.replace("""                        <div className="mb-3">
                          <label className="form-label text-dark fw-semibold small">New Password (leave blank to keep current)</label>""", edit_user_phone_jsx + """                        <div className="mb-3">
                          <label className="form-label text-dark fw-semibold small">New Password (leave blank to keep current)</label>""")

with open('/Users/pratikghodke/Desktop/NeoSowInfraFrontendMain/src/pages/AdminPanel.jsx', 'w') as f:
    f.write(content)

