# VisualRiskAssessor

A comprehensive Android application for HSE (Health, Safety, and Environment) risk assessment that allows users to take pictures or select images from their photo library, analyze them for occupational hazards, and generate immediate risk assessment reports with visualizations.

## Features

### 📸 Dual Image Input
- **Camera Capture**: Take photos directly within the app using the advanced CameraX API
  - Flash control (Off/On/Auto)
  - Front/back camera switching
  - High-quality image capture
- **Photo Library**: Select existing images from your device's photo gallery
  - Compatible with Android 13+ Photo Picker API
  - Fallback support for older Android versions
  - Automatic image orientation correction

### 🔍 AI-Powered Hazard Detection
- **ML Kit Integration**: Real-time image analysis using Google's ML Kit
- **Computer Vision**: Advanced object detection and image labeling
- **Hazard Classification**: Identifies multiple hazard types including:
  - Slip, Trip & Fall hazards
  - Electrical hazards
  - Chemical exposures
  - Fire hazards
  - Machinery risks
  - Working at height
  - Ergonomic issues
  - Missing PPE (Personal Protective Equipment)
  - Struck-by object risks
  - Confined space hazards

### 📊 HSE Risk Assessment
- **5×5 Risk Matrix**: Industry-standard likelihood × severity matrix
- **Risk Levels**:
  - Low Risk (1-4)
  - Medium Risk (5-9)
  - High Risk (10-15)
  - Very High Risk (16-20)
  - Extreme Risk (20+)
- **Automated Calculations**: Instant risk scoring based on detected hazards
- **Visual Risk Matrix**: Interactive color-coded risk visualization

### 📈 Comprehensive Reports
- **Instant Results**: Real-time analysis and report generation
- **Detailed Hazard Information**: Each hazard includes:
  - Hazard type and description
  - Likelihood and severity ratings
  - Risk level classification
  - Confidence scores
- **Actionable Recommendations**: Specific control measures for each hazard type
- **Report Sharing**: Export and share assessments via text or other apps
- **Report Storage**: Save assessments for future reference

### 🎨 Professional UI/UX
- **Material Design 3**: Modern, professional interface
- **Intuitive Navigation**: Easy-to-use dual-input design
- **Loading States**: Clear progress indicators during analysis
- **Risk Color Coding**: Visual risk level identification
- **Responsive Layout**: Optimized for various screen sizes

## Technical Stack

### Android Platform
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Language**: Kotlin 1.9.20
- **Build Tool**: Gradle 8.2 with Kotlin DSL

### Core Libraries
- **AndroidX Core**: Latest AndroidX components
- **Material Components**: Material Design 3 UI elements
- **CameraX**: Modern camera API for image capture
- **ML Kit**: On-device machine learning for image analysis
- **TensorFlow Lite**: Machine learning inference
- **Coroutines**: Asynchronous programming
- **Coil**: Efficient image loading and caching
- **MPAndroidChart**: Data visualization (optional for future enhancements)

### Architecture
- **MVVM Pattern**: Separation of concerns
- **Coroutines**: Asynchronous image processing
- **Repository Pattern**: Data management abstraction
- **Clean Architecture**: Modular, testable code

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/hse/visualriskassessor/
│   │   │   ├── analysis/
│   │   │   │   ├── HazardDetector.kt          # ML-based hazard detection
│   │   │   │   └── RiskAssessmentEngine.kt    # Risk calculation engine
│   │   │   ├── model/
│   │   │   │   ├── AssessmentResult.kt        # Assessment data model
│   │   │   │   ├── Hazard.kt                  # Hazard data model
│   │   │   │   ├── HazardType.kt              # Hazard type enumeration
│   │   │   │   └── RiskLevel.kt               # Risk level enumeration
│   │   │   ├── ui/
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraActivity.kt      # Camera capture interface
│   │   │   │   ├── history/
│   │   │   │   │   └── HistoryActivity.kt     # Assessment history
│   │   │   │   ├── results/
│   │   │   │   │   ├── ResultsActivity.kt     # Results display
│   │   │   │   │   └── HazardAdapter.kt       # Hazard list adapter
│   │   │   │   ├── widget/
│   │   │   │   │   └── RiskMatrixView.kt      # Custom risk matrix view
│   │   │   │   └── MainActivity.kt            # Main app entry point
│   │   │   ├── utils/
│   │   │   │   ├── ImageUtils.kt              # Image processing utilities
│   │   │   │   └── PermissionManager.kt       # Permission handling
│   │   │   └── HSEApplication.kt              # Application class
│   │   ├── res/
│   │   │   ├── layout/                        # XML layouts
│   │   │   ├── values/                        # Strings, colors, themes
│   │   │   ├── drawable/                      # Vector graphics
│   │   │   └── xml/                           # Configuration files
│   │   └── AndroidManifest.xml
│   ├── test/                                   # Unit tests
│   └── androidTest/                            # Instrumentation tests
└── build.gradle.kts                            # App-level Gradle config
```

## Building the App

### Prerequisites
- **Android Studio**: Arctic Fox or later
- **JDK**: Java 17
- **Android SDK**: API 34
- **Gradle**: 8.2 (included via wrapper)

### Build Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd VisualRiskAssessor
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory
   - Wait for Gradle sync to complete

3. **Build the app**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on device/emulator**:
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with code optimization and minification

## Permissions

The app requires the following permissions:

- **CAMERA**: Required for capturing workplace photos
- **READ_MEDIA_IMAGES** (Android 13+): Required for selecting images from photo library
- **READ_EXTERNAL_STORAGE** (Android 12 and below): Required for selecting images from photo library

All permissions are requested at runtime with clear explanations to users.

## Usage Guide

### Taking a Photo
1. Open the app
2. Tap "Take Photo"
3. Grant camera permission if prompted
4. Point camera at workplace area
5. Tap capture button
6. Review results and recommendations

### Selecting from Library
1. Open the app
2. Tap "Choose from Library"
3. Grant storage permission if prompted
4. Select an image from your photo gallery
5. Review results and recommendations

### Understanding Results
- **Overall Risk Level**: Highest risk classification based on all detected hazards
- **Hazards List**: Detailed breakdown of each identified hazard
- **Risk Matrix**: Visual representation of risk levels
- **Recommendations**: Specific actions to mitigate identified risks

## Risk Assessment Methodology

The app uses the HSE (Health and Safety Executive) standard risk assessment methodology:

### Risk Calculation
**Risk Score = Likelihood × Severity**

### Likelihood Levels (1-5)
1. **Rare**: May occur only in exceptional circumstances
2. **Unlikely**: Could occur at some time
3. **Possible**: Might occur at some time
4. **Likely**: Will probably occur in most circumstances
5. **Certain**: Expected to occur in most circumstances

### Severity Levels (1-5)
1. **Negligible**: Minor injury, no time off work
2. **Minor**: First aid needed, short absence
3. **Moderate**: Medical treatment required, absence up to 3 days
4. **Major**: Serious injury, long-term absence, permanent disability
5. **Catastrophic**: Fatality or multiple serious injuries

### Risk Matrix
| L/S | 1 | 2 | 3 | 4 | 5 |
|-----|---|---|---|---|---|
| 5   | 5 | 10| 15| 20| 25|
| 4   | 4 | 8 | 12| 16| 20|
| 3   | 3 | 6 | 9 | 12| 15|
| 2   | 2 | 4 | 6 | 8 | 10|
| 1   | 1 | 2 | 3 | 4 | 5 |

## Future Enhancements

- [ ] Cloud storage and synchronization
- [ ] Multi-language support
- [ ] Custom hazard type definitions
- [ ] PDF report generation
- [ ] Team collaboration features
- [ ] Historical trend analysis
- [ ] Offline mode improvements
- [ ] Enhanced ML models with custom training
- [ ] Integration with workplace safety management systems
- [ ] Augmented reality hazard overlay

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
- Model classes: Unit tested
- Risk calculation: Unit tested
- Image processing: Unit tested
- UI components: Instrumentation tested

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Submit a pull request

## Disclaimer

**Important**: This tool provides guidance only and should not replace professional safety assessments. Always follow your organization's safety procedures and consult qualified safety professionals for comprehensive risk assessments.

The app's AI-based hazard detection is designed to assist safety professionals but may not identify all hazards. Users should:
- Conduct thorough manual inspections
- Follow established safety protocols
- Seek expert advice for complex situations
- Use the app as a supplementary tool, not a replacement for professional judgment

## License

MIT License

Copyright (c) 2026 TKSharma

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact the development team

## Acknowledgments

- Google ML Kit for image analysis capabilities
- Android Open Source Project
- Material Design team at Google
- HSE (Health and Safety Executive) for risk assessment methodologies

---

**Version**: 1.0.0  
**Last Updated**: February 2026  
**Platform**: Android 7.0+
