package com.neosow.infra.service;

import com.neosow.infra.dto.dashboard.AdminDashboardDTO;
import com.neosow.infra.dto.dashboard.SuperAdminDashboardDTO;
import com.neosow.infra.dto.dashboard.UserDashboardDTO;

public interface DashboardService {
    UserDashboardDTO getUserDashboard();
    AdminDashboardDTO getAdminDashboard();
    SuperAdminDashboardDTO getSuperAdminDashboard();
}
