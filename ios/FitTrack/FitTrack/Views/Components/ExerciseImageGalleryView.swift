import SwiftUI

struct ExerciseImageGalleryView: View {
    let exerciseName: String
    @State private var currentPage = 0

    private var imageURLs: [URL] {
        ExerciseImageService.getImageURLs(for: exerciseName)
    }

    var body: some View {
        if imageURLs.isEmpty {
            VStack(spacing: 4) {
                Image(systemName: "photo").font(.title2).foregroundColor(.secondary)
                Text("No images available").font(.caption).foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, minHeight: 100)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        } else {
            VStack(spacing: 0) {
                TabView(selection: $currentPage) {
                    ForEach(Array(imageURLs.enumerated()), id: \.0) { index, url in
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .empty:
                                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
                            case .success(let image):
                                image.resizable().aspectRatio(contentMode: .fit)
                            case .failure:
                                Image(systemName: "photo").foregroundColor(.secondary)
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            @unknown default:
                                EmptyView()
                            }
                        }
                        .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .always))
                .frame(height: 220)

                HStack {
                    Text(currentPage == 0 ? "Starting Position" : "End Position")
                        .font(.caption2.bold())
                        .foregroundColor(.accentColor)
                    Spacer()
                    Text("Swipe to compare")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
            }
            .background(Color(.secondarySystemBackground))
            .cornerRadius(10)
        }
    }
}
