# VisualRiskAssessor - Technical Specification

## 1. System Overview

### 1.1 Purpose
VisualRiskAssessor is an Android mobile application designed to perform real-time Health, Safety, and Environment (HSE) risk assessments using computer vision and machine learning technologies.

### 1.2 Target Platform
- **Platform**: Android
- **Minimum API**: 24 (Android 7.0 Nougat)
- **Target API**: 34 (Android 14)
- **Device Types**: Smartphones and tablets
- **Orientation**: Portrait (primary)

### 1.3 Key Technologies
- Kotlin 1.9.20
- Android Gradle Plugin 8.2.0
- CameraX 1.3.1
- Google ML Kit 17.0.8
- TensorFlow Lite 2.14.0
- Material Design 3

## 2. Architecture

### 2.1 Architectural Pattern
Modified MVVM (Model-View-ViewModel) pattern:
- **Model**: Data classes representing business entities
- **View**: Activities and XML layouts
- **ViewModel**: (Future enhancement - currently logic in Activities)

### 2.2 Package Structure
```
com.hse.visualriskassessor/
├── analysis/           # Business logic for image analysis
├── model/              # Data models
├── ui/                 # User interface components
│   ├── camera/         # Camera-specific UI
│   ├── history/        # History management UI
│   ├── results/        # Results display UI
│   └── widget/         # Custom UI components
└── utils/              # Utility classes
```

### 2.3 Component Diagram
```
┌─────────────────────────────────────────────────┐
│                  Presentation Layer              │
│  ┌──────────┐ ┌──────────┐ ┌────────────────┐  │
│  │  Main    │ │  Camera  │ │    Results     │  │
│  │ Activity │ │ Activity │ │   Activity     │  │
│  └──────────┘ └──────────┘ └────────────────┘  │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│              Business Logic Layer               │
│  ┌──────────────────────┐  ┌─────────────────┐ │
│  │ RiskAssessmentEngine │  │ HazardDetector  │ │
│  └──────────────────────┘  └─────────────────┘ │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                 Data Layer                      │
│  ┌──────────┐ ┌─────────┐ ┌──────────────────┐ │
│  │   Model  │ │ ML Kit  │ │  Image Storage   │ │
│  └──────────┘ └─────────┘ └──────────────────┘ │
└─────────────────────────────────────────────────┘
```

## 3. Data Models

### 3.1 RiskLevel
```kotlin
enum class RiskLevel(val displayName: String, val colorRes: Int, val score: Int)
```
**Values**: LOW, MEDIUM, HIGH, VERY_HIGH, EXTREME

**Methods**:
- `fromScore(score: Int): RiskLevel`
- `calculate(likelihood: Int, severity: Int): RiskLevel`

### 3.2 HazardType
```kotlin
enum class HazardType(val displayName: String, val description: String)
```
**Supported Types** (11 total):
1. SLIP_TRIP_FALL
2. ELECTRICAL
3. CHEMICAL
4. FIRE
5. MACHINERY
6. HEIGHT
7. ERGONOMIC
8. PPE_MISSING
9. STRUCK_BY
10. CONFINED_SPACE
11. OTHER

### 3.3 Hazard
```kotlin
data class Hazard(
    val type: HazardType,
    val likelihood: Int,        // 1-5
    val severity: Int,          // 1-5
    val confidence: Float,      // 0.0-1.0
    val location: String?,
    val details: String?
)
```

**Computed Properties**:
- `riskLevel: RiskLevel`
- `riskScore: Int`

**Methods**:
- `getRecommendations(): List<String>`

### 3.4 AssessmentResult
```kotlin
data class AssessmentResult(
    val id: String,
    val timestamp: Date,
    val imagePath: String,
    val hazards: List<Hazard>,
    val overallRiskLevel: RiskLevel,
    val analysisTimeMs: Long
)
```

**Computed Properties**:
- `hasHazards: Boolean`
- `highestRiskScore: Int`

**Methods**:
- `getAllRecommendations(): List<String>`
- `getSummary(): String`
- `getHazardsByRiskLevel(): Map<RiskLevel, List<Hazard>>`

## 4. Core Components

### 4.1 HazardDetector

**Purpose**: Performs ML-based hazard detection on images

**Key Methods**:
```kotlin
suspend fun analyzeImage(bitmap: Bitmap): List<Hazard>
```

**ML Integration**:
- Google ML Kit Image Labeling
- Google ML Kit Object Detection
- Custom hazard type mapping
- Confidence threshold: 0.5

**Process**:
1. Convert image to ML Kit InputImage
2. Run image labeling
3. Run object detection
4. Map labels to hazard types
5. Calculate likelihood and severity
6. Return hazard list

### 4.2 RiskAssessmentEngine

**Purpose**: Orchestrates the complete assessment process

**Key Methods**:
```kotlin
suspend fun assessImage(uri: Uri): AssessmentResult
suspend fun assessImage(bitmap: Bitmap): AssessmentResult
```

**Process**:
1. Load/preprocess image
2. Resize to optimal size (max 1024px)
3. Save processed image
4. Run hazard detection
5. Calculate overall risk
6. Return AssessmentResult

**Risk Calculation Algorithm**:
```kotlin
fun calculateOverallRisk(hazards: List<Hazard>): RiskLevel {
    // Priority 1: Any extreme hazard = EXTREME
    if (extremeCount > 0 || maxRiskScore >= 20) return EXTREME
    
    // Priority 2: Multiple very high = VERY_HIGH
    if (veryHighCount >= 2 || maxRiskScore >= 16) return VERY_HIGH
    
    // Priority 3: High hazards or high average
    if (veryHighCount >= 1 || highCount >= 2 || maxRiskScore >= 10) return HIGH
    
    // Priority 4: Any high or medium average
    if (highCount >= 1 || avgRiskScore >= 6) return MEDIUM
    
    // Default: LOW
    return LOW
}
```

### 4.3 PermissionManager

**Purpose**: Handles runtime permissions

**Permissions Managed**:
- `CAMERA` (all Android versions)
- `READ_MEDIA_IMAGES` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 and below)

**Key Methods**:
```kotlin
fun hasCameraPermission(): Boolean
fun hasStoragePermission(): Boolean
fun requestCameraPermission(launcher: ActivityResultLauncher<String>)
fun requestStoragePermission(launcher: ActivityResultLauncher<Array<String>>)
fun openAppSettings()
```

### 4.4 ImageUtils

**Purpose**: Image processing and manipulation utilities

**Key Methods**:
```kotlin
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap?
fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap
fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap
fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File?
```

**Features**:
- EXIF orientation correction
- Memory-efficient loading
- Quality optimization
- File I/O operations

## 5. User Interface

### 5.1 Activity Flow

```
MainActivity (Entry Point)
    ├─> Take Photo
    │   └─> CameraActivity
    │       └─> ResultsActivity
    │
    ├─> Choose Photo
    │   └─> Photo Picker
    │       └─> ResultsActivity
    │
    ├─> View History
    │   └─> HistoryActivity
    │
    └─> About
        └─> Dialog
```

### 5.2 MainActivity

**Purpose**: Main application entry point

**UI Components**:
- Header card with app branding
- "Take Photo" button (primary)
- "Choose from Library" button (secondary)
- Quick access cards (History, About)
- Information card

**Key Features**:
- Permission request handling
- Activity Result API integration
- Photo picker launcher
- Alert dialogs for permissions

### 5.3 CameraActivity

**Purpose**: Camera capture interface

**UI Components**:
- Full-screen camera preview (PreviewView)
- Capture button (FAB)
- Flash toggle button
- Camera switch button
- Close button
- Instructions overlay

**CameraX Integration**:
```kotlin
// Use cases bound to lifecycle
- Preview
- ImageCapture
- CameraSelector (front/back)
```

**Features**:
- Flash modes: Off, On, Auto
- Front/back camera switching
- High-quality capture mode
- Automatic lifecycle management

### 5.4 ResultsActivity

**Purpose**: Display assessment results

**UI Components**:
- Toolbar with back navigation
- Analyzed image display
- Overall risk level card
- Hazards RecyclerView
- Risk matrix visualization
- Recommendations card
- Action buttons (Save, Share, New)
- Loading overlay

**Layout Structure**:
- CoordinatorLayout (root)
- AppBarLayout with Toolbar
- NestedScrollView (scrollable content)
- Multiple MaterialCardViews
- Custom RiskMatrixView

### 5.5 RiskMatrixView (Custom Component)

**Purpose**: Visual representation of 5×5 risk matrix

**Features**:
- Custom Canvas drawing
- Color-coded risk levels
- Hazard position markers
- Axis labels
- Responsive sizing

**Drawing Elements**:
```kotlin
- Cell backgrounds (color-coded)
- Cell borders
- Risk scores (text)
- Hazard markers (circles)
- Axis labels
```

## 6. Data Flow

### 6.1 Image Capture Flow
```
User Action: Take Photo
    ↓
Request Camera Permission (if needed)
    ↓
CameraActivity launches
    ↓
CameraX captures image
    ↓
Save to temp file
    ↓
Navigate to ResultsActivity with URI
```

### 6.2 Image Selection Flow
```
User Action: Choose from Library
    ↓
Request Storage Permission (if needed)
    ↓
Launch Photo Picker
    ↓
User selects image
    ↓
Navigate to ResultsActivity with URI
```

### 6.3 Analysis Flow
```
ResultsActivity receives URI
    ↓
Display loading overlay
    ↓
Load bitmap from URI
    ↓
RiskAssessmentEngine.assessImage()
    ├─> Preprocess image (resize, optimize)
    ├─> Save to persistent storage
    ├─> HazardDetector.analyzeImage()
    │   ├─> ML Kit Image Labeling
    │   ├─> ML Kit Object Detection
    │   └─> Map to hazard types
    └─> Calculate overall risk
    ↓
Return AssessmentResult
    ↓
Display results
    ↓
Hide loading overlay
```

## 7. Risk Assessment Methodology

### 7.1 Risk Matrix (5×5)

| L/S | 1  | 2  | 3  | 4  | 5  |
|-----|----|----|----|----|----|
| 5   | 5  | 10 | 15 | 20 | 25 |
| 4   | 4  | 8  | 12 | 16 | 20 |
| 3   | 3  | 6  | 9  | 12 | 15 |
| 2   | 2  | 4  | 6  | 8  | 10 |
| 1   | 1  | 2  | 3  | 4  | 5  |

### 7.2 Likelihood Scale (1-5)
1. **Rare**: May occur only in exceptional circumstances
2. **Unlikely**: Could occur at some time
3. **Possible**: Might occur at some time
4. **Likely**: Will probably occur in most circumstances
5. **Certain**: Expected to occur in most circumstances

### 7.3 Severity Scale (1-5)
1. **Negligible**: Minor injury, no time off work
2. **Minor**: First aid needed, short absence
3. **Moderate**: Medical treatment required, absence up to 3 days
4. **Major**: Serious injury, long-term absence, permanent disability
5. **Catastrophic**: Fatality or multiple serious injuries

### 7.4 Risk Level Thresholds
- **Low**: Score 1-4
- **Medium**: Score 5-9
- **High**: Score 10-15
- **Very High**: Score 16-20
- **Extreme**: Score 20+

### 7.5 Control Measures
Each hazard type includes 5+ specific control measures based on:
- HSE guidance
- ISO 31000 principles
- Industry best practices
- Hierarchy of controls (elimination → PPE)

## 8. Performance Considerations

### 8.1 Image Processing
- **Max image dimensions**: 1024×1024 pixels
- **Compression quality**: 90%
- **Format**: JPEG
- **Memory management**: Bitmap recycling
- **Processing**: Background coroutines (Dispatchers.Default)

### 8.2 ML Inference
- **On-device processing**: No cloud dependency
- **Model loading**: Lazy initialization
- **Resource cleanup**: Detector release on destroy
- **Timeout**: None (UI shows progress)

### 8.3 UI Responsiveness
- **Async operations**: Kotlin coroutines
- **Loading states**: Progress indicators
- **Smooth scrolling**: NestedScrollView
- **Efficient lists**: RecyclerView with ViewHolder

## 9. Security & Privacy

### 9.1 Data Storage
- **Local only**: All processing on-device
- **No cloud upload**: Images stay on device
- **Permissions**: Minimal required permissions
- **File provider**: Secure file sharing

### 9.2 Privacy Compliance
- **No tracking**: No analytics (currently)
- **No user accounts**: No personal data collection
- **GDPR ready**: Local processing, user control
- **Transparent**: Clear permission explanations

## 10. Error Handling

### 10.1 Common Error Scenarios
```kotlin
// Camera errors
try {
    cameraProvider.bindToLifecycle(...)
} catch (e: Exception) {
    Log.e(TAG, "Camera binding failed", e)
    showErrorAndFinish()
}

// Image loading errors
val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
if (bitmap == null) {
    showError("Failed to load image")
    finish()
}

// Analysis errors
try {
    val result = assessmentEngine.assessImage(uri)
} catch (e: Exception) {
    showError("Analysis failed")
    finish()
}
```

### 10.2 User Feedback
- Toast messages for quick errors
- Alert dialogs for critical errors
- Inline error states in UI
- Retry options where appropriate

## 11. Testing Strategy

### 11.1 Unit Tests
**Target**: Business logic and data models
```kotlin
- RiskLevel calculations
- Hazard type mappings
- Risk score calculations
- Recommendation generation
```

### 11.2 Instrumentation Tests
**Target**: UI and integration
```kotlin
- Activity launches
- Permission flows
- Camera integration
- Image selection
- Results display
```

### 11.3 Manual Testing
- Multiple Android versions (7.0 - 14)
- Various device sizes
- Different camera hardware
- Low memory scenarios
- Offline mode
- Permission denial flows

## 12. Build Configuration

### 12.1 Gradle Configuration
```kotlin
android {
    compileSdk = 34
    minSdk = 24
    targetSdk = 34
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

### 12.2 Dependencies
**Total**: ~20 dependencies
- AndroidX libraries: 8
- ML/AI libraries: 5
- UI libraries: 3
- Utility libraries: 4

### 12.3 Build Variants
- **debug**: Development with debugging
- **release**: Production with optimization

## 13. Deployment

### 13.1 APK Size Estimate
- **Debug APK**: ~15-20 MB
- **Release APK (minified)**: ~8-12 MB
- **ML models**: Included in ML Kit dependency

### 13.2 System Requirements
- Android 7.0 or higher
- Camera hardware (optional)
- ~50 MB storage space
- ~200 MB RAM (typical usage)

### 13.3 Release Checklist
- [ ] Version number updated
- [ ] Release build tested
- [ ] ProGuard configuration verified
- [ ] APK signed
- [ ] Play Store assets prepared
- [ ] Privacy policy finalized

## 14. Future Enhancements

### 14.1 Short-term
- Persistent assessment history (Room database)
- PDF report generation
- Enhanced ML models
- Dark theme support

### 14.2 Long-term
- Cloud storage and sync
- Team collaboration
- Custom ML model training
- Augmented reality overlay
- Integration with safety management systems

---

**Document Version**: 1.0  
**Last Updated**: February 18, 2026  
**Author**: Development Team  
**Status**: Complete
