#!/usr/bin/env python3
"""Apply image metadata extraction changes to index.html"""

# Read the original file
with open('index.html', 'r') as f:
    content = f.read()

# Track changes
changes_made = []

# 1. Add the imageMetadata variable declaration
old_vars = """  let manualModeActive = false;

  const BASELINE_DB = {"""
new_vars = """  let manualModeActive = false;
  let imageMetadata = null;

  const BASELINE_DB = {"""
if old_vars in content:
    content = content.replace(old_vars, new_vars)
    changes_made.append("✓ Added imageMetadata variable declaration")

# Read the EXIF functions from a separate file for clarity
exif_functions = """
  // Extract EXIF metadata from image file
  async function extractImageMetadata(file) {
    const metadata = {
      filename: file.name,
      size: file.size,
      type: file.type,
      dimensions: null,
      exif: null
    };

    try {
      const img = new Image();
      const dataUrl = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = e => resolve(e.target.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
      img.src = dataUrl;
      await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = reject;
        if (img.complete) resolve();
      });
      metadata.dimensions = {
        width: img.width,
        height: img.height,
        resolution: img.width * img.height,
        megapixels: (img.width * img.height / 1000000).toFixed(2)
      };

      if (file.type.startsWith('image/jpeg') || file.type.startsWith('image/tiff')) {
        const exifData = await extractEXIF(file);
        metadata.exif = exifData;
      }
    } catch (err) {
      console.warn('Failed to extract some image metadata:', err.message);
    }

    return metadata;
  }

  async function extractEXIF(file) {
    try {
      const arrayBuffer = await file.arrayBuffer();
      const view = new DataView(arrayBuffer);

      if (view.getUint16(0) !== 0xFFD8) return null;

      let offset = 2;
      const exif = {};

      while (offset < view.byteLength) {
        if (view.getUint16(offset) !== 0xFFE1) {
          offset += 2 + view.getUint16(offset + 2);
          continue;
        }

        if (view.getUint32(offset + 4) !== 0x45786966) break;

        const tiffOffset = offset + 10;
        const littleEndian = view.getUint16(tiffOffset) === 0x4949;
        const ifdOffset = view.getUint32(tiffOffset + 4, littleEndian);

        parseIFD(view, tiffOffset + ifdOffset, littleEndian, exif);

        if (exif.ExifIFDPointer) {
          parseIFD(view, tiffOffset + exif.ExifIFDPointer, littleEndian, exif);
        }

        if (exif.GPSInfoIFDPointer) {
          parseGPS(view, tiffOffset + exif.GPSInfoIFDPointer, littleEndian, exif);
        }

        break;
      }

      if (exif.DateTimeOriginal) {
        exif.DateTimeOriginal = parseEXIFDate(exif.DateTimeOriginal);
      }
      if (exif.GPSLatitude && exif.GPSLongitude) {
        exif.GPS = {
          latitude: convertDMSToDD(exif.GPSLatitude, exif.GPSLatitudeRef),
          longitude: convertDMSToDD(exif.GPSLongitude, exif.GPSLongitudeRef)
        };
      }

      return exif;
    } catch (err) {
      console.warn('Failed to parse EXIF:', err.message);
      return null;
    }
  }

  function parseIFD(view, offset, littleEndian, exif) {
    const numEntries = view.getUint16(offset, littleEndian);
    for (let i = 0; i < numEntries; i++) {
      const entryOffset = offset + 2 + (i * 12);
      const tag = view.getUint16(entryOffset, littleEndian);
      const type = view.getUint16(entryOffset + 2, littleEndian);
      const count = view.getUint32(entryOffset + 4, littleEndian);
      const valueOffset = view.getUint32(entryOffset + 8, littleEndian) + (count > 4 ? offset : 0);

      const tagName = getEXIFTagName(tag);
      if (tagName) {
        exif[tagName] = readEXIFValue(view, valueOffset, type, count, littleEndian);
      }
    }
  }

  function parseGPS(view, offset, littleEndian, exif) {
    const numEntries = view.getUint16(offset, littleEndian);
    for (let i = 0; i < numEntries; i++) {
      const entryOffset = offset + 2 + (i * 12);
      const tag = view.getUint16(entryOffset, littleEndian);
      const type = view.getUint16(entryOffset + 2, littleEndian);
      const count = view.getUint32(entryOffset + 4, littleEndian);
      const valueOffset = view.getUint32(entryOffset + 8, littleEndian) + (count > 4 ? offset : 0);

      if (tag === 1) exif.GPSLatitudeRef = readEXIFValue(view, valueOffset, type, count, littleEndian);
      else if (tag === 2) exif.GPSLatitude = readEXIFValue(view, valueOffset, type, count, littleEndian);
      else if (tag === 3) exif.GPSLongitudeRef = readEXIFValue(view, valueOffset, type, count, littleEndian);
      else if (tag === 4) exif.GPSLongitude = readEXIFValue(view, valueOffset, type, count, littleEndian);
    }
  }

  function getEXIFTagName(tag) {
    const tags = {
      0x010F: 'Make',
      0x0110: 'Model',
      0x0132: 'DateTime',
      0x8769: 'ExifIFDPointer',
      0x8825: 'GPSInfoIFDPointer',
      0x9003: 'DateTimeOriginal',
      0x9004: 'DateTimeDigitized',
      0xA002: 'PixelXDimension',
      0xA003: 'PixelYDimension'
    };
    return tags[tag];
  }

  function readEXIFValue(view, offset, type, count, littleEndian) {
    switch (type) {
      case 1: return view.getUint8(offset);
      case 2: return readEXIFString(view, offset, count);
      case 3: return view.getUint16(offset, littleEndian);
      case 4: return view.getUint32(offset, littleEndian);
      case 5: return view.getUint32(offset, littleEndian) / view.getUint32(offset + 4, littleEndian);
      case 7: return readEXIFRationalArray(view, offset, count, littleEndian);
      case 9: return view.getInt32(offset, littleEndian);
      case 10: return view.getInt32(offset, littleEndian) / view.getInt32(offset + 4, littleEndian);
      default: return null;
    }
  }

  function readEXIFString(view, offset, count) {
    let str = '';
    for (let i = 0; i < count - 1; i++) {
      str += String.fromCharCode(view.getUint8(offset + i));
    }
    return str;
  }

  function readEXIFRationalArray(view, offset, count, littleEndian) {
    const arr = [];
    for (let i = 0; i < count; i++) {
      arr.push({
        numerator: view.getUint32(offset + (i * 8), littleEndian),
        denominator: view.getUint32(offset + (i * 8) + 4, littleEndian)
      });
    }
    return arr;
  }

  function parseEXIFDate(dateStr) {
    if (!dateStr || dateStr.length !== 19) return null;
    const parts = dateStr.split(' ');
    if (parts.length !== 2) return null;
    const dateParts = parts[0].split(':');
    const timeParts = parts[1].split(':');
    if (dateParts.length !== 3 || timeParts.length !== 3) return null;
    return new Date(
      parseInt(dateParts[0]),
      parseInt(dateParts[1]) - 1,
      parseInt(dateParts[2]),
      parseInt(timeParts[0]),
      parseInt(timeParts[1]),
      parseInt(timeParts[2])
    );
  }

  function convertDMSToDD(dms, ref) {
    if (!dms || dms.length < 3) return null;
    const degrees = dms[0].numerator / dms[0].denominator;
    const minutes = dms[1].numerator / dms[1].denominator;
    const seconds = dms[2].numerator / dms[2].denominator;
    let dd = degrees + minutes / 60 + seconds / 3600;
    if (ref === 'S' || ref === 'W') dd = dd * -1;
    return dd;
  }

"""

# 2. Add EXIF extraction function before analyze function
analyze_function_start = "  async function analyze(dataUrl) {"
if analyze_function_start in content:
    content = content.replace(analyze_function_start, exif_functions + analyze_function_start)
    changes_made.append("✓ Added EXIF extraction functions")

# 3. Modify file input event listener
old_file_listener = """  els.fileInput.addEventListener('change', e => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (!f.type.startsWith('image/')) { showWarning('Please upload a valid image file.'); e.target.value = ''; return; }
    const reader = new FileReader();
    reader.onload = ev => analyze(ev.target.result);
    reader.onerror = () => showWarning('Failed to read selected image.');
    reader.readAsDataURL(f);
  });"""

new_file_listener = """  els.fileInput.addEventListener('change', async e => {
    const f = e.target.files?.[0];
    if (!f) return;
    if (!f.type.startsWith('image/')) { showWarning('Please upload a valid image file.'); e.target.value = ''; return; }
    imageMetadata = await extractImageMetadata(f);
    const reader = new FileReader();
    reader.onload = ev => analyze(ev.target.result);
    reader.onerror = () => showWarning('Failed to read selected image.');
    reader.readAsDataURL(f);
  });"""

if old_file_listener in content:
    content = content.replace(old_file_listener, new_file_listener)
    changes_made.append("✓ Modified file input listener to extract metadata")

# 4. Add renderImageMetadata helper function
render_metadata_function = """

  function renderImageMetadata(metadata, assessmentDate) {
    let html = '<h4 style="margin-top:16px;">Image Metadata</h4>';

    html += `
      <div class="metadata-row">
        <span class="metadata-label">Image Filename:</span>
        <span class="metadata-value">${metadata.filename}</span>
      </div>`;

    if (metadata.dimensions) {
      const isLowRes = metadata.dimensions.resolution < 640000;
      html += `
      <div class="metadata-row">
        <span class="metadata-label">Dimensions:</span>
        <span class="metadata-value">${metadata.dimensions.width} × ${metadata.dimensions.height} px</span>
        ${isLowRes ? '<span style="color:#f44336;font-weight:700;margin-left:8px;">⚠️ Low Resolution</span>' : ''}
      </div>
      <div class="metadata-row">
        <span class="metadata-label">Resolution:</span>
        <span class="metadata-value">${metadata.dimensions.megapixels} MP</span>
      </div>`;
      if (isLowRes) {
        html += `
      <div class="warning" style="margin-top:8px;font-size:11px;">
        ⚠️ Low resolution image detected. This may affect AI analysis accuracy. For best results, use images with at least 1280×720 resolution.
      </div>`;
      }
    }

    html += `
      <div class="metadata-row">
        <span class="metadata-label">File Size:</span>
        <span class="metadata-value">${(metadata.size / 1024).toFixed(1)} KB</span>
      </div>`;

    if (metadata.exif) {
      if (metadata.exif.Make || metadata.exif.Model) {
        html += `
      <div class="metadata-row">
        <span class="metadata-label">Device:</span>
        <span class="metadata-value">${metadata.exif.Make || ''} ${metadata.exif.Model || ''}</span>
      </div>`;
      }

      if (metadata.exif.DateTimeOriginal) {
        const captureDate = new Date(metadata.exif.DateTimeOriginal);
        const daysDiff = Math.floor((assessmentDate - captureDate) / (1000 * 60 * 60 * 24));
        const isStale = Math.abs(daysDiff) > 7;

        html += `
      <div class="metadata-row">
        <span class="metadata-label">Capture Date:</span>
        <span class="metadata-value">${captureDate.toLocaleString()}</span>
        ${isStale ? '<span style="color:#f44336;font-weight:700;margin-left:8px;">⚠️ Stale Image</span>' : ''}
      </div>`;

        if (isStale) {
          html += `
      <div class="warning" style="margin-top:8px;font-size:11px;">
        ⚠️ Image may not reflect current site conditions (captured ${Math.abs(daysDiff)} days ${daysDiff > 0 ? 'ago' : 'in the future'}).
      </div>`;
        }
      }

      if (metadata.exif.GPS && metadata.exif.GPS.latitude !== null && metadata.exif.GPS.longitude !== null) {
        const lat = metadata.exif.GPS.latitude.toFixed(6);
        const lon = metadata.exif.GPS.longitude.toFixed(6);
        html += `
      <div class="metadata-row">
        <span class="metadata-label">GPS Coordinates:</span>
        <span class="metadata-value">${lat}, ${lon}</span>
        <a href="https://www.google.com/maps?q=${lat},${lon}" target="_blank" rel="noopener" style="margin-left:8px;color:#1565c0;text-decoration:none;font-size:11px;">📍 View Map</a>
      </div>`;
      }
    } else {
      html += `
      <div class="metadata-row">
        <span class="metadata-label">EXIF Data:</span>
        <span class="metadata-value">Not available</span>
      </div>`;
    }

    return html;
  }
"""

reset_function = "  function resetApp() {"
if reset_function in content:
    content = content.replace(reset_function, render_metadata_function + reset_function)
    changes_made.append("✓ Added renderImageMetadata helper function")

# 5. Modify metadata footer update
old_metadata_update = """    els.metadataFooter.innerHTML = `
      <h4>Assessment Information</h4>
      <div class="metadata-row">
        <span class="metadata-label">Assessment Date and Time:</span>
        <span class="metadata-value">${formattedDate}</span>
      </div>${contextRow}
      <div class="metadata-row">
        <span class="metadata-label">Assessment Method:</span>
        <span class="metadata-value">AI Visual Analysis — Llama 4 Scout via Groq API</span>
      </div>
      <div class="metadata-row">
        <span class="metadata-label">Standard Reference:</span>
        <span class="metadata-value">ISO 45001:2018 / ADOSH-SF</span>
      </div>
      <div class="disclaimer">This assessment is AI-generated and requires verification by a competent HSE professional before use in a formal risk register.</div>
    `;"""

new_metadata_update = """    els.metadataFooter.innerHTML = `
      <h4>Assessment Information</h4>
      <div class="metadata-row">
        <span class="metadata-label">Assessment Date and Time:</span>
        <span class="metadata-value">${formattedDate}</span>
      </div>${contextRow}
      <div class="metadata-row">
        <span class="metadata-label">Assessment Method:</span>
        <span class="metadata-value">AI Visual Analysis — Llama 4 Scout via Groq API</span>
      </div>
      <div class="metadata-row">
        <span class="metadata-label">Standard Reference:</span>
        <span class="metadata-value">ISO 45001:2018 / ADOSH-SF</span>
      </div>${imageMetadata ? renderImageMetadata(imageMetadata, assessmentTimestamp) : ''}
      <div class="disclaimer">This assessment is AI-generated and requires verification by a competent HSE professional before use in a formal risk register.</div>
    `;"""

if old_metadata_update in content:
    content = content.replace(old_metadata_update, new_metadata_update)
    changes_made.append("✓ Modified metadata footer update function")

# 6. Reset imageMetadata when resetting
old_reset_body = """  function resetApp() {
    imageData = null;
    hazards = [];"""

new_reset_body = """  function resetApp() {
    imageData = null;
    imageMetadata = null;
    hazards = [];"""

if old_reset_body in content:
    content = content.replace(old_reset_body, new_reset_body)
    changes_made.append("✓ Modified resetApp to clear imageMetadata")

# Write modified content back
with open('index.html', 'w') as f:
    f.write(content)

print("\nImage Metadata Extraction - Changes Applied Successfully!")
print("=" * 60)
for change in changes_made:
    print(change)
print("=" * 60)
print(f"\nTotal changes: {len(changes_made)}")
print("\nThe following features have been added:")
print("• Image filename display")
print("• Image dimensions and resolution")
print("• Low resolution warning (< 0.64 MP)")
print("• EXIF data extraction (JPEG/TIFF)")
print("• Device information from EXIF")
print("• Capture date with stale image warning (> 7 days)")
print("• GPS coordinates with Google Maps link")
print("\nTo test: Open index.html in a browser and upload an image")
