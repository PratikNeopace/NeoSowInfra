import 'dart:async';
import 'package:flutter/material.dart';
import 'package:neo_sow_infra/screens/splash_screen_two.dart';

class SplashScreenOne extends StatefulWidget {
  const SplashScreenOne({super.key});

  @override
  State<SplashScreenOne> createState() => _SplashScreenOneState();
}

class _SplashScreenOneState extends State<SplashScreenOne> {
  Timer? _timer;
  bool _navigated = false;

  @override
  void initState() {
    super.initState();
    // Auto-navigate to next screen after 3 seconds
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
      MaterialPageRoute(builder: (context) => const SplashScreenTwo()),
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
                      onPressed: _navigateToNextScreen, // Navigate to splash_screen_two on SKIP
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
                        'Create',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Color(0xFF0F6127), // Forest green color
                          fontSize: 32,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text(
                        'Architecture & Interior\nQuotations',
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
                        'Build professional Quotations Quickly\nwith your scope,Items and pricing',
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
                      'lib/assets/splashscreen/splash_screen_one.jpeg',
                      height: 280,
                      fit: BoxFit.contain,
                    ),
                  ),
                  const Spacer(flex: 2),

                  // Bottom Button
                  SizedBox(
                    height: 54,
                    child: ElevatedButton(
                      onPressed: _navigateToNextScreen, // Navigate to splash_screen_two on NEXT
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