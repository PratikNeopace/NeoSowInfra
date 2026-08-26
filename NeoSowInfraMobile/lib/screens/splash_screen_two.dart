import 'dart:async';
import 'package:flutter/material.dart';
import 'package:neo_sow_infra/screens/login_page.dart';
import 'package:neo_sow_infra/screens/splash_screen_one.dart';
import 'package:neo_sow_infra/screens/splash_screen_three.dart';

class SplashScreenTwo extends StatefulWidget {
  const SplashScreenTwo({super.key});

  @override
  State<SplashScreenTwo> createState() => _SplashScreenTwoState();
}

class _SplashScreenTwoState extends State<SplashScreenTwo> {
  Timer? _timer;
  bool _navigated = false;

  @override
  void initState() {
    super.initState();
    _timer = Timer(const Duration(seconds: 3), () {
      _navigateToNextScreen();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _navigateToNextScreen() {
    if (!mounted || _navigated) return;
    _navigated = true;
    _timer?.cancel();
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (context) => const SplashScreenThree()),
    );
  }

  void _navigateToBackScreen() {
    if (!mounted || _navigated) return;
    _navigated = true;
    _timer?.cancel();
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (context) => const SplashScreenOne()),
    );
  }

  void _navigateToLoginScreen() {
    if (!mounted || _navigated) return;
    _navigated = true;
    _timer?.cancel();
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(builder: (context) => const LoginPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF9F9F9), // Light background hue
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
            
            // Main content
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 10),
                  // Top Bar with Skip Button
                  Align(
                    alignment: Alignment.topRight,
                    child: TextButton(
                      onPressed: _navigateToLoginScreen,
                      child: const Text(
                        'SKIP',
                        style: TextStyle(
                          color: Color(0xFF8E8E93),
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                          decoration: TextDecoration.underline,
                        ),
                      ),
                    ),
                  ),
                  const Spacer(flex: 1),

                  // Heading Section
                  Column(
                    children: const [
                      Text(
                        'Edit & Customize',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Color(0xFF0F6127), // Forest green color
                          fontSize: 32,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text(
                        'With Ease',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Colors.black,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          height: 1.2,
                        ),
                      ),
                      SizedBox(height: 16),
                      Text(
                        'Update Items, Quantaties, rate and\nterms, Make every quotation fit your Project',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Color(0xFF8E8E93),
                          fontSize: 15,
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                  const Spacer(flex: 2),

                  // Illustration Area
                  Center(
                    child: Image.asset(
                      'lib/assets/splashscreen/splash_screen_two.jpeg',
                      height: 280,
                      fit: BoxFit.contain,
                      errorBuilder: (context, error, stackTrace) {
                        // Fallback in case screen2.jpg is not yet added in the folder
                        return Container(
                          height: 280,
                          decoration: BoxDecoration(
                            color: Colors.grey[200],
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: const Center(
                            child: Icon(
                              Icons.edit_note_rounded,
                              size: 80,
                              color: Color(0xFFB89552),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                  const Spacer(flex: 2),

                  // Bottom Action Buttons (Back & Next)
                  Row(
                    children: [
                      // Back Button
                      SizedBox(
                        width: 54,
                        height: 54,
                        child: ElevatedButton(
                          onPressed: _navigateToBackScreen,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFB89552), // Brownish gold shade
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            padding: EdgeInsets.zero,
                            elevation: 0,
                          ),
                          child: const Icon(
                            Icons.arrow_back_ios_new,
                            color: Colors.white,
                            size: 20,
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      // Next Button
                      Expanded(
                        child: SizedBox(
                          height: 54,
                          child: ElevatedButton(
                            onPressed: _navigateToNextScreen,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFFB89552), // Brownish gold shade
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(16),
                              ),
                              elevation: 0,
                            ),
                            child: const Text(
                              'NEXT',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 14,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 1.0,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
