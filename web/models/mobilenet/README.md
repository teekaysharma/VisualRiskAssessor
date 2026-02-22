# Optional local MobileNet model

Place a TensorFlow.js MobileNet `model.json` and shard files in this directory to enable offline/lab inference.

- Expected entry point: `web/models/mobilenet/model.json`
- To force local model loading, launch with query string: `?localModel=1`
- Default behavior: attempt CDN-hosted MobileNet model.
