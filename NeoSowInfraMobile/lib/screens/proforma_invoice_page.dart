import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../api/api_services.dart';
import 'login_page.dart';
import 'dashboard_page.dart';
import 'quotation_management_page.dart';

class ProformaInvoicePage extends StatefulWidget {
  final String quotationId;
  final String customerPhone;
  final String customerAddress;
  final String workType;

  const ProformaInvoicePage({
    super.key,
    required this.quotationId,
    required this.customerPhone,
    required this.customerAddress,
    required this.workType,
  });

  @override
  State<ProformaInvoicePage> createState() => _ProformaInvoicePageState();
}

class _ProformaInvoicePageState extends State<ProformaInvoicePage> {
  String _token = '';
  bool _isLoading = true;
  Map<String, dynamic>? _quotation;

  @override
  void initState() {
    super.initState();
    _loadSessionAndFetchDetails();
  }

  Future<void> _loadSessionAndFetchDetails() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('accessToken') ?? '';

    setState(() {
      _token = token;
    });

    if (token.isNotEmpty) {
      final data = await ApiService.fetchQuotationById(token, widget.quotationId);
      setState(() {
        _quotation = data;
        _isLoading = false;
      });
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

            // 2. Main Content Card
            Expanded(
              child: _isLoading
                  ? const Center(child: CircularProgressIndicator(color: Colors.white))
                  : _quotation == null
                      ? const Center(
                          child: Text(
                            "Quotation details could not be loaded.",
                            style: TextStyle(color: Colors.white, fontSize: 16),
                          ),
                        )
                      : SingleChildScrollView(
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
                          child: Center(
                            child: Container(
                              constraints: const BoxConstraints(maxWidth: 1000),
                              child: Card(
                                elevation: 6,
                                color: Colors.white,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(16),
                                ),
                                child: Padding(
                                  padding: const EdgeInsets.all(32.0),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      // Top buttons row: Back & Export
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          OutlinedButton.icon(
                                            onPressed: () {
                                              Navigator.pushReplacement(
                                                context,
                                                MaterialPageRoute(builder: (context) => const QuotationManagementPage()),
                                              );
                                            },
                                            icon: const Icon(Icons.arrow_back, size: 16),
                                            label: const Text("Back to Quotations"),
                                            style: OutlinedButton.styleFrom(
                                              foregroundColor: Colors.black,
                                              side: BorderSide(color: Colors.grey.shade400),
                                              shape: RoundedRectangleBorder(
                                                borderRadius: BorderRadius.circular(6),
                                              ),
                                            ),
                                          ),
                                          ElevatedButton.icon(
                                            onPressed: () {},
                                            icon: const Icon(Icons.picture_as_pdf, size: 16),
                                            label: const Text("Export PDF"),
                                            style: ElevatedButton.styleFrom(
                                              backgroundColor: Colors.green,
                                              foregroundColor: Colors.white,
                                              shape: RoundedRectangleBorder(
                                                borderRadius: BorderRadius.circular(6),
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 32),

                                      // Main Invoice Title & Logo Row
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: const [
                                              Text(
                                                "PROFORMA INVOICE",
                                                style: TextStyle(
                                                  fontSize: 28,
                                                  fontWeight: FontWeight.bold,
                                                  color: Color(0xFF1B6DF8),
                                                ),
                                              ),
                                              SizedBox(height: 4),
                                              Text(
                                                "Professional Estimation Sheet",
                                                style: TextStyle(
                                                  color: Colors.grey,
                                                  fontSize: 13,
                                                ),
                                              ),
                                            ],
                                          ),
                                          Column(
                                            crossAxisAlignment: CrossAxisAlignment.end,
                                            children: const [
                                              Text(
                                                "NEOSOW INFRA",
                                                style: TextStyle(
                                                  fontSize: 20,
                                                  fontWeight: FontWeight.bold,
                                                  color: Color(0xFF1B6DF8),
                                                ),
                                              ),
                                              SizedBox(height: 4),
                                              Text(
                                                "support@neosow.com",
                                                style: TextStyle(
                                                  color: Colors.grey,
                                                  fontSize: 12,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 16),
                                      const Divider(),
                                      const SizedBox(height: 16),

                                      // Bill To vs Details Row
                                      _buildBillAndDetailsSection(),
                                      const SizedBox(height: 32),

                                      // Table
                                      _buildItemsTable(),
                                      const SizedBox(height: 24),

                                      // Totals Section
                                      _buildTotalsSection(),
                                      const SizedBox(height: 32),

                                      // Terms & Conditions Callout
                                      _buildTermsCallout(),
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

  Widget _buildBillAndDetailsSection() {
    final String customerName = _quotation?['customerName'] ?? 'Unknown';
    final String invoiceId = _quotation?['id'] ?? '';
    final String unitSetting = _quotation?['projectUnit'] ?? 'Ft Inch';

    final String createdAt = _quotation?['createdAt'] ?? '';
    String dateStr = '';
    if (createdAt.isNotEmpty) {
      final dateTime = DateTime.tryParse(createdAt);
      if (dateTime != null) {
        final hourStr = dateTime.hour == 0 ? 12 : (dateTime.hour > 12 ? dateTime.hour - 12 : dateTime.hour);
        final amPm = dateTime.hour >= 12 ? 'PM' : 'AM';
        dateStr = "${dateTime.month}/${dateTime.day}/${dateTime.year}, $hourStr:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')} $amPm";
      }
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        bool isMobile = constraints.maxWidth < 600;
        
        final billToColumn = Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("BILL TO:", style: TextStyle(color: Color(0xFF1B6DF8), fontWeight: FontWeight.bold, fontSize: 13)),
            const SizedBox(height: 8),
            Text(
              customerName,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.black),
            ),
            const SizedBox(height: 4),
            Text(
              widget.customerPhone,
              style: const TextStyle(color: Colors.black54, fontSize: 13),
            ),
            const SizedBox(height: 4),
            Text(
              widget.customerAddress,
              style: const TextStyle(color: Colors.black54, fontSize: 13),
            ),
          ],
        );

        final detailsColumn = Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Align(
              alignment: Alignment.centerRight,
              child: Text("DETAILS:", style: TextStyle(color: Color(0xFF1B6DF8), fontWeight: FontWeight.bold, fontSize: 13)),
            ),
            const SizedBox(height: 8),
            _buildDetailRow("Proforma Invoice ID:", invoiceId),
            _buildDetailRow("Date:", dateStr),
            _buildDetailRow("Unit Setting:", unitSetting),
            _buildDetailRow("Work Type:", widget.workType),
          ],
        );

        if (isMobile) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              billToColumn,
              const SizedBox(height: 24),
              detailsColumn,
            ],
          );
        } else {
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(flex: 1, child: billToColumn),
              const SizedBox(width: 32),
              Expanded(flex: 1, child: detailsColumn),
            ],
          );
        }
      },
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          Text(label, style: const TextStyle(color: Colors.black54, fontSize: 12)),
          const SizedBox(width: 6),
          Flexible(
            child: Text(
              value,
              style: const TextStyle(color: Colors.black, fontWeight: FontWeight.w600, fontSize: 12),
              textAlign: TextAlign.end,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildItemsTable() {
    final List<dynamic> items = _quotation?['items'] ?? [];

    if (items.isEmpty) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 24.0),
        child: Center(
          child: Text("No items listed in this estimation sheet.", style: TextStyle(color: Colors.grey)),
        ),
      );
    }

    return Scrollbar(
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Container(
          constraints: const BoxConstraints(minWidth: 900),
          child: Table(
            columnWidths: const {
              0: FixedColumnWidth(60),  // S.No
              1: FixedColumnWidth(260), // Description
              2: FixedColumnWidth(70),  // Width
              3: FixedColumnWidth(70),  // Height
              4: FixedColumnWidth(70),  // Depth
              5: FixedColumnWidth(80),  // Unit
              6: FixedColumnWidth(80),  // Qty
              7: FixedColumnWidth(60),  // Nos
              8: FixedColumnWidth(80),  // Total Qty
              9: FixedColumnWidth(90),  // Rate
              10: FixedColumnWidth(110), // Amount
            },
            children: [
              // Header Row
              TableRow(
                decoration: const BoxDecoration(
                  color: Color(0xFFDCE6F5),
                ),
                children: const [
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("S.NO", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("DESCRIPTION", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("WIDTH", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("HEIGHT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("DEPTH", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("UNIT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("QTY", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("NOS", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("TOTAL QTY.", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("RATE", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                  Padding(
                    padding: EdgeInsets.all(10.0),
                    child: Text("AMOUNT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Color(0xFF334EAC))),
                  ),
                ],
              ),

              // Item Rows
              ...List.generate(items.length, (index) {
                final item = items[index];
                final String category = item['category'] ?? '';
                final String desc = item['description'] ?? category;

                final String widthStr = item['width'] ?? '-';
                final String heightStr = item['height'] ?? '-';
                final String depthStr = item['depth'] ?? '-';
                final String unit = item['unit'] ?? '-';
                final double qty = (item['qty'] as num?)?.toDouble() ?? 0.0;
                final double noOfUnit = (item['noOfUnit'] as num?)?.toDouble() ?? 0.0;
                final double totalQty = (item['totalQty'] as num?)?.toDouble() ?? 0.0;
                final double unitRate = (item['unitRate'] as num?)?.toDouble() ?? 0.0;
                final double amount = (item['amount'] as num?)?.toDouble() ?? 0.0;

                return TableRow(
                  decoration: BoxDecoration(
                    border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
                  ),
                  children: [
                    // S.No
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text("${index + 1}", style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Description
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            category,
                            style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black),
                          ),
                          if (desc != category && desc.isNotEmpty) ...[
                            const SizedBox(height: 4),
                            Text(
                              desc,
                              style: const TextStyle(fontSize: 10, color: Colors.grey),
                            ),
                          ],
                        ],
                      ),
                    ),
                    // Width
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(widthStr, style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Height
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(heightStr, style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Depth
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(depthStr, style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Unit
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(unit, style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Qty
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(qty.toStringAsFixed(2), style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Nos
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(noOfUnit.toInt().toString(), style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Total Qty
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text(totalQty.toStringAsFixed(2), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black)),
                    ),
                    // Rate
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text("₹${unitRate.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12, color: Colors.black)),
                    ),
                    // Amount
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
                      child: Text("₹${amount.toStringAsFixed(2)}", style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.black)),
                    ),
                  ],
                );
              }),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTotalsSection() {
    final double subtotal = (_quotation?['subtotal'] as num?)?.toDouble() ?? 0.0;
    final double discount = (_quotation?['discount'] as num?)?.toDouble() ?? 0.0;
    final double discountPercent = (_quotation?['discountPercent'] as num?)?.toDouble() ?? 0.0;
    final double gstAmount = (_quotation?['gstAmount'] as num?)?.toDouble() ?? 0.0;
    final double totalAmount = (_quotation?['totalAmount'] as num?)?.toDouble() ?? 0.0;

    return Align(
      alignment: Alignment.centerRight,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 350),
        child: Column(
          children: [
            _buildTotalBreakdownRow("Subtotal:", "₹${subtotal.toStringAsFixed(2)}"),
            const SizedBox(height: 8),
            _buildTotalBreakdownRow("Discount (${discountPercent.toStringAsFixed(0)}%):", "-₹${discount.toStringAsFixed(2)}", isRed: true),
            const SizedBox(height: 8),
            _buildTotalBreakdownRow("GST (18%):", "₹${gstAmount.toStringAsFixed(2)}"),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFDCE6F5),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    "GRAND TOTAL:",
                    style: TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF334EAC), fontSize: 14),
                  ),
                  Text(
                    "₹${totalAmount.toStringAsFixed(2)}",
                    style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF334EAC), fontSize: 16),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTotalBreakdownRow(String label, String val, {bool isRed = false}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(color: Colors.black54, fontSize: 13)),
        Text(
          val,
          style: TextStyle(
            color: isRed ? Colors.redAccent : Colors.black,
            fontWeight: FontWeight.w600,
            fontSize: 13,
          ),
        ),
      ],
    );
  }

  Widget _buildTermsCallout() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(
        border: Border(
          left: BorderSide(color: Color(0xFF1B6DF8), width: 4),
        ),
      ),
      child: const Text(
        "Terms & Conditions: Valid for 30 days. Confirm via email. Thank you for your business!",
        style: TextStyle(color: Colors.black54, fontSize: 12, fontStyle: FontStyle.italic),
      ),
    );
  }
}
