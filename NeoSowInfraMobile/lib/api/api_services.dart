import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  static const String baseUrl = 'https://api.neosowinfra.com/api/v1';
   
   // 1. Login Function
   static Future<Map<String, dynamic>?> login(String email, String password) async {
    final url = Uri.parse('$baseUrl/auth/login');

    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email' : email,
          'password' : password,
        }),
      );

      if(response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
         // failed login
         return null;
      }
    } catch(e) {
      print("Login Error: $e");
      return null;
    }
   }

   // 2. Fetch Dashboard data based on Role
   static Future<Map<String, dynamic>?> fetchDashboardData(String token, String role) async {
     // Determine target endpoint based on role
     final endpoint = (role == 'ROLE_SUPER_ADMIN' || role == 'ROLE_ADMIN') ? '/dashboard/super-admin' : '/dashboard/user';
     final url = Uri.parse('$baseUrl$endpoint');

     try {
      final response = await http.get(
        url, 
        headers: {
          'Content-Type' : 'application/json',
          'Authorization' : 'Bearer $token',
        },
      );
      if(response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        print('Dashboard API Error: ${response.statusCode}');
        return null;        
      }
      } catch(e) {
        print("Fetch dashboard error: $e");
        return null;
      }
     }

   // 3. Fetch Customers Function
   static Future<Map<String, dynamic>?> fetchCustomers(String token, {String search = '', int page = 0, int size = 100}) async {
     final url = Uri.parse('$baseUrl/customers?search=$search&page=$page&size=$size');
     try {
       final response = await http.get(
         url,
         headers: {
           'Content-Type': 'application/json',
           'Authorization': 'Bearer $token',
         },
       );
       if (response.statusCode == 200) {
         return jsonDecode(response.body);
       } else {
         print('Fetch Customers API Error: ${response.statusCode}');
         return null;
       }
     } catch (e) {
       print('Fetch Customers Error: $e');
       return null;
     }
   }

   // 4. Fetch Quotations by Customer ID
   static Future<Map<String, dynamic>?> fetchQuotationsByCustomer(String token, String customerId, {int page = 0, int size = 50}) async {
     final url = Uri.parse('$baseUrl/quotations/customer/$customerId?page=$page&size=$size');
     try {
       final response = await http.get(
         url,
         headers: {
           'Content-Type': 'application/json',
           'Authorization': 'Bearer $token',
         },
       );
       if (response.statusCode == 200) {
         return jsonDecode(response.body);
       } else {
         print('Fetch Quotations API Error: ${response.statusCode}');
         return null;
       }
     } catch (e) {
       print('Fetch Quotations Error: $e');
       return null;
     }
   }

   // 5. Fetch Quotation by ID
   static Future<Map<String, dynamic>?> fetchQuotationById(String token, String quotationId) async {
     final url = Uri.parse('$baseUrl/quotations/$quotationId');
     try {
       final response = await http.get(
         url,
         headers: {
           'Content-Type': 'application/json',
           'Authorization': 'Bearer $token',
         },
       );
       if (response.statusCode == 200) {
         return jsonDecode(response.body);
       } else {
         print('Fetch Quotation ID Error: ${response.statusCode}');
         return null;
       }
     } catch (e) {
       print('Fetch Quotation ID Exception: $e');
       return null;
     }
   }
}