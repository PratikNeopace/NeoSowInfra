import re

with open('/Users/pratikghodke/Desktop/NeoSowInfraFrontendMain/src/pages/AdminPanel.jsx', 'r') as f:
    content = f.read()

# Replace:
#                                     <div className="d-flex flex-column">
#                                       <span className="fw-semibold text-dark" style={{ fontSize: '14px' }}>{displayName}</span>
#                                       <span className="text-muted" style={{ fontSize: '12px' }}>{userCode} ({u.email})</span>
#                                     </div>
# With:
#                                     <div className="d-flex flex-column">
#                                       <span className="fw-semibold text-dark" style={{ fontSize: '14px' }}>{displayName}</span>
#                                       <span className="text-muted" style={{ fontSize: '12px' }}>{userCode} ({u.email})</span>
#                                       {u.phone && <span className="text-muted" style={{ fontSize: '11px' }}>📱 {u.phone}</span>}
#                                     </div>

content = content.replace(
"""                                    <div className="d-flex flex-column">
                                      <span className="fw-semibold text-dark" style={{ fontSize: '14px' }}>{displayName}</span>
                                      <span className="text-muted" style={{ fontSize: '12px' }}>{userCode} ({u.email})</span>
                                    </div>""",
"""                                    <div className="d-flex flex-column">
                                      <span className="fw-semibold text-dark" style={{ fontSize: '14px' }}>{displayName}</span>
                                      <span className="text-muted" style={{ fontSize: '12px' }}>{userCode} ({u.email})</span>
                                      {u.phone && <span className="text-muted mt-1" style={{ fontSize: '11px' }}>📱 {u.phone}</span>}
                                    </div>""")

with open('/Users/pratikghodke/Desktop/NeoSowInfraFrontendMain/src/pages/AdminPanel.jsx', 'w') as f:
    f.write(content)

