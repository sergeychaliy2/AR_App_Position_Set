# Built-in models

Place the following `.glb` files here to populate the built-in gallery shown in
the `GalleryCatalog`:

| id                         | file                    |
|----------------------------|-------------------------|
| builtin:chair_modern       | `chair_modern.glb`      |
| builtin:table_round        | `table_round.glb`       |
| builtin:lamp_arc           | `lamp_arc.glb`          |
| builtin:plant_monstera     | `plant_monstera.glb`    |
| builtin:speaker_mini       | `speaker_mini.glb`      |
| builtin:sculpture_curve    | `sculpture_curve.glb`   |

## Where to get models

- [Khronos glTF Sample Models](https://github.com/KhronosGroup/glTF-Sample-Models)
- [Sketchfab](https://sketchfab.com) — filter by CC-BY / CC0
- [Poly Haven](https://polyhaven.com/models) — CC0
- [Google Poly archive](https://poly.pizza)

## Why aren't the files committed?

They are user-replaceable assets. Committing large binaries to the repo bloats
clone time and clashes with licensing — pick your own.

## How they are loaded

The `GalleryCatalog` references them through the URI
`file:///android_asset/models/<name>.glb`. The SceneView ModelLoader resolves
this and streams the model into Filament at model-placement time.

If a referenced file is missing, the placement will fail gracefully and emit a
`LoadingFailed` event (shown as a snackbar). The app itself still runs.
