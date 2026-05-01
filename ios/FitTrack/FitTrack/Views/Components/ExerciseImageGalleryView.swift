import SwiftUI

struct ExerciseImageGalleryView: View {
    let exerciseName: String
    @StateObject private var imageService = ExerciseImageService.shared

    private var mediaData: ExerciseMediaData? {
        imageService.media[exerciseName] ?? nil
    }
    private var isLoading: Bool {
        imageService.loadingStates[exerciseName] ?? false
    }

    var body: some View {
        VStack(spacing: 0) {
            // Image/GIF area
            Group {
                if isLoading {
                    VStack(spacing: 8) {
                        ProgressView()
                        Text("Loading exercise animation...")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 200)
                    .background(Color(.secondarySystemBackground))
                } else if let media = mediaData {
                    AsyncImage(url: media.gifUrl) { phase in
                        switch phase {
                        case .empty:
                            ProgressView()
                                .frame(maxWidth: .infinity, minHeight: 200)
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(maxWidth: .infinity)
                        case .failure:
                            VStack(spacing: 4) {
                                Image(systemName: "photo.badge.exclamationmark")
                                    .font(.title2).foregroundColor(.secondary)
                                Text("Failed to load").font(.caption).foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity, minHeight: 200)
                        @unknown default:
                            EmptyView()
                        }
                    }
                } else {
                    VStack(spacing: 4) {
                        Image(systemName: "photo")
                            .font(.title2).foregroundColor(.secondary)
                        Text("No animation available")
                            .font(.caption).foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, minHeight: 200)
                    .background(Color(.secondarySystemBackground))
                }
            }

            // Target muscles from API
            if let media = mediaData, !media.target.isEmpty {
                HStack(spacing: 6) {
                    Text("Target:").font(.caption2.bold())
                    Text(media.target.capitalized)
                        .font(.caption2.bold())
                        .padding(.horizontal, 8).padding(.vertical, 3)
                        .background(Color.accentColor.opacity(0.15))
                        .cornerRadius(8)
                    ForEach(media.secondaryMuscles.prefix(2), id: \.self) { muscle in
                        Text(muscle.capitalized)
                            .font(.caption2.bold())
                            .padding(.horizontal, 8).padding(.vertical, 3)
                            .background(Color.teal.opacity(0.15))
                            .cornerRadius(8)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
            }
        }
        .cornerRadius(10)
        .task {
            await imageService.loadMedia(for: exerciseName)
        }
    }
}
