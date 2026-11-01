import Foundation

/// Snapshot the SwiftData store before the app opens it with a changed
/// schema, so a migration that goes wrong is recoverable. iOS sibling of
/// Android's `DatabaseBackup`.
///
/// SwiftData has no explicit version number, so the "schema stamp" is the
/// sorted list of model names the app was built with. When it differs from
/// the stamp recorded at the last launch, the store files are copied to
/// `Application Support/Backups/` before `ModelContainer` runs its
/// migration. The last five snapshots are kept.
enum StoreBackup {
    private static let stampKey = "fittrack_schema_stamp"
    private static let keep = 5

    static func backupIfSchemaChanged(modelNames: [String]) {
        let stamp = modelNames.sorted().joined(separator: ",")
        let defaults = UserDefaults.standard
        let previous = defaults.string(forKey: stampKey)
        defer { defaults.set(stamp, forKey: stampKey) }
        // First launch (nothing to back up) or unchanged schema.
        guard let previous, previous != stamp else { return }

        let fm = FileManager.default
        guard let support = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return }
        let store = support.appendingPathComponent("default.store")
        guard fm.fileExists(atPath: store.path) else { return }

        let backups = support.appendingPathComponent("Backups", isDirectory: true)
        try? fm.createDirectory(at: backups, withIntermediateDirectories: true)

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyyMMdd-HHmmss"
        let tag = formatter.string(from: Date())

        for suffix in ["", "-wal", "-shm"] {
            let src = support.appendingPathComponent("default.store" + suffix)
            guard fm.fileExists(atPath: src.path) else { continue }
            let dst = backups.appendingPathComponent("default.\(tag).store" + suffix)
            try? fm.copyItem(at: src, to: dst)
        }
        prune(in: backups)
    }

    private static func prune(in dir: URL) {
        let fm = FileManager.default
        guard let files = try? fm.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil) else { return }
        // Group by snapshot tag ("default.<tag>.store*"), newest first by name.
        let tags = Set(files.compactMap { url -> String? in
            let name = url.lastPathComponent
            guard name.hasPrefix("default."), let range = name.range(of: ".store") else { return nil }
            return String(name[name.index(name.startIndex, offsetBy: 8)..<range.lowerBound])
        }).sorted(by: >)
        for stale in tags.dropFirst(keep) {
            for url in files where url.lastPathComponent.hasPrefix("default.\(stale).store") {
                try? fm.removeItem(at: url)
            }
        }
    }
}
