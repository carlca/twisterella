from PIL import Image

image_path = "./TwisterColors 1 to 126.png"  # Replace with the correct path if needed

try:
    img = Image.open(image_path)
    width, height = img.size

    num_bands = 126  # Assuming 126 bands are arranged horizontally or vertically

    if width > height: # Assume horizontal bands
        band_width = width // num_bands
        y_center = height // 2
        for i in range(num_bands):
            x_center = (i * band_width) + (band_width // 2)
            r, g, b = img.getpixel((x_center, y_center))
            print(f"Band {i+1}: RGB({r}, {g}, {b})")

    else: # Assume vertical bands
        band_height = height // num_bands
        x_center = width // 2
        for i in range(num_bands):
            y_center = (i * band_height) + (band_height // 2)
            r, g, b = img.getpixel((x_center, y_center))
            print(f"Band {i+1}: RGB({r}, {g}, {b})")


except FileNotFoundError:
    print(f"Error: File not found at path: {image_path}")
except Exception as e:
    print(f"An error occurred: {e}")
