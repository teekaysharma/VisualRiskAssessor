# Image Metadata Extraction Feature

This feature adds image metadata extraction and display to the VisualRiskAssessor web application.

## Features Added

1. **Image Filename Display** - Shows the uploaded image filename (may contain location/date clues)
2. **Dimensions and Resolution** - Displays image width, height, and megapixels
3. **Low Resolution Warning** - Warns if image resolution is below 0.64 MP
4. **EXIF Data Extraction** - Extracts metadata from JPEG and TIFF images including:
   - Device make and model
   - Capture date/time
   - GPS coordinates (latitude/longitude)
5. **Stale Image Warning** - Warns if image was captured more than 7 days ago
6. **GPS Map Link** - Provides a link to Google Maps when GPS coordinates are available

## How to Apply Changes

### Option 1: Automated (Recommended)

Run the Python script to apply all changes:

```bash
cd /home/engine/project
python3 apply_metadata_changes.py
```

This will automatically:
- Add the `imageMetadata` global variable
- Add EXIF extraction functions
- Modify the file upload handler
- Add the `renderImageMetadata` helper function
- Update the metadata footer display
- Update the reset function

### Option 2: Manual

Follow the detailed instructions in `IMPLEMENTATION_GUIDE.md` which provides:
- Exact line numbers for each change
- Before/after code snippets
- Testing instructions

## After Applying Changes

### Testing the Feature

1. Open `index.html` in a web browser
2. Upload an image (preferably a JPEG from a camera with EXIF data)
3. Check the "Assessment Metadata" footer section at the bottom

### Expected Results

You should see:

```
Assessment Information
├─ Assessment Date and Time: [current date/time]
├─ Assessment Method: AI Visual Analysis — Llama 4 Scout via Groq API
├─ Standard Reference: ISO 45001:2018 / ADOSH-SF
│
Image Metadata
├─ Image Filename: [filename.jpg]
├─ Dimensions: [width] × [height] px [⚠️ Low Resolution]
├─ Resolution: [X.XX] MP
├─ File Size: [XXX.X] KB
├─ Device: [Make] [Model] (if available)
├─ Capture Date: [date/time] [⚠️ Stale Image] (if available)
├─ GPS Coordinates: [lat], [lon] 📍 View Map (if available)
└─ EXIF Data: Not available (if no EXIF)
```

### Warnings Displayed

1. **Low Resolution Warning** (⚠️)
   - Appears when: Image resolution < 0.64 MP (below 800×600)
   - Message: "Low resolution image detected. This may affect AI analysis accuracy. For best results, use images with at least 1280×720 resolution."

2. **Stale Image Warning** (⚠️)
   - Appears when: Image capture date is more than 7 days before/after assessment date
   - Message: "Image may not reflect current site conditions (captured X days ago/in the future)."

## Technical Details

### When Metadata is Extracted

The metadata extraction happens **before** the image is sent to the Groq API, in the file upload event handler:

```javascript
els.fileInput.addEventListener('change', async e => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (!f.type.startsWith('image/')) {
        showWarning('Please upload a valid image file.');
        e.target.value = '';
        return;
    }
    // Metadata extracted HERE, before analysis
    imageMetadata = await extractImageMetadata(f);
    const reader = new FileReader();
    reader.onload = ev => analyze(ev.target.result);
    reader.onerror = () => showWarning('Failed to read selected image.');
    reader.readAsDataURL(f);
});
```

### Supported Image Formats

- **JPEG**: Full EXIF support including GPS and date/time
- **TIFF**: Full EXIF support
- **PNG, WebP, others**: Basic metadata only (filename, size, dimensions), no EXIF

### Privacy Considerations

- All metadata extraction happens client-side in the browser
- No data is sent to any server except the Groq API (which receives the image itself)
- GPS coordinates are only displayed to the user, not transmitted anywhere

## Browser Compatibility

Works in all modern browsers that support:
- FileReader API
- DataView API
- Promise/async-await
- ES6 JavaScript

Tested on: Chrome, Firefox, Safari, Edge (latest versions)

## Troubleshooting

### "EXIF Data: Not available"

This is normal for:
- PNG, WebP, and other non-JPEG/TIFF formats
- JPEG images stripped of EXIF (e.g., from social media)
- Images edited with software that removes EXIF

### No GPS coordinates shown

GPS coordinates only appear if:
- The image was taken with GPS enabled (camera phone with location services)
- The GPS data wasn't stripped during editing/sharing
- The image is in JPEG format

### Stale image warning not appearing

The stale image warning requires:
- EXIF DateTimeOriginal field to be present
- The capture date to differ from assessment date by more than 7 days

## Rollback

If you need to revert the changes, restore from git:

```bash
git checkout index.html
```

Or manually remove the changes following the reverse of the IMPLEMENTATION_GUIDE.md instructions.

## Files Modified

- `index.html` - Main application file (all changes in the `<script>` section)

## Files Created

- `IMPLEMENTATION_GUIDE.md` - Detailed manual implementation instructions
- `IMAGE_METADATA_README.md` - This file
- `apply_metadata_changes.py` - Automated application script
