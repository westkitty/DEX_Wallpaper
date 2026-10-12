"""Prevent the observed Canvas recycled-bitmap crash from returning.

Compose Image()/BitmapPainter retains the same native Bitmap passed through
asImageBitmap(). A DisposableEffect cannot safely recycle that backing allocation
because rendering may still be using it after a state change.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / "app/src/main/java/com/westkitty/dexwallpaper/ui"


class ComposeBitmapOwnershipTest(unittest.TestCase):
    def test_library_thumbnail_owns_pixels_for_compositional_lifetime(self):
        text = (ROOT / "CoreWorkspaces.kt").read_text(encoding="utf-8")
        section = text.split("@Composable private fun Thumbnail", 1)[1].split(
            "@OptIn", 1
        )[0]
        self.assertIn("asImageBitmap()", section)
        self.assertNotIn(".recycle()", section)
        self.assertNotIn("DisposableEffect(bitmap)", section)

    def test_artwork_preview_owns_pixels_for_compositional_lifetime(self):
        text = (ROOT / "Components.kt").read_text(encoding="utf-8")
        section = text.split("@Composable fun ArtworkPreview", 1)[1].split(
            "@Composable fun CollectionPicker", 1
        )[0]
        self.assertIn("asImageBitmap()", section)
        self.assertNotIn(".recycle()", section)
        self.assertNotIn("DisposableEffect(image)", section)


if __name__ == "__main__":
    unittest.main()
