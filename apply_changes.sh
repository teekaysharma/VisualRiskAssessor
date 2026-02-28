#!/bin/bash
cd /home/engine/project
if [ -f "apply_metadata_changes.py" ]; then
    python3 apply_metadata_changes.py
    echo "✓ Changes applied successfully!"
else
    echo "✗ apply_metadata_changes.py not found"
    exit 1
fi
