import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:ui';
import '../api/api_services.dart';
import 'login_page.dart';
import 'dashboard_page.dart';
import 'proforma_invoice_page.dart';

class QuotationManagementPage extends StatefulWidget {
  const QuotationManagementPage({super.key});

  @override
  State<QuotationManagementPage> createState() => _QuotationManagementPageState();
}

class _QuotationManagementPageState extends State<QuotationManagementPage> {
  String _token = '';
  bool _isLoading = true;
  List<dynamic> _customers = [];
  List<dynamic> _filteredCustomers = [];
  final TextEditingController _searchController = TextEditingController();

  // State for nested quotations list
  final Set<String> _expandedCustomerIds = {};
  final Map<String, List<dynamic>> _customerQuotations = {};
  final Map<String, bool> _loadingQuotations = {};

  // State for parent quotation revisions expansion
  final Set<String> _expandedParentQuotationIds = {};
  // Controllers for horizontal scrollbars
  final Map<String, ScrollController> _scrollControllers = {};

  @override
  void initState() {
    super.initState();
    _loadSessionAndFetchCustomers();
    _searchController.addListener(_filterCustomers);
  }

  @override
  void dispose() {
    _searchController.dispose();
    for (var controller in _scrollControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _loadSessionAndFetchCustomers() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken') ?? '';

    setState(() {
      _token = token;
    });

    if (token.isNotEmpty) {
      final data = await ApiService.fetchCustomers(token);
      if (data != null && data['content'] != null) {
        setState(() {
          _customers = data['content'];
          _filteredCustomers = _customers;
          _isLoading = false;
        });
      } else {
        setState(() {
          _isLoading = false;
        });
      }
    } else {
      _logout();
    }
  }

  void _filterCustomers() {
    final query = _searchController.text.toLowerCase();
    setState(() {
      _filteredCustomers = _customers.where((cust) {
        final name = (cust['name'] ?? '').toString().toLowerCase();
        final phone = (cust['phone'] ?? '').toString().toLowerCase();
        return name.contains(query) || phone.contains(query);
      }).toList();
    });
  }

  Future<void> _toggleQuotations(String customerId) async {
    print("Toggling quotations for customer: $customerId");
    if (_expandedCustomerIds.contains(customerId)) {
      setState(() {
        _expandedCustomerIds.remove(customerId);
      });
      print("Collapsed quotations panel");
    } else {
      setState(() {
        _expandedCustomerIds.add(customerId);
      });
      print("Expanded quotations panel. Token empty? ${_token.isEmpty}");

      // If we don't have the quotations for this customer yet, fetch them
      if (!_customerQuotations.containsKey(customerId)) {
        setState(() {
          _loadingQuotations[customerId] = true;
        });

        print("Fetching quotations from API...");
        final data = await ApiService.fetchQuotationsByCustomer(_token, customerId);
        print("API Response: $data");
        
        setState(() {
          _loadingQuotations[customerId] = false;
          if (data != null && data['content'] != null) {
            _customerQuotations[customerId] = data['content'];
            print("Successfully loaded ${data['content'].length} quotations");
          } else {
            _customerQuotations[customerId] = [];
            print("No quotations loaded (null or empty content)");
          }
        });
      }
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

  @override
  Widget build(BuildContext context) {
    final gradientColors = [
      const Color(0xFF5D78DE),
      const Color(0xFF6E56C4),
      const Color(0xFF7A4AA8),
    ];

    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: gradientColors,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: Column(
          children: [
            // 1. Navigation Header Bar
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              color: Colors.black.withOpacity(0.15),
              child: SafeArea(
                bottom: false,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: const [
                        Icon(Icons.home_work_outlined, color: Colors.white, size: 24),
                        SizedBox(width: 8),
                        Text(
                          "Portal",
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 18,
                          ),
                        ),
                      ],
                    ),
                    Row(
                      children: [
                        TextButton(
                          onPressed: () {
                            Navigator.pushReplacement(
                              context,
                              MaterialPageRoute(builder: (context) => const DashboardPage()),
                            );
                          },
                          child: const Text(
                            "Dashboard",
                            style: TextStyle(color: Colors.white70),
                          ),
                        ),
                        const SizedBox(width: 8),
                        TextButton(
                          onPressed: () {},
                          child: const Text(
                            "Quotations",
                            style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                          ),
                        ),
                        const SizedBox(width: 16),
                        ElevatedButton(
                          onPressed: _logout,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.blueAccent,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(6),
                            ),
                          ),
                          child: const Text("Logout"),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            // 2. Main Content
            Expanded(
              child: _isLoading
                  ? const Center(child: CircularProgressIndicator(color: Colors.white))
                  : SingleChildScrollView(
                      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
                      child: Center(
                        child: Container(
                          constraints: const BoxConstraints(maxWidth: 1000),
                          child: Card(
                            elevation: 4,
                            color: Colors.white,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            child: Padding(
                              padding: const EdgeInsets.all(24.0),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Row(
                                        children: const [
                                          Icon(Icons.assignment_outlined, color: Color(0xFF1EA896), size: 28),
                                          SizedBox(width: 12),
                                          Text(
                                            "Quotation Management",
                                            style: TextStyle(
                                              fontSize: 22,
                                              fontWeight: FontWeight.bold,
                                              color: Color(0xFF334EAC),
                                            ),
                                          ),
                                        ],
                                      ),
                                      ElevatedButton.icon(
                                        onPressed: () {},
                                        icon: const Icon(Icons.add_circle_outline, size: 18),
                                        label: const Text("Create Customer"),
                                        style: ElevatedButton.styleFrom(
                                          backgroundColor: Colors.blueAccent,
                                          foregroundColor: Colors.white,
                                          shape: RoundedRectangleBorder(
                                            borderRadius: BorderRadius.circular(8),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 20),

                                  TextField(
                                    controller: _searchController,
                                    decoration: InputDecoration(
                                      hintText: "Search by customer name or phone number...",
                                      prefixIcon: const Icon(Icons.search, color: Colors.grey),
                                      contentPadding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                                      border: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(8),
                                        borderSide: BorderSide(color: Colors.grey.shade300),
                                      ),
                                      enabledBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(8),
                                        borderSide: BorderSide(color: Colors.grey.shade300),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: 20),

                                  _filteredCustomers.isEmpty
                                      ? const Center(
                                          child: Padding(
                                            padding: EdgeInsets.symmetric(vertical: 40.0),
                                            child: Text(
                                              "No customers found.",
                                              style: TextStyle(color: Colors.grey),
                                            ),
                                          ),
                                        )
                                      : ListView.separated(
                                          shrinkWrap: true,
                                          physics: const NeverScrollableScrollPhysics(),
                                          itemCount: _filteredCustomers.length,
                                          separatorBuilder: (context, index) => const SizedBox(height: 16),
                                          itemBuilder: (context, index) {
                                            final customer = _filteredCustomers[index];
                                            return _buildCustomerCard(customer);
                                          },
                                        ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCustomerCard(Map<String, dynamic> customer) {
    final String customerId = customer['id'] ?? '';
    final name = customer['name'] ?? 'Unknown';
    final phone = customer['phone'] ?? '';
    final address = customer['address'] ?? '';
    String createdByEmail = '';
    final rawCreatedBy = customer['createdBy'];
    if (rawCreatedBy is Map) {
      createdByEmail = rawCreatedBy['email'] ?? '';
    } else if (rawCreatedBy is String) {
      createdByEmail = rawCreatedBy;
    }

    String createdBy = createdByEmail;
    if (createdByEmail.contains('@')) {
      final part = createdByEmail.split('@')[0];
      createdBy = part[0].toUpperCase() + part.substring(1);
    }

    final project = customer['project'] ?? {};
    final workType = project['workType'] ?? 'N/A';
    final carpetArea = project['carpetArea']?.toString() ?? '0';
    final areaUnit = project['areaUnit'] ?? 'SQFT';
    final budget = project['budget']?.toString() ?? '0';

    final isExpanded = _expandedCustomerIds.contains(customerId);
    final isMobile = MediaQuery.of(context).size.width < 650;

    final infoColumn = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          name,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.blueAccent,
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            const Icon(Icons.phone_outlined, size: 16, color: Colors.grey),
            const SizedBox(width: 4),
            Text("$phone | ", style: const TextStyle(color: Colors.black, fontSize: 13)),
            const Icon(Icons.location_on_outlined, size: 16, color: Colors.grey),
            const SizedBox(width: 4),
            Text(address, style: const TextStyle(color: Colors.black, fontSize: 13)),
          ],
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            const Icon(Icons.person_outline, size: 16, color: Colors.grey),
            const SizedBox(width: 4),
            Text("Created By: $createdBy", style: const TextStyle(color: Colors.black54, fontSize: 12)),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.grey.shade200,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                "$workType ($carpetArea $areaUnit)",
                style: const TextStyle(color: Colors.black, fontSize: 11, fontWeight: FontWeight.bold),
              ),
            ),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.green.shade100,
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                "Budget: ₹$budget",
                style: const TextStyle(color: Colors.green, fontSize: 11, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      ],
    );

    final actionsRow = Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        ElevatedButton.icon(
          onPressed: () => _toggleQuotations(customerId),
          icon: Icon(
            isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
            size: 16,
          ),
          label: Text(isExpanded ? "Hide Quotations" : "View Quotations"),
          style: ElevatedButton.styleFrom(
            backgroundColor: isExpanded ? Colors.blue.shade600 : Colors.white,
            foregroundColor: isExpanded ? Colors.white : Colors.blue.shade600,
            side: BorderSide(color: Colors.blue.shade600),
            elevation: 0,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
          ),
        ),
        OutlinedButton.icon(
          onPressed: () {},
          icon: const Icon(Icons.insert_drive_file_outlined, size: 16),
          label: const Text("Add Quote"),
          style: OutlinedButton.styleFrom(
            foregroundColor: Colors.green,
            side: const BorderSide(color: Colors.green),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
          ),
        ),
        OutlinedButton.icon(
          onPressed: () {},
          icon: const Icon(Icons.edit_outlined, size: 16),
          label: const Text("Edit"),
          style: OutlinedButton.styleFrom(
            foregroundColor: Colors.amber.shade700,
            side: BorderSide(color: Colors.amber.shade700),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(6)),
          ),
        ),
      ],
    );

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (isMobile) ...[
            infoColumn,
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 12),
            actionsRow,
          ] else ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Expanded(child: infoColumn),
                const SizedBox(width: 16),
                actionsRow,
              ],
            ),
          ],

          // 3. Nested Quotation List Section
          if (isExpanded) ...[
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 12),
            _buildNestedQuotationList(customer),
          ],
        ],
      ),
    );
  }

  Widget _buildNestedQuotationList(Map<String, dynamic> customer) {
    final String customerId = customer['id'] ?? '';
    final String customerName = customer['name'] ?? 'Unknown';
    final String customerPhone = customer['phone'] ?? '';
    final String customerAddress = customer['address'] ?? '';
    final project = customer['project'] ?? {};
    final String workType = project['workType'] ?? 'N/A';

    if (_loadingQuotations[customerId] == true) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20.0),
        child: Center(
          child: SizedBox(
            height: 24,
            width: 24,
            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.blueAccent),
          ),
        ),
      );
    }

    final quotations = _customerQuotations[customerId] ?? [];
    if (quotations.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 20.0),
        child: Center(
          child: Text(
            "No quotations found for this customer.",
            style: TextStyle(color: Colors.grey, fontSize: 13),
          ),
        ),
      );
    }    // Structure parent-child revisions:
    final parents = quotations.where((q) => q['parentQuotationId'] == null).toList();
    parents.sort((a, b) => (b['createdAt'] ?? '').toString().compareTo((a['createdAt'] ?? '').toString()));

    final List<Map<String, dynamic>> rowItems = [];
    
    for (var parent in parents) {
      final children = quotations.where((q) => q['parentQuotationId'] == parent['id']).toList();
      children.sort((a, b) => (a['createdAt'] ?? '').toString().compareTo((b['createdAt'] ?? '').toString()));

      rowItems.add({
        'data': parent,
        'isParent': true,
        'revisionText': '',
        'revisionCount': children.length,
      });

      // Expand revision child rows only if parent ID is in the set
      if (_expandedParentQuotationIds.contains(parent['id'])) {
        for (int i = 0; i < children.length; i++) {
          rowItems.add({
            'data': children[i],
            'isParent': false,
            'revisionText': 'Rev ${i + 1}: ',
            'revisionCount': 0,
          });
        }
      }
    }

    final scrollController = _scrollControllers.putIfAbsent(customerId, () => ScrollController());

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade100),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: const [
              Icon(Icons.assignment_outlined, color: Colors.grey, size: 20),
              SizedBox(width: 8),
              Text(
                "Quotation List",
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                  color: Color(0xFF334EAC),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // ScrollConfiguration enables drag scroll with Mouse, Trackpad, and Touch on all platforms (Web/Desktop)
          ScrollConfiguration(
            behavior: ScrollConfiguration.of(context).copyWith(
              dragDevices: {
                PointerDeviceKind.touch,
                PointerDeviceKind.mouse,
                PointerDeviceKind.trackpad,
              },
            ),
            child: Scrollbar(
              controller: scrollController,
              thumbVisibility: true,
              child: SingleChildScrollView(
                controller: scrollController,
                scrollDirection: Axis.horizontal,
                child: Container(
                  constraints: const BoxConstraints(minWidth: 1080),
                  child: Table(
                    columnWidths: const {
                      0: FixedColumnWidth(230), // Date Created
                      1: FixedColumnWidth(100), // Created By
                      2: FixedColumnWidth(130), // Status Dropdown
                      3: FixedColumnWidth(100), // Project Unit
                      4: FixedColumnWidth(100), // Subtotal
                      5: FixedColumnWidth(100), // Discount
                      6: FixedColumnWidth(130), // Total
                      7: FixedColumnWidth(290), // Actions
                    },
                    children: [
                      TableRow(
                        decoration: const BoxDecoration(
                          color: Color(0xFFDCE6F5),
                        ),
                        children: const [
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("DATE CREATED", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("CREATED BY", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("STATUS", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("PROJECT UNIT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("SUBTOTAL", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("DISCOUNT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("TOTAL (INC. GST)", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                          Padding(
                            padding: EdgeInsets.symmetric(vertical: 10, horizontal: 8),
                            child: Text("ACTIONS", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                          ),
                        ],
                      ),

                      ...rowItems.map((item) {
                        final quote = item['data'];
                        final isParent = item['isParent'] as bool;
                        final String revisionText = item['revisionText'];
                        final int revisionCount = item['revisionCount'] as int;

                        final String createdAt = quote['createdAt'] ?? '';
                        String dateStr = '';
                        if (createdAt.isNotEmpty) {
                          final dateTime = DateTime.tryParse(createdAt);
                          if (dateTime != null) {
                            final hourStr = dateTime.hour == 0 ? 12 : (dateTime.hour > 12 ? dateTime.hour - 12 : dateTime.hour);
                            final amPm = dateTime.hour >= 12 ? 'PM' : 'AM';
                            dateStr = "${dateTime.month}/${dateTime.day}/${dateTime.year}, $hourStr:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')} $amPm";
                          }
                        }

                        String createdByEmail = '';
                        final rawCreatedBy = quote['createdBy'];
                        if (rawCreatedBy is Map) {
                          createdByEmail = rawCreatedBy['email'] ?? '';
                        } else if (rawCreatedBy is String) {
                          createdByEmail = rawCreatedBy;
                        }

                        String createdBy = createdByEmail;
                        if (createdByEmail.contains('@')) {
                          final part = createdByEmail.split('@')[0];
                          createdBy = part[0].toUpperCase() + part.substring(1);
                        }

                        final String status = (quote['status'] ?? 'ENQUIRY').toString().toUpperCase();
                        final String selectedStatus = ['ENQUIRY', 'ONGOING', 'COMPLETED'].contains(status) ? status : 'ENQUIRY';

                        Color statusBorderColor = Colors.orange.shade300;
                        Color statusTextColor = Colors.orange.shade800;
                        if (selectedStatus == 'ONGOING') {
                          statusBorderColor = Colors.blue.shade300;
                          statusTextColor = Colors.blue.shade800;
                        } else if (selectedStatus == 'COMPLETED') {
                          statusBorderColor = Colors.green.shade300;
                          statusTextColor = Colors.green.shade800;
                        }

                        final String projectUnit = quote['projectUnit'] ?? 'Ft Inch';
                        final double subtotal = (quote['subtotal'] as num?)?.toDouble() ?? 0.0;
                        final double discount = (quote['discount'] as num?)?.toDouble() ?? 0.0;
                        final double totalAmount = (quote['totalAmount'] as num?)?.toDouble() ?? 0.0;

                        // Revision toggle helper
                        void toggleRevisions() {
                          final quoteId = isParent ? quote['id'] : quote['parentQuotationId'];
                          if (quoteId != null) {
                            setState(() {
                              if (_expandedParentQuotationIds.contains(quoteId)) {
                                _expandedParentQuotationIds.remove(quoteId);
                              } else {
                                _expandedParentQuotationIds.add(quoteId);
                              }
                            });
                          }
                        }

                        return TableRow(
                          decoration: BoxDecoration(
                            border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
                            color: Colors.white,
                          ),
                          children: [
                            // 1. Date Created (Clicking the parent's blue box or cell toggles revisions)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                              child: InkWell(
                                onTap: toggleRevisions,
                                borderRadius: BorderRadius.circular(4),
                                child: Row(
                                  children: [
                                    if (isParent) ...[
                                      Container(
                                        height: 10,
                                        width: 10,
                                        color: Colors.blueAccent,
                                      ),
                                      const SizedBox(width: 6),
                                      Expanded(
                                        child: Text(
                                          dateStr,
                                          style: const TextStyle(fontSize: 12, color: Colors.black, fontWeight: FontWeight.w600),
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                      if (revisionCount > 0) ...[
                                        const SizedBox(width: 6),
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                          decoration: BoxDecoration(
                                            color: Colors.grey.shade600,
                                            borderRadius: BorderRadius.circular(10),
                                          ),
                                          child: Text(
                                            "$revisionCount Rev",
                                            style: const TextStyle(color: Colors.white, fontSize: 9, fontWeight: FontWeight.bold),
                                          ),
                                        ),
                                      ],
                                    ] else ...[
                                      const SizedBox(width: 8),
                                      const Text("└── ", style: TextStyle(color: Colors.grey, fontSize: 12)),
                                      Expanded(
                                        child: Text(
                                          "$revisionText$dateStr",
                                          style: const TextStyle(fontSize: 12, color: Colors.black54),
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                    ],
                                  ],
                                ),
                              ),
                            ),
                            // 2. Created By
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                              child: Text(createdBy, style: const TextStyle(fontSize: 12, color: Colors.black)),
                            ),
                            // 3. Status Dropdown (Contains Enquiry, Ongoing, Completed)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                decoration: BoxDecoration(
                                  border: Border.all(color: statusBorderColor),
                                  borderRadius: BorderRadius.circular(4),
                                  color: Colors.white,
                                ),
                                child: DropdownButtonHideUnderline(
                                  child: DropdownButton<String>(
                                    value: selectedStatus,
                                    isDense: true,
                                    dropdownColor: Colors.white,
                                    icon: Icon(Icons.keyboard_arrow_down, size: 14, color: statusTextColor),
                                    items: const [
                                      DropdownMenuItem(
                                        value: 'ENQUIRY',
                                        child: Text("Enquiry", style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
                                      ),
                                      DropdownMenuItem(
                                        value: 'ONGOING',
                                        child: Text("Ongoing", style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
                                      ),
                                      DropdownMenuItem(
                                        value: 'COMPLETED',
                                        child: Text("Completed", style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
                                      ),
                                    ],
                                    style: TextStyle(color: statusTextColor, fontFamily: 'Outfit', fontWeight: FontWeight.w600),
                                    onChanged: (newVal) {
                                      if (newVal != null) {
                                        setState(() {
                                          quote['status'] = newVal;
                                        });
                                      }
                                    },
                                  ),
                                ),
                              ),
                            ),
                            // 4. Project Unit
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                              child: Text(projectUnit, style: const TextStyle(fontSize: 12, color: Colors.black)),
                            ),
                            // 5. Subtotal
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                              child: Text("₹${subtotal.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12, color: Colors.black)),
                            ),
                            // 6. Discount
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                              child: Text("-₹${discount.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12, color: Colors.redAccent, fontWeight: FontWeight.w600)),
                            ),
                            // 7. Total (Inc GST)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                              child: Text("₹${totalAmount.toStringAsFixed(2)}", style: const TextStyle(fontSize: 13, color: Colors.green, fontWeight: FontWeight.bold)),
                            ),
                            // 8. Actions (View also triggers toggleRevisions locally)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  InkWell(
                                    onTap: () {
                                      Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (context) => ProformaInvoicePage(
                                            quotationId: quote['id'],
                                            customerPhone: customerPhone,
                                            customerAddress: customerAddress,
                                            workType: workType,
                                          ),
                                        ),
                                      );
                                    },
                                    borderRadius: BorderRadius.circular(4),
                                    child: _buildTableActionButton("View", Icons.visibility_outlined, Colors.grey.shade600),
                                  ),
                                  const SizedBox(width: 4),
                                  _buildTableActionButton("PDF", Icons.picture_as_pdf_outlined, Colors.green),
                                  const SizedBox(width: 4),
                                  _buildTableActionButton("Edit", Icons.edit_outlined, Colors.blue),
                                  const SizedBox(width: 4),
                                  _buildTableActionButton("Delete", Icons.delete_outline, Colors.red),
                                ],
                              ),
                            ),
                          ],
                        );
                      }).toList(),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTableActionButton(String label, IconData icon, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
      decoration: BoxDecoration(
        border: Border.all(color: color.withOpacity(0.5)),
        borderRadius: BorderRadius.circular(4),
        color: Colors.white,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 12, color: color),
          const SizedBox(width: 2),
          Text(label, style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}
