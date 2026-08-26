import 'package:flutter/material.dart';

/// Centralized color constants for the application.
/// You can update these color hex codes here, and it will apply to all pages that consume them.
class AppColors {
  // Theme main colors
  static const Color primary = Color(0XFF075C10);
  static const Color primaryLight = Color(0XFF075C10);
  static const Color secondary = Color(0xFF1EA896);
  static const Color accent = Color(0xFFB89552);
  static const Color background = Color(0xFFF7F8FA);
  
  // Status colors
  static const Color statusSuccessBg = Color(0xFFE6F7ED);
  static const Color statusSuccessText = Color(0xFF0F6127);
  static const Color statusWarningBg = Color(0xFFFFF5E5);
  static const Color statusWarningText = Color(0xFFE68A00);
  
  // Neutral colors
  static const Color white = Colors.white;
  static const Color border = Color(0xFFE5E5EA);
  static const Color cardBorder = Color(0xFFF2F2F2);
  static const Color textDark = Color(0xFF1D1D1F);
  static const Color textGrey = Color(0xFF4A4A4A);
  static const Color textLightGrey = Color(0xFF8E8E93);
}

/// Centralized font size constants for the application.
/// Modifying these values updates the typography hierarchy across the app.
class AppFontSizes {
  static const double headingLarge = 32.0;
  static const double heading = 24.0;
  static const double titleLarge = 20.0;
  static const double title = 18.0;
  static const double bodyLarge = 16.0;
  static const double bodyMedium = 14.0;
  static const double caption = 12.0;
  static const double overline = 10.0;
}

/// Provider class for theme states. Allows dynamic configuration changes at runtime
/// (e.g. changing text scale factor or light/dark mode properties) and automatically notifies listeners.
class AppThemeProvider extends ChangeNotifier {
  Color _primaryColor = AppColors.primary;
  double _fontSizeScale = 1.0;
  bool _isDarkMode = false;

  Color get primaryColor => _isDarkMode ? Colors.blueGrey : _primaryColor;
  double get fontSizeScale => _fontSizeScale;
  bool get isDarkMode => _isDarkMode;

  // Custom getters that scale font sizes reactively
  double get headingLarge => AppFontSizes.headingLarge * _fontSizeScale;
  double get heading => AppFontSizes.heading * _fontSizeScale;
  double get titleLarge => AppFontSizes.titleLarge * _fontSizeScale;
  double get title => AppFontSizes.title * _fontSizeScale;
  double get bodyLarge => AppFontSizes.bodyLarge * _fontSizeScale;
  double get bodyMedium => AppFontSizes.bodyMedium * _fontSizeScale;
  double get caption => AppFontSizes.caption * _fontSizeScale;

  /// Update primary color at runtime and notify all pages
  void setPrimaryColor(Color color) {
    if (_primaryColor != color) {
      _primaryColor = color;
      notifyListeners();
    }
  }

  /// Change the global font scale factor (useful for accessibility settings)
  void setFontSizeScale(double scale) {
    if (_fontSizeScale != scale) {
      _fontSizeScale = scale;
      notifyListeners();
    }
  }

  /// Toggle dark mode setting
  void toggleDarkMode() {
    _isDarkMode = !_isDarkMode;
    notifyListeners();
  }
}
