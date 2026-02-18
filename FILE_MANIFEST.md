# VisualRiskAssessor - File Manifest

## Project Root Files
- `.gitignore` - Git ignore rules for Android project
- `LICENSE` - MIT License
- `README.md` - Comprehensive project documentation
- `DEVELOPMENT.md` - Developer guide
- `CONTRIBUTING.md` - Contribution guidelines
- `PROJECT_SUMMARY.md` - Project overview and status
- `IMPLEMENTATION_CHECKLIST.md` - Feature completion checklist
- `FILE_MANIFEST.md` - This file
- `build.gradle.kts` - Root Gradle build configuration
- `settings.gradle.kts` - Gradle settings and module configuration
- `gradle.properties` - Gradle properties and build optimization

## Gradle Wrapper
- `gradle/wrapper/gradle-wrapper.properties` - Gradle wrapper configuration

## Application Module (app/)
- `app/build.gradle.kts` - App module Gradle configuration
- `app/proguard-rules.pro` - ProGuard/R8 rules for code optimization

## Source Code (app/src/main/java/com/hse/visualriskassessor/)

### Application
- `HSEApplication.kt` - Application class

### Data Models (model/)
- `model/RiskLevel.kt` - Risk level enumeration and calculation
- `model/HazardType.kt` - Hazard type enumeration
- `model/Hazard.kt` - Hazard data model with recommendations
- `model/AssessmentResult.kt` - Assessment result data model

### Analysis Engine (analysis/)
- `analysis/HazardDetector.kt` - ML-based hazard detection
- `analysis/RiskAssessmentEngine.kt` - Risk assessment and calculation

### User Interface (ui/)
- `ui/MainActivity.kt` - Main application entry point
- `ui/camera/CameraActivity.kt` - Camera capture functionality
- `ui/results/ResultsActivity.kt` - Results display
- `ui/results/HazardAdapter.kt` - RecyclerView adapter for hazards
- `ui/history/HistoryActivity.kt` - Assessment history (placeholder)
- `ui/widget/RiskMatrixView.kt` - Custom risk matrix visualization

### Utilities (utils/)
- `utils/PermissionManager.kt` - Permission handling utility
- `utils/ImageUtils.kt` - Image processing utilities

## Resources (app/src/main/res/)

### Layouts (layout/)
- `layout/activity_main.xml` - Main screen layout
- `layout/activity_camera.xml` - Camera interface layout
- `layout/activity_results.xml` - Results display layout
- `layout/activity_history.xml` - History screen layout
- `layout/item_hazard.xml` - Hazard card item layout

### Values (values/)
- `values/strings.xml` - String resources (90+ strings)
- `values/colors.xml` - Color palette definitions
- `values/themes.xml` - Material Design themes and styles
- `values/dimens.xml` - Dimension resources

### Drawables (drawable/)
- `drawable/badge_background.xml` - Risk badge background shape
- `drawable/ic_launcher_foreground.xml` - App launcher icon foreground

### Launcher Icons (mipmap-anydpi-v26/)
- `mipmap-anydpi-v26/ic_launcher.xml` - Adaptive launcher icon
- `mipmap-anydpi-v26/ic_launcher_round.xml` - Adaptive round launcher icon

### XML Configuration (xml/)
- `xml/backup_rules.xml` - Backup configuration
- `xml/data_extraction_rules.xml` - Data extraction rules
- `xml/file_paths.xml` - File provider paths

### Android Manifest
- `AndroidManifest.xml` - Application manifest with permissions and activities

## Tests

### Unit Tests (app/src/test/java/com/hse/visualriskassessor/)
- `model/RiskLevelTest.kt` - Unit tests for RiskLevel

### Instrumentation Tests (app/src/androidTest/java/com/hse/visualriskassessor/)
- `ExampleInstrumentedTest.kt` - Sample instrumentation test

## File Statistics

### Source Code
- Kotlin files (main): 15
- Kotlin files (test): 2
- Total lines of Kotlin code: ~3,500

### Resources
- XML files: 16
- Layout files: 5
- Value files: 4
- Drawable files: 2
- Configuration files: 4

### Documentation
- Markdown files: 7
- Total documentation words: ~15,000

### Total Project Files
- Source/Resource files: ~35
- Configuration files: 8
- Documentation files: 7
- **Total: 50+ files**

## Key File Relationships

### Activity Flow
```
MainActivity.kt
> CameraActivity.kt ──> ResultsActivity.kt
> HistoryActivity.kt
```

### Data Flow
```
Image Input (Camera/Library)
    ↓
RiskAssessmentEngine.kt
    ↓
HazardDetector.kt (ML Kit)
    ↓
AssessmentResult (Hazard objects)
    ↓
ResultsActivity.kt (Display)
```

### Model Dependencies
```
AssessmentResult
> List<Hazard>
   ├─> HazardType
   └─> RiskLevel
> RiskLevel
```

---

**Last Updated**: February 18, 2026
**Total Files**: 50+
**Total LOC**: ~3,500
