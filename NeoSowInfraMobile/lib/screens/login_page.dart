import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:neo_sow_infra/screens/dashboard_page.dart';
import 'package:neo_sow_infra/theme/app_theme.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../api/api_services.dart';


class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _keepMeSignedIn = false;

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isLoading = true;
    });

    final response = await ApiService.login(
      _emailController.text.trim(),
      _passwordController.text,
    );
    setState(() {
      _isLoading = false;
    });

    if (response != null) {
      // Save credentials in Shared Preferences
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('accessToken', response['accessToken'] ?? '');
      await prefs.setString('refreshToken', response['refreshToken'] ?? '');
      await prefs.setString('email', response['email'] ?? '');

      // Get role from roles array
      final List<dynamic> roles = response['roles'] ?? [];
      final userRole = roles.isNotEmpty ? roles.first.toString() : 'ROLE_USER';

      await prefs.setString('role', userRole);

      if (mounted) {
        // Navigate to Dashboard and clear route history
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(builder: (context) => const DashboardPage()),
        );
      }
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Login failed. Please check your credentials.'),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final themeProvider = Provider.of<AppThemeProvider>(context);

    return Scaffold(
      backgroundColor: AppColors.background, // Centralized background color
      body: SafeArea(
        child: Stack(
          children: [
            // Background corner sparkle image
            Positioned(
              top: 0,
              right: 0,
              width: 120,
              height: 200,
              child: Opacity(
                opacity: 0.8,
                child: Image.asset(
                  'lib/assets/splashscreen/corner_image.jpg',
                  fit: BoxFit.fill,
                ),
              ),
            ),
            Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
                child: Form(
                  key: _formKey,
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Custom Built Logo
                      _buildLogo(),
                      const SizedBox(height: 24),

                      // Welcome Header
                      Text(
                        'Welcome Back',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: themeProvider.primaryColor, // Forest green/primary color from provider
                          fontSize: themeProvider.heading, // Scalable font size
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Log in to Manage and edit\nyour Quotations',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: AppColors.textLightGrey,
                          fontSize: themeProvider.bodyMedium, // Scalable font size
                          height: 1.3,
                        ),
                      ),
                      const SizedBox(height: 32),

                      // Input Fields Card
                      Container(
                        padding: const EdgeInsets.all(24.0),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFAFAFA),
                          borderRadius: BorderRadius.circular(28),
                          border: Border.all(color: AppColors.cardBorder),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.02),
                              blurRadius: 16,
                              offset: const Offset(0, 8),
                            ),
                          ],
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            // Email Label
                            Text(
                              'WORK EMAIL',
                              style: TextStyle(
                                color: AppColors.textGrey,
                                fontWeight: FontWeight.bold,
                                fontSize: themeProvider.caption - 1.0, // Scalable small label
                                letterSpacing: 0.5,
                              ),
                            ),
                            const SizedBox(height: 8),
                            // Email input field
                            TextFormField(
                              controller: _emailController,
                              keyboardType: TextInputType.emailAddress,
                              style: TextStyle(fontSize: themeProvider.bodyMedium),
                              decoration: InputDecoration(
                                hintText: 'admin@enterprise.com',
                                hintStyle: const TextStyle(color: Color(0xFFC7C7CC)),
                                prefixIcon: const Icon(Icons.mail_outline_rounded, color: AppColors.textLightGrey, size: 20),
                                filled: true,
                                fillColor: Colors.white,
                                contentPadding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: const BorderSide(color: AppColors.border),
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: BorderSide(color: themeProvider.primaryColor, width: 1.5),
                                ),
                                errorBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: const BorderSide(color: Colors.redAccent),
                                ),
                                focusedErrorBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: const BorderSide(color: Colors.redAccent, width: 1.5),
                                ),
                              ),
                              validator: (value) {
                                  if (value == null || value.trim().isEmpty) {
                                    return 'Please enter your email';
                                  }
                                  return null;
                              },
                            ),
                            const SizedBox(height: 20),

                            // Password Label
                            Text(
                              'PASSWORD',
                              style: TextStyle(
                                color: AppColors.textGrey,
                                fontWeight: FontWeight.bold,
                                fontSize: themeProvider.caption - 1.0,
                                letterSpacing: 0.5,
                              ),
                            ),
                            const SizedBox(height: 8),
                            // Password input field
                            TextFormField(
                              controller: _passwordController,
                              obscureText: _obscurePassword,
                              style: TextStyle(fontSize: themeProvider.bodyMedium),
                              decoration: InputDecoration(
                                hintText: '••••••••',
                                hintStyle: const TextStyle(color: Color(0xFFC7C7CC)),
                                prefixIcon: const Icon(Icons.lock_outline_rounded, color: AppColors.textLightGrey, size: 20),
                                suffixIcon: IconButton(
                                  icon: Icon(
                                    _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                                    color: AppColors.textLightGrey,
                                    size: 20,
                                  ),
                                  onPressed: () {
                                    setState(() {
                                      _obscurePassword = !_obscurePassword;
                                    });
                                  },
                                ),
                                filled: true,
                                fillColor: Colors.white,
                                contentPadding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
                                enabledBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: const BorderSide(color: AppColors.border),
                                ),
                                focusedBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: BorderSide(color: themeProvider.primaryColor, width: 1.5),
                                ),
                                errorBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: const BorderSide(color: Colors.redAccent),
                                ),
                                focusedErrorBorder: OutlineInputBorder(
                                  borderRadius: BorderRadius.circular(12),
                                  borderSide: const BorderSide(color: Colors.redAccent, width: 1.5),
                                ),
                              ),
                              validator: (value) {
                                if (value == null || value.isEmpty) {
                                  return 'Please enter your password';
                                }
                                return null;
                              },
                            ),
                            const SizedBox(height: 8),

                            // Forgot Password link
                            Align(
                              alignment: Alignment.topRight,
                              child: GestureDetector(
                                onTap: () {
                                  // Action for forgot password
                                },
                                child: Text(
                                  'Forgot password?',
                                  style: TextStyle(
                                    color: themeProvider.primaryColor,
                                    fontWeight: FontWeight.bold,
                                    fontSize: themeProvider.caption,
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(height: 16),

                            // Keep me signed in Checkbox
                            Row(
                              children: [
                                SizedBox(
                                  width: 24,
                                  height: 24,
                                  child: Checkbox(
                                    value: _keepMeSignedIn,
                                    activeColor: themeProvider.primaryColor,
                                    onChanged: (val) {
                                      setState(() {
                                        _keepMeSignedIn = val ?? false;
                                      });
                                    },
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(4),
                                    ),
                                    side: const BorderSide(color: Color(0xFFC7C7CC)),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  'Keep me signed in',
                                  style: TextStyle(
                                    color: AppColors.textLightGrey,
                                    fontSize: themeProvider.bodyMedium - 1.0,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 24),

                            // Sign In Button
                            SizedBox(
                              height: 52,
                              child: ElevatedButton(
                                onPressed: _isLoading ? null : _handleLogin,
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppColors.accent, // Centralized Accent gold shade
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(16),
                                  ),
                                  elevation: 0,
                                ),
                                child: _isLoading
                                    ? const SizedBox(
                                        height: 20,
                                        width: 20,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2,
                                          color: Colors.white,
                                        ),
                                      )
                                    : Text(
                                        'SIGN IN',
                                        style: TextStyle(
                                          color: Colors.white,
                                          fontSize: themeProvider.bodyMedium,
                                          fontWeight: FontWeight.bold,
                                          letterSpacing: 1.0,
                                        ),
                                      ),
                              ),
                            ),
                            const SizedBox(height: 16),

                            // Sign Up footer
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  'No account Yet ? ',
                                  style: TextStyle(
                                    color: AppColors.textLightGrey,
                                    fontSize: themeProvider.bodyMedium - 1.0,
                                  ),
                                ),
                                GestureDetector(
                                  onTap: () {
                                    // Action to Create One
                                  },
                                  child: Text(
                                    'Create One',
                                    style: TextStyle(
                                      color: themeProvider.primaryColor,
                                      fontWeight: FontWeight.bold,
                                      fontSize: themeProvider.bodyMedium - 1.0,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }


  Widget _buildLogo() {
    return Center(
      child: SvgPicture.asset(
        'lib/assets/splashscreen/logo.svg',
        height: 75,
        fit: BoxFit.contain,
      ),
    );
  }
}

class LogoNPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final greenPaint = Paint()
      ..color = const Color(0xFF0F6127)
      ..style = PaintingStyle.fill;

    final goldPaint = Paint()
      ..color = const Color(0xFFB89552)
      ..style = PaintingStyle.fill;

    // Draw left green pillar
    final pathLeft = Path()
      ..moveTo(0, size.height * 0.15)
      ..lineTo(size.width * 0.28, 0)
      ..lineTo(size.width * 0.28, size.height)
      ..lineTo(0, size.height * 0.85)
      ..close();
    canvas.drawPath(pathLeft, greenPaint);

    // Draw diagonal green connection
    final pathDiagonal = Path()
      ..moveTo(size.width * 0.28, size.height * 0.1)
      ..lineTo(size.width * 0.72, size.height * 0.9)
      ..lineTo(size.width * 0.72, size.height)
      ..lineTo(size.width * 0.28, size.height * 0.2)
      ..close();
    canvas.drawPath(pathDiagonal, greenPaint);

    // Draw right gold pillar
    final pathRight = Path()
      ..moveTo(size.width * 0.72, 0)
      ..lineTo(size.width, size.height * 0.15)
      ..lineTo(size.width, size.height * 0.85)
      ..lineTo(size.width * 0.72, size.height)
      ..close();
    canvas.drawPath(pathRight, goldPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}