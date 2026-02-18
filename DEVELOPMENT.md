# Development Guide

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API level 24-34
- Git

### Initial Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd VisualRiskAssessor
```

2. Open the project in Android Studio

3. Sync Gradle files:
   - Android Studio will automatically prompt for Gradle sync
   - Or manually trigger: File → Sync Project with Gradle Files

4. Build the project:
```bash
./gradlew build
```

## Project Architecture

### MVVM Pattern
The app follows the Model-View-ViewModel architectural pattern:

- **Model**: Data models representing hazards, risk assessments, etc.
- **View**: Activities and XML layouts
- **ViewModel**: Business logic and data preparation (to be expanded)

### Package Structure

```
com.hse.visualriskassessor/
├── analysis/           # Image analysis and ML processing
├── model/              # Data models
├── ui/                 # User interface components
│   ├── camera/         # Camera functionality
│   ├── history/        # Assessment history
│   ├── results/        # Results display
│   └── widget/         # Custom UI widgets
└── utils/              # Utility classes
```

## Key Components

### Image Analysis Pipeline

1. **Image Capture/Selection**
   - CameraX for camera capture
   - Photo Picker API for library selection

2. **Image Preprocessing**
   - Resize to optimal dimensions
   - Orientation correction
   - Quality optimization

3. **Hazard Detection**
   - ML Kit image labeling
   - Object detection
   - Custom hazard mapping

4. **Risk Assessment**
   - Likelihood and severity calculation
   - Risk matrix evaluation
   - Overall risk level determination

### Custom Views

#### RiskMatrixView
A custom view that renders the 5×5 risk matrix with:
- Color-coded risk levels
- Hazard position markers
- Interactive display

Usage:
```kotlin
val riskMatrixView = findViewById<RiskMatrixView>(R.id.riskMatrixView)
riskMatrixView.setHazards(listOf(hazard1, hazard2))
```

## Adding New Features

### Adding a New Hazard Type

1. Update `HazardType.kt`:
```kotlin
NEW_HAZARD(
    "New Hazard Name",
    "Description of the hazard"
)
```

2. Update mapping in `HazardDetector.kt`:
```kotlin
lowerLabel.contains("keyword") -> HazardType.NEW_HAZARD
```

3. Add recommendations in `Hazard.kt`:
```kotlin
HazardType.NEW_HAZARD -> listOf(
    "Recommendation 1",
    "Recommendation 2"
)
```

### Adding New UI Screens

1. Create layout XML in `res/layout/`
2. Create Activity/Fragment class in appropriate package
3. Register in `AndroidManifest.xml`
4. Add navigation logic

## Testing

### Running Unit Tests
```bash
./gradlew test
```

### Running Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Camera capture works on physical device
- [ ] Photo selection works from gallery
- [ ] Permissions are requested properly
- [ ] Image analysis completes successfully
- [ ] Risk matrix displays correctly
- [ ] Results can be shared
- [ ] App works on different screen sizes
- [ ] App works on different Android versions (24-34)

## Code Style

### Kotlin Style Guide
Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use camelCase for variables and functions
- Use PascalCase for classes
- Use UPPER_SNAKE_CASE for constants
- Prefer val over var
- Use meaningful variable names

### Example:
```kotlin
class RiskAssessmentEngine(private val context: Context) {
    
    companion object {
        private const val MAX_IMAGE_SIZE = 1024
    }
    
    suspend fun assessImage(uri: Uri): AssessmentResult {
        // Implementation
    }
}
```

## Performance Optimization

### Image Processing
- Resize images before processing
- Use appropriate bitmap compression
- Release resources after use

### Memory Management
- Avoid memory leaks with proper lifecycle management
- Use weak references where appropriate
- Clean up ML Kit detectors when done

### Background Processing
- Use coroutines for heavy operations
- Show loading indicators during processing
- Handle errors gracefully

## Debugging

### Common Issues

#### ML Kit not detecting hazards
- Check image quality and size
- Verify ML Kit dependencies are included
- Test with various sample images

#### Camera not starting
- Verify camera permission is granted
- Check device has camera hardware
- Test on physical device (emulator camera may be limited)

#### Build errors
- Clean and rebuild: `./gradlew clean build`
- Invalidate caches in Android Studio
- Check Gradle and SDK versions

### Logging
Use Android's Log class for debugging:

```kotlin
import android.util.Log

private const val TAG = "MyClass"

Log.d(TAG, "Debug message")
Log.e(TAG, "Error message", exception)
```

## Dependencies Management

### Updating Dependencies
1. Check for updates in `app/build.gradle.kts`
2. Update version numbers
3. Sync Gradle
4. Test thoroughly

### Adding New Dependencies
1. Add to `dependencies` block in `app/build.gradle.kts`
2. Sync Gradle
3. Update ProGuard rules if needed
4. Document in README

## Release Process

### Creating a Release Build

1. Update version in `app/build.gradle.kts`:
```kotlin
versionCode = 2
versionName = "1.1.0"
```

2. Build release APK:
```bash
./gradlew assembleRelease
```

3. Sign the APK (requires keystore)
4. Test release build thoroughly
5. Create release notes

### ProGuard Configuration
ProGuard rules are in `app/proguard-rules.pro`. Add rules for:
- Keep model classes
- Keep ML Kit classes
- Keep third-party library requirements

## Contributing

### Pull Request Process
1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Update documentation
5. Submit PR with clear description

### Commit Message Format
```
feat: Add new hazard type for confined spaces
fix: Camera crash on Android 11 devices
docs: Update installation instructions
refactor: Improve risk calculation algorithm
```

## Resources

### Documentation
- [Android Developer Guide](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [ML Kit Documentation](https://developers.google.com/ml-kit)

### HSE Resources
- [HSE Risk Assessment Guide](https://www.hse.gov.uk/simple-health-safety/risk/)
- [ISO 31000 Risk Management](https://www.iso.org/iso-31000-risk-management.html)

## Support

For questions or issues:
- Check existing GitHub issues
- Create new issue with detailed description
- Include device info, Android version, and logs

---

**Happy Coding!** 🚀
