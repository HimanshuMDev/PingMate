import kagglehub
import os

print("Downloading Android-optimized Gemma 2B INT4 TFLite model...")
# Download the Android MediaPipe version (CPU INT4 Quantized)
path = kagglehub.model_download("google/gemma/tfLite/gemma-2b-it-cpu-int4")

print(f"Download complete! Saved to: {path}")

# Verify the file is actually there
files = os.listdir(path)
print("Files in directory:")
for f in files:
    print(f" - {f}")
