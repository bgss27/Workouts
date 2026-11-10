import Foundation
import SwiftData

/// Which way the user was facing. Raw values shared with Android.
enum PhotoPose: String, Codable, CaseIterable, Identifiable {
    case front = "FRONT", side = "SIDE", back = "BACK", other = "OTHER"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .front: return "Front"
        case .side: return "Side"
        case .back: return "Back"
        case .other: return "Other"
        }
    }
}

/// One progress photo. The image lives as a JPEG in the app's own
/// `Application Support/ProgressPhotos/` folder (see `ProgressPhotoStore`);
/// this row only holds the file name and the metadata. Photos never leave
/// the device. Mirrors Android's `ProgressPhoto` entity.
@Model
final class ProgressPhoto {
    var takenAt: Date
    /// File name inside the photos directory.
    var fileName: String
    var poseRaw: String = PhotoPose.front.rawValue
    var notes: String?

    var pose: PhotoPose {
        get { PhotoPose(rawValue: poseRaw) ?? .front }
        set { poseRaw = newValue.rawValue }
    }

    init(takenAt: Date = Date(), fileName: String, pose: PhotoPose = .front, notes: String? = nil) {
        self.takenAt = takenAt
        self.fileName = fileName
        self.poseRaw = pose.rawValue
        self.notes = notes
    }
}
