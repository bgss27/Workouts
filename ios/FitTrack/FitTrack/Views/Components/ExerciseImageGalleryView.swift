import SwiftUI

struct ExerciseImageGalleryView: View {
    let exerciseName: String
    @StateObject private var imageService = ExerciseImageService.shared
    @State private var currentPage = 0

    private var imageURLs: [URL] {
        imageService.images[exerciseName] ?? []
    }
    private var isLoading: Bool {
        imageService.loadingStates[exerciseName] ?? false
    }

    var body: some View {
        Group {
            if isLoading {
                VStack(spacing: 8) {
                    ProgressView()
                    Text("Loading exercise images...")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.secondarySystemBackground))
            } else if imageURLs.isEmpty {
                VStack(spacing: 4) {
                    Image(systemName: "photo.badge.exclamationmark")
                        .font(.title2)
                        .foregroundColor(.secondary)
                    Text("No images available")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.secondarySystemBackground))
            } else {
                TabView(selection: $currentPage) {
                    ForEach(Array(imageURLs.enumerated()), id: \.1) { index, url in
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .empty:
                                ProgressView()
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            case .success(let image):
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                            case .failure:
                                Image(systemName: "photo")
                                    .foregroundColor(.secondary)
                            @unknown default:
                                EmptyView()
                            }
                        }
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: imageURLs.count > 1 ? .always : .never))
            }
        }
        .task {
            await imageService.loadImages(for: exerciseName)
        }
    }
}
