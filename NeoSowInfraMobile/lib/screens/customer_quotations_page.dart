import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../api/api_services.dart';
import 'login_page.dart';
import 'quotation_management_page.dart';
import 'dashboard_page.dart';

class CustomerQuotationsPage extends StatefulWidget {
  final String customerId;
  final String customerName;

  const CustomerQuotationsPage({
    super.key,
    required this.customerId,
    required this.customerName,
  });

  @override
  State<CustomerQuotationsPage> createState() => _CustomerQuotationsPageState();
}

class _CustomerQuotationsPageState extends State<CustomerQuotationsPage> {
  String _token = '';
  bool _isLoading = true;
  List<dynamic> _quotations = [];

  @override
  void initState() {
    super.initState();
    _loadSessionAndFetchQuotations();
  }

  Future<void> _loadSessionAndFetchQuotations() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken') ?? '';

    setState(() {
      _token = token;
    });

    if (token.isNotEmpty) {
      final data = await ApiService.fetchQuotationsByCustomer(token, widget.customerId);
      if (data != null && data['content'] != null) {
        setState(() {
          _quotations = data['content'];
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
            // 1. Top Navbar
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
                          onPressed: () {
                            Navigator.pushReplacement(
                              context,
                              MaterialPageRoute(builder: (context) => const QuotationManagementPage()),
                            );
                          },
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
                          constraints: const BoxConstraints(maxWidth: 900),
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
                                  // Back Button + Header Title
                                  Row(
                                    children: [
                                      IconButton(
                                        icon: const Icon(Icons.arrow_back, color: Color(0xFF334EAC)),
                                        onPressed: () {
                                          Navigator.pushReplacement(
                                            context,
                                            MaterialPageRoute(builder: (context) => const QuotationManagementPage()),
                                          );
                                        },
                                      ),
                                      const Icon(Icons.description_outlined, color: Color(0xFF1EA896), size: 28),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(
                                          "Quotations for ${widget.customerName}",
                                          style: const TextStyle(
                                            fontSize: 22,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF334EAC),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 24),

                                  _quotations.isEmpty
                                      ? const Center(
                                          child: Padding(
                                            padding: EdgeInsets.symmetric(vertical: 40.0),
                                            child: Text(
                                              "No quotations found for this customer.",
                                              style: TextStyle(color: Colors.grey),
                                            ),
                                          ),
                                        )
                                      : ListView.separated(
                                          shrinkWrap: true,
                                          physics: const NeverScrollableScrollPhysics(),
                                          itemCount: _quotations.length,
                                          separatorBuilder: (context, index) => const SizedBox(height: 20),
                                          itemBuilder: (context, index) {
                                            final quote = _quotations[index];
                                            return _buildQuotationCard(quote);
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

  Widget _buildQuotationCard(Map<String, dynamic> quote) {
    final String status = quote['status'] ?? 'ENQUIRY';
    final double subtotal = (quote['subtotal'] as num?)?.toDouble() ?? 0.0;
    final double discount = (quote['discount'] as num?)?.toDouble() ?? 0.0;
    final double discountPercent = (quote['discountPercent'] as num?)?.toDouble() ?? 0.0;
    final double gstAmount = (quote['gstAmount'] as num?)?.toDouble() ?? 0.0;
    final double totalAmount = (quote['totalAmount'] as num?)?.toDouble() ?? 0.0;
    final List<dynamic> items = quote['items'] ?? [];
    final String createdAt = quote['createdAt'] ?? '';
    final String formattedDate = createdAt.split('T')[0];

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.02),
            blurRadius: 6,
            offset: const Offset(0, 3),
          )
        ],
      ),
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        childrenPadding: const EdgeInsets.all(16),
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // Left info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: Colors.amber.shade100,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          status,
                          style: TextStyle(
                            color: Colors.amber.shade900,
                            fontWeight: FontWeight.bold,
                            fontSize: 11,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        "Date: $formattedDate",
                        style: const TextStyle(color: Colors.grey, fontSize: 12),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    "Quotation ID: ${quote['id'].toString().substring(0, 8)}...",
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13, color: Colors.black),
                  ),
                ],
              ),
            ),
            // Right Total
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                const Text(
                  "Total Amount",
                  style: TextStyle(color: Colors.grey, fontSize: 11),
                ),
                const SizedBox(height: 2),
                Text(
                  "₹${totalAmount.toStringAsFixed(2)}",
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.green,
                  ),
                ),
              ],
            ),
          ],
        ),
        children: [
          const Divider(),
          const SizedBox(height: 8),
          
          // Pricing Summary Row
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _buildSummaryItem("Subtotal", "₹${subtotal.toStringAsFixed(2)}"),
              _buildSummaryItem("Discount ($discountPercent%)", "₹${discount.toStringAsFixed(2)}"),
              _buildSummaryItem("GST Amount", "₹${gstAmount.toStringAsFixed(2)}"),
            ],
          ),
          const SizedBox(height: 16),
          const Divider(),
          const SizedBox(height: 8),
          
          const Text(
            "Items List",
            style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF334EAC), fontSize: 14),
          ),
          const SizedBox(height: 12),

          // Items Table/Grid
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: items.length,
            separatorBuilder: (context, index) => const Divider(height: 16),
            itemBuilder: (context, index) {
              final item = items[index];
              final category = item['category'] ?? 'N/A';
              final desc = item['description'] ?? 'N/A';
              final w = item['width'] ?? '-';
              final h = item['height'] ?? '-';
              final d = item['depth'] ?? '-';
              final double qty = (item['qty'] as num?)?.toDouble() ?? 0.0;
              final double rate = (item['unitRate'] as num?)?.toDouble() ?? 0.0;
              final double amt = (item['amount'] as num?)?.toDouble() ?? 0.0;
              final String unit = item['unit'] ?? 'SQ.FT.';

              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          category,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.blueAccent),
                        ),
                      ),
                      Text(
                        "₹${amt.toStringAsFixed(2)}",
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.black),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  if (desc != category && desc.isNotEmpty) ...[
                    Text(desc, style: const TextStyle(color: Colors.grey, fontSize: 11)),
                    const SizedBox(height: 4),
                  ],
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        "Dimensions: $w x $h x $d",
                        style: const TextStyle(color: Colors.grey, fontSize: 11),
                      ),
                      Text(
                        "$qty $unit @ ₹${rate.toStringAsFixed(2)}",
                        style: const TextStyle(color: Colors.black54, fontSize: 11),
                      ),
                    ],
                  ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryItem(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 11)),
        const SizedBox(height: 4),
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.black)),
      ],
    );
  }
}
