import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:neo_sow_infra/api/api_services.dart';
import 'package:neo_sow_infra/screens/login_page.dart';
import 'package:neo_sow_infra/screens/quotation_management_page.dart';
import 'package:shared_preferences/shared_preferences.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  String _email = '';
  String _role = '';
  String _token = '';
  bool _isLoading = true;
  Map<String, dynamic>? _dashboardData;
  String _selectedDate = 'Mar 13, 2025';
  String _selectedRevenueRange = 'Last 6 Months';

  @override
  void initState() {
    super.initState();
    _loadSessionAndFetchData();
  }

  Future<void> _loadSessionAndFetchData() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken') ?? '';
    final role = prefs.getString('role') ?? '';
    final email = prefs.getString('email') ?? '';

    setState(() {
      _token = token;
      _role = role;
      _email = email;
    });

    if (token.isNotEmpty) {
      final data = await ApiService.fetchDashboardData(token, role);
      setState(() {
        _dashboardData = data;
        _isLoading = false;
      });
    } else {
      _isLoading = false;
    }
  }

  Future<void> _logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();

    if (mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const LoginPage()),
      );
    }
  }

  String get _userName {
    if (_email.isNotEmpty && _email.contains('@')) {
      final part = _email.split('@')[0];
      return part[0].toUpperCase() + part.substring(1);
    }
    return "Sunil";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F8FA),
      body: SafeArea(
        child: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: Color(0xFF0F6127)),
              )
            : SingleChildScrollView(
                physics: const BouncingScrollPhysics(),
                padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 1. Top Bar Header (Logo, Bell badge, Avatar Profile)
                    _buildTopHeader(),
                    const SizedBox(height: 20),

                    // 2. Greeting Section
                    _buildGreetingSection(),
                    const SizedBox(height: 16),

                    // 3. Search Bar
                    _buildSearchBar(),
                    const SizedBox(height: 20),

                    // 4. Action Controls Bar (Date Dropdown, Import, New Quotation)
                    _buildActionControls(),
                    const SizedBox(height: 20),

                    // 5. KPI Metrics Grid (2x2)
                    _buildKpiMetricsGrid(),
                    const SizedBox(height: 28),

                    // 6. Recent Quotations Section
                    _buildRecentQuotationsHeader(),
                    const SizedBox(height: 12),
                    _buildRecentQuotationsList(),
                    const SizedBox(height: 28),

                    // 7. Today's Focus Section
                    _buildTodaysFocusSection(),
                    const SizedBox(height: 28),

                    // 8. Revenue Trend Section (Chart)
                    _buildRevenueTrendSection(),
                    const SizedBox(height: 28),

                    // 9. Quick Tools Section
                    _buildQuickToolsSection(),
                    const SizedBox(height: 32),
                  ],
                ),
              ),
      ),
    );
  }

  // --- 1. Top Header ---
  Widget _buildTopHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        // SVG Logo
        SvgPicture.asset(
          'lib/assets/splashscreen/logo.svg',
          height: 36,
          fit: BoxFit.contain,
        ),
        // Action icons: Notification Bell & Profile Avatar
        Row(
          children: [
            // Bell Notification with Badge
            Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    shape: BoxShape.circle,
                    border: Border.all(color: const Color(0xFFE5E5EA)),
                  ),
                  child: const Icon(
                    Icons.notifications_none_rounded,
                    color: Color(0xFF4A4A4A),
                    size: 22,
                  ),
                ),
                Positioned(
                  top: 8,
                  right: 8,
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: Colors.redAccent,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(width: 12),
            // User Profile Circle Avatar
            GestureDetector(
              onTap: _logout,
              child: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  border: Border.all(color: const Color(0xFFE5E5EA), width: 1.5),
                ),
                child: const Icon(
                  Icons.person_outline_rounded,
                  color: Color(0xFF4A4A4A),
                  size: 22,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  // --- 2. Greeting Section ---
  Widget _buildGreetingSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              "Good Afternoon,",
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1D1D1F),
              ),
            ),
          ],
        ),
        const SizedBox(height: 2),
        Row(
          children: [
            Text(
              "$_userName ",
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1D1D1F),
              ),
            ),
            const Text(
              "👋",
              style: TextStyle(fontSize: 20),
            ),
          ],
        ),
        const SizedBox(height: 6),
        const Text(
          "Overview of your architectural pipeline today.",
          style: TextStyle(
            fontSize: 13,
            color: Color(0xFF7D7D7D),
            fontWeight: FontWeight.w400,
          ),
        ),
      ],
    );
  }

  // --- 3. Search Bar ---
  Widget _buildSearchBar() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFF0F2F5),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: const [
          Icon(Icons.search_rounded, color: Color(0xFF8E8E93), size: 22),
          SizedBox(width: 12),
          Expanded(
            child: Text(
              "What are you looking for...",
              style: TextStyle(
                color: Color(0xFF9EA0A5),
                fontSize: 14,
              ),
            ),
          ),
          Icon(Icons.tune_rounded, color: Color(0xFF8E8E93), size: 20),
        ],
      ),
    );
  }

  // --- 4. Action Controls Bar ---
  Widget _buildActionControls() {
    return Column(
      children: [
        // Date Selector Button
        Align(
          alignment: Alignment.centerLeft,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: const Color(0xFFE5E5EA)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.calendar_today_outlined, size: 16, color: Color(0xFF4A4A4A)),
                const SizedBox(width: 8),
                Text(
                  _selectedDate,
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF1D1D1F),
                  ),
                ),
                const SizedBox(width: 6),
                const Icon(Icons.keyboard_arrow_down_rounded, size: 18, color: Color(0xFF8E8E93)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),

        // Action Buttons Row (Import & New Quotation)
        Row(
          children: [
            // Import Button
            Expanded(
              child: SizedBox(
                height: 46,
                child: OutlinedButton.icon(
                  onPressed: () {},
                  icon: const Icon(Icons.download_rounded, size: 18, color: Color(0xFF1D1D1F)),
                  label: const Text(
                    "Import",
                    style: TextStyle(
                      color: Color(0xFF1D1D1F),
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                  style: OutlinedButton.styleFrom(
                    backgroundColor: Colors.white,
                    side: const BorderSide(color: Color(0xFFE5E5EA)),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),

            // New Quotation Button
            Expanded(
              child: SizedBox(
                height: 46,
                child: ElevatedButton.icon(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const QuotationManagementPage(),
                      ),
                    );
                  },
                  icon: const Icon(Icons.add, size: 20, color: Colors.white),
                  label: const Text(
                    "New Quotation",
                    style: TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF0F6127),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  // --- 5. KPI Metrics Grid ---
  Widget _buildKpiMetricsGrid() {
    final double totalQ = _dashboardData?['totalQuotations']?.toDouble() ?? 128.0;
    final double approvedQ = _dashboardData?['approvedQuotations']?.toDouble() ?? 86.0;
    final double rev = _dashboardData?['totalQuotationAmount']?.toDouble() ?? 2400.0;
    final double clients = _dashboardData?['activeClients']?.toDouble() ?? 45.0;

    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: _buildKpiCard(
                title: "TOTAL QUOTATIONS",
                value: totalQ.toInt().toString(),
                badgeText: "▲ 12%",
                badgeBg: const Color(0xFFFFF5E5),
                badgeTextColor: const Color(0xFFE68A00),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildKpiCard(
                title: "APPROVED",
                value: approvedQ.toInt().toString(),
                badgeText: "▲ 8%",
                badgeBg: const Color(0xFFE6F7ED),
                badgeTextColor: const Color(0xFF0F6127),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _buildKpiCard(
                title: "EST. REVENUE",
                value: "₹ ${rev.toInt()}",
                badgeText: "▲ 6.5%",
                badgeBg: const Color(0xFFFFF5E5),
                badgeTextColor: const Color(0xFFE68A00),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildKpiCard(
                title: "ACTIVE CLIENTS",
                value: clients.toInt().toString(),
                badgeText: "▼ 2 new",
                badgeBg: const Color(0xFFFFF5E5),
                badgeTextColor: const Color(0xFFE68A00),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildKpiCard({
    required String title,
    required String value,
    required String badgeText,
    required Color badgeBg,
    required Color badgeTextColor,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFF2F2F2)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.02),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.bold,
              color: Color(0xFF7D7D7D),
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                value,
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1D1D1F),
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: badgeBg,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  badgeText,
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: badgeTextColor,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // --- 6. Recent Quotations Section ---
  Widget _buildRecentQuotationsHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        const Text(
          "Recent Quotations",
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Color(0xFF1D1D1F),
          ),
        ),
        GestureDetector(
          onTap: () {
            Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => const QuotationManagementPage(),
              ),
            );
          },
          child: const Text(
            "View all",
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: Color(0xFFB89552),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildRecentQuotationsList() {
    final quotes = [
      {
        'title': 'Skyline Penthouse',
        'code': '#QT-2491',
        'client': 'Rohan Mehra',
        'price': '\$124,500',
        'status': 'Sent',
        'statusBg': const Color(0xFF0F6127),
        'statusText': Colors.white,
      },
      {
        'title': 'Modern Villa A2',
        'code': '#QT-2316',
        'client': 'Sara Khan',
        'price': '\$89,200',
        'status': 'Approved',
        'statusBg': const Color(0xFFE6F7ED),
        'statusText': const Color(0xFF0F6127),
      },
      {
        'title': 'Office Fit-out',
        'code': '#QT-2395',
        'client': 'TechCorp Inc.',
        'price': '\$89,200',
        'status': 'Draft',
        'statusBg': const Color(0xFFFFF5E5),
        'statusText': const Color(0xFFE68A00),
      },
      {
        'title': 'Office Fit-out',
        'code': '#QT-2396',
        'client': 'TechCorp Inc.',
        'price': '\$89,200',
        'status': 'Draft',
        'statusBg': const Color(0xFFFFF5E5),
        'statusText': const Color(0xFFE68A00),
      },
    ];

    return Column(
      children: quotes.map((item) {
        return Container(
          margin: const EdgeInsets.only(bottom: 12),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFF2F2F2)),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.015),
                blurRadius: 8,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Title & Code
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    item['title'] as String,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1D1D1F),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Text(
                item['code'] as String,
                style: const TextStyle(
                  fontSize: 11,
                  color: Color(0xFF8E8E93),
                ),
              ),
              const SizedBox(height: 12),

              // Bottom details row (Client, Price, Status Badge, Eye icon)
              Row(
                children: [
                  Text(
                    item['client'] as String,
                    style: const TextStyle(
                      fontSize: 13,
                      color: Color(0xFF8E8E93),
                    ),
                  ),
                  const Spacer(),
                  Text(
                    item['price'] as String,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1D1D1F),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: item['statusBg'] as Color,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      item['status'] as String,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: item['statusText'] as Color,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Icon(
                    Icons.remove_red_eye_outlined,
                    size: 16,
                    color: Color(0xFFC7C7CC),
                  ),
                ],
              ),
            ],
          ),
        );
      }).toList(),
    );
  }

  // --- 7. Today's Focus Section ---
  Widget _buildTodaysFocusSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "Today's Focus",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1D1D1F),
              ),
            ),
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: const Color(0xFFB89552), width: 1.5),
              ),
              child: const Icon(
                Icons.add,
                size: 18,
                color: Color(0xFFB89552),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: const Color(0xFFF2F2F2)),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.015),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            children: [
              _buildFocusBullet(
                title: "Follow up: Patel Residence",
                subtitle: "Review late feedback",
              ),
              const SizedBox(height: 16),
              _buildFocusBullet(
                title: "Prepare BOQ: Villa Project",
                subtitle: "Due by 5:30 PM",
              ),
              const SizedBox(height: 16),
              _buildFocusBullet(
                title: "Site Visit: Lofts",
                subtitle: "Today at 2:00 PM",
              ),
              const SizedBox(height: 20),

              // Open Calendar Button
              SizedBox(
                width: double.infinity,
                height: 44,
                child: ElevatedButton(
                  onPressed: () {},
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFE6F4EA),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text(
                    "Open Calendar",
                    style: TextStyle(
                      color: Color(0xFF0F6127),
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildFocusBullet({required String title, required String subtitle}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.only(top: 5),
          width: 8,
          height: 8,
          decoration: const BoxDecoration(
            color: Color(0xFFE68A00),
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1D1D1F),
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: const TextStyle(
                  fontSize: 12,
                  color: Color(0xFF8E8E93),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  // --- 8. Revenue Trend Section ---
  Widget _buildRevenueTrendSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "Revenue Trend",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1D1D1F),
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFE5E5EA)),
              ),
              child: Row(
                children: [
                  Text(
                    _selectedRevenueRange,
                    style: const TextStyle(
                      fontSize: 12,
                      color: Color(0xFF8E8E93),
                    ),
                  ),
                  const SizedBox(width: 4),
                  const Icon(Icons.keyboard_arrow_down_rounded, size: 16, color: Color(0xFF8E8E93)),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: const Color(0xFFF2F2F2)),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.015),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            children: [
              // Custom Bar Chart representation
              SizedBox(
                height: 130,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    _buildBar(heightRatio: 0.3, isHighlighted: false, month: "JAN"),
                    _buildBar(heightRatio: 0.45, isHighlighted: false, month: "FEB"),
                    _buildBar(heightRatio: 1.0, isHighlighted: true, month: "MAR"),
                    _buildBar(heightRatio: 0.6, isHighlighted: false, month: "APR"),
                    _buildBar(heightRatio: 0.75, isHighlighted: false, month: "MAY"),
                    _buildBar(heightRatio: 0.5, isHighlighted: false, month: "JUN"),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildBar({
    required double heightRatio,
    required bool isHighlighted,
    required String month,
  }) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Container(
          width: 38,
          height: 90 * heightRatio,
          decoration: BoxDecoration(
            color: isHighlighted ? const Color(0xFF0F6127) : const Color(0xFFE6F0E9),
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        const SizedBox(height: 10),
        Text(
          month,
          style: TextStyle(
            fontSize: 10,
            fontWeight: isHighlighted ? FontWeight.bold : FontWeight.w600,
            color: isHighlighted ? const Color(0xFF0F6127) : const Color(0xFF8E8E93),
          ),
        ),
      ],
    );
  }

  // --- 9. Quick Tools Section ---
  Widget _buildQuickToolsSection() {
    final tools = [
      {
        'icon': Icons.person_add_alt_1_outlined,
        'label': 'New\nCustomer',
        'isHighlighted': false,
      },
      {
        'icon': Icons.apartment_rounded,
        'label': 'Add Project',
        'isHighlighted': false,
      },
      {
        'icon': Icons.calculate_outlined,
        'label': 'Gen. BOQ',
        'isHighlighted': false,
      },
      {
        'icon': Icons.design_services_outlined,
        'label': 'Drawings',
        'isHighlighted': false,
      },
      {
        'icon': Icons.show_chart_rounded,
        'label': 'Reports',
        'isHighlighted': false,
      },
      {
        'icon': Icons.settings_outlined,
        'label': 'Customize',
        'isHighlighted': true,
      },
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          "Quick Tools",
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Color(0xFF1D1D1F),
          ),
        ),
        const SizedBox(height: 14),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: tools.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.0,
          ),
          itemBuilder: (context, index) {
            final tool = tools[index];
            final bool isHighlighted = tool['isHighlighted'] as bool;

            return Container(
              decoration: BoxDecoration(
                color: isHighlighted ? const Color(0xFFE6F4EA) : Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: isHighlighted ? Colors.transparent : const Color(0xFFF2F2F2),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.015),
                    blurRadius: 8,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    tool['icon'] as IconData,
                    color: const Color(0xFF0F6127),
                    size: 26,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    tool['label'] as String,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: Color(0xFF1D1D1F),
                      height: 1.2,
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ],
    );
  }
}