import Foundation
import UIKit

/// Owns the on-device JPEG files behind `ProgressPhoto` rows. Images are
/// re-encoded from the decoded bitmap (so EXIF, including location, is
/// dropped) and downscaled to a 1600 px long edge. Nothing here touches the
/// network. Mirrors Android's `ProgressPhotoStore`.
enum ProgressPhotoStore {
    static let maxLongEdge: CGFloat = 1600
    static let jpegQuality: CGFloat = 0.85

    static var directory: URL {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
        let dir = support.appendingPathComponent("ProgressPhotos", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    static func url(for fileName: String) -> URL {
        directory.appendingPathComponent(fileName)
    }

    /// Downscale, strip metadata and write. Returns the new file name.
    static func save(_ image: UIImage) throws -> String {
        let scaled = downscale(image)
        guard let data = scaled.jpegData(compressionQuality: jpegQuality) else {
            throw CocoaError(.fileWriteUnknown)
        }
        let name = "photo-\(Int(Date().timeIntervalSince1970 * 1000))-\(UUID().uuidString.prefix(8)).jpg"
        try data.write(to: url(for: name), options: .atomic)
        return name
    }

    static func delete(fileName: String) {
        try? FileManager.default.removeItem(at: url(for: fileName))
    }

    static func load(fileName: String) -> UIImage? {
        UIImage(contentsOfFile: url(for: fileName).path)
    }

    /// Small decode for grid tiles, so a 3-column grid of 1600 px JPEGs
    /// doesn't hold tens of megabytes of bitmaps.
    static func thumbnail(fileName: String, maxPixel: CGFloat = 400) -> UIImage? {
        guard let source = CGImageSourceCreateWithURL(url(for: fileName) as CFURL, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixel,
            kCGImageSourceCreateThumbnailWithTransform: true,
        ]
        guard let cg = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
        return UIImage(cgImage: cg)
    }

    private static func downscale(_ image: UIImage) -> UIImage {
        let size = image.size
        let longEdge = max(size.width, size.height)
        guard longEdge > maxLongEdge else { return image.normalizedOrientation() }
        let scale = maxLongEdge / longEdge
        let target = CGSize(width: (size.width * scale).rounded(), height: (size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))
        }
    }
}

private extension UIImage {
    /// Re-render so the pixel data matches the display orientation and the
    /// encoder writes no orientation / EXIF metadata.
    func normalizedOrientation() -> UIImage {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: size, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: size))
        }
    }
}
