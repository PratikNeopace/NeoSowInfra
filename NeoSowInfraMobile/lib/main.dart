import 'package:flutter/material.dart';
import 'package:neo_sow_infra/screens/logo_screen.dart';
import 'package:neo_sow_infra/theme/app_theme.dart';

import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final prefs = await SharedPreferences.getInstance();
  final String? token = prefs.getString('accessToken');
  
  runApp(
    ChangeNotifierProvider(
      create: (_) => AppThemeProvider(),
      child: MainApp(isLoggedIn: token != null && token.isNotEmpty),
    ),
  );
}

class MainApp extends StatelessWidget {
  final bool isLoggedIn;
  const MainApp({super.key, required this.isLoggedIn});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NeosowInfra',
      debugShowCheckedModeBanner: false,
      theme: ThemeData( 
        useMaterial3: true, 
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0XFF0F4C81),
        ),
      ),
      home: LogoScreen(),
    );
  }
}

