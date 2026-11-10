import SwiftUI
import SwiftData
import PhotosUI
import UIKit

/// Progress photo grid inside Body Metrics: add from camera or library,
/// view full screen, compare two side by side. Files live in the app
/// sandbox (see `ProgressPhotoStore`) and are never uploaded. Mirrors
/// Android's `ProgressPhotosScreen`.
struct ProgressPhotosView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ProgressPhoto.takenAt, order: .reverse) private var photos: [ProgressPhoto]

    @State private var pickerItem: PhotosPickerItem?
    @State private var showLibrary = false
    @State private var showCamera = false
    @State private var pendingImage: UIImage?
    @State private var editing: ProgressPhoto?
    @State private var viewing: ProgressPhoto?
    @State private var compareMode = false
    @State private var compareSelection: [PersistentIdentifier] = []
    @State private var comparePair: ComparePair?
    @State private var saveError: String?

    private struct ComparePair: Identifiable {
        let first: ProgressPhoto
        let second: ProgressPhoto
        var id: String { "\(first.fileName)|\(second.fileName)" }
    }

    private var monthGroups: [(String, [ProgressPhoto])] {
        let f = DateFormatter()
        f.dateFormat = "LLLL yyyy"
        var order: [String] = []
        var groups: [String: [ProgressPhoto]] = [:]
        for p in photos {
            let key = f.string(from: p.takenAt)
            if groups[key] == nil { order.append(key) }
            groups[key, default: []].append(p)
        }
        return order.map { ($0, groups[$0] ?? []) }
    }

    var body: some View {
        Group {
            if photos.isEmpty {
                emptyState
            } else {
                grid
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                if photos.count >= 2 {
                    Button {
                        compareMode.toggle()
                        compareSelection = []
                    } label: {
                        Image(systemName: compareMode ? "rectangle.split.2x1.fill" : "rectangle.split.2x1")
                    }
                    .accessibilityLabel(compareMode ? "Stop comparing" : "Compare two photos")
                }
                Menu {
                    if UIImagePickerController.isSourceTypeAvailable(.camera) {
                        Button { showCamera = true } label: { Label("Take photo", systemImage: "camera") }
                    }
                    Button { showLibrary = true } label: {
                        Label("Choose from library", systemImage: "photo.on.rectangle")
                    }
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Add progress photo")
            }
        }
        // Presented from a modifier rather than inside the Menu, which
        // doesn't reliably host a PhotosPicker.
        .photosPicker(isPresented: $showLibrary, selection: $pickerItem, matching: .images)
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                    pendingImage = image
                }
                pickerItem = nil
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker { image in
                showCamera = false
                if let image { pendingImage = image }
            }
            .ignoresSafeArea()
        }
        .sheet(item: $pendingImage) { image in
            PhotoDetailsSheet(image: image, existing: nil) { date, pose, notes in
                addPhoto(image, takenAt: date, pose: pose, notes: notes)
            }
        }
        .sheet(item: $editing) { photo in
            PhotoDetailsSheet(image: ProgressPhotoStore.load(fileName: photo.fileName), existing: photo) { date, pose, notes in
                photo.takenAt = date
                photo.pose = pose
                photo.notes = notes
                try? modelContext.save()
            }
        }
        .fullScreenCover(item: $viewing) { photo in
            PhotoViewer(photos: photos, start: photo, onEdit: { editing = $0 }, onDelete: { delete($0) })
        }
        .sheet(item: $comparePair) { pair in
            ComparePhotosView(first: pair.first, second: pair.second)
        }
        .alert("Couldn't save photo", isPresented: Binding(get: { saveError != nil }, set: { if !$0 { saveError = nil } })) {
            Button("OK", role: .cancel) {}
        } message: { Text(saveError ?? "") }
    }

    private var emptyState: some View {
        Tile {
            VStack(alignment: .leading, spacing: 8) {
                Label("No progress photos yet", systemImage: "camera.fill")
                    .font(.subheadline.weight(.semibold))
                Text("Take one every few weeks in the same light and pose. Photos stay on this device in the app's own folder and are never uploaded.")
                    .font(.caption).foregroundStyle(Color.fitOnSurfaceVariant)
            }
        }
        .padding(.horizontal, Fit.gutter)
    }

    private var grid: some View {
        VStack(alignment: .leading, spacing: 12) {
            if compareMode {
                Text(compareSelection.isEmpty ? "Tap two photos to compare them side by side."
                     : "Now tap the second photo.")
                    .font(.caption).foregroundStyle(Color.fitOnSurfaceVariant)
                    .padding(.horizontal, Fit.gutter)
            }
            ForEach(monthGroups, id: \.0) { month, items in
                Overline(text: month).padding(.horizontal, Fit.gutter)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                    ForEach(items, id: \.persistentModelID) { photo in
                        PhotoTile(photo: photo, selected: compareSelection.contains(photo.persistentModelID))
                            .onTapGesture { tap(photo) }
                            .contextMenu {
                                Button { editing = photo } label: { Label("Edit details", systemImage: "pencil") }
                                Button(role: .destructive) { delete(photo) } label: { Label("Delete", systemImage: "trash") }
                            }
                    }
                }
                .padding(.horizontal, Fit.gutter)
            }
        }
    }

    private func tap(_ photo: ProgressPhoto) {
        guard compareMode else { viewing = photo; return }
        let id = photo.persistentModelID
        if let idx = compareSelection.firstIndex(of: id) {
            compareSelection.remove(at: idx)
            return
        }
        compareSelection.append(id)
        if compareSelection.count == 2,
           let a = photos.first(where: { $0.persistentModelID == compareSelection[0] }),
           let b = photos.first(where: { $0.persistentModelID == compareSelection[1] }) {
            // Older on the left.
            comparePair = a.takenAt <= b.takenAt ? ComparePair(first: a, second: b) : ComparePair(first: b, second: a)
            compareSelection = []
            compareMode = false
        }
    }

    private func addPhoto(_ image: UIImage, takenAt: Date, pose: PhotoPose, notes: String?) {
        do {
            let name = try ProgressPhotoStore.save(image)
            modelContext.insert(ProgressPhoto(takenAt: takenAt, fileName: name, pose: pose, notes: notes))
            try modelContext.save()
        } catch {
            saveError = error.localizedDescription
        }
    }

    private func delete(_ photo: ProgressPhoto) {
        ProgressPhotoStore.delete(fileName: photo.fileName)
        modelContext.delete(photo)
        try? modelContext.save()
        if viewing?.persistentModelID == photo.persistentModelID { viewing = nil }
    }
}

extension UIImage: @retroactive Identifiable {
    public var id: ObjectIdentifier { ObjectIdentifier(self) }
}

// MARK: - Tile

private struct PhotoTile: View {
    let photo: ProgressPhoto
    let selected: Bool
    @State private var thumb: UIImage?

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Color.fitSurfaceHigh
                .aspectRatio(3.0 / 4.0, contentMode: .fit)
                .overlay {
                    if let thumb {
                        Image(uiImage: thumb).resizable().scaledToFill()
                    }
                }
                .clipped()
            LinearGradient(colors: [.clear, .black.opacity(0.55)], startPoint: .center, endPoint: .bottom)
            VStack(alignment: .leading, spacing: 1) {
                Text(photo.takenAt.formatted(.dateTime.day().month(.abbreviated)))
                    .font(.caption2.weight(.semibold))
                Text(photo.pose.displayName)
                    .font(.caption2)
                    .opacity(0.85)
            }
            .foregroundStyle(.white)
            .padding(6)
        }
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(selected ? Color.fitPrimary : Color.clear, lineWidth: 3)
        )
        .contentShape(Rectangle())
        .task(id: photo.fileName) {
            let name = photo.fileName
            thumb = await Task.detached { ProgressPhotoStore.thumbnail(fileName: name) }.value
        }
        .accessibilityLabel("\(photo.pose.displayName) photo, \(photo.takenAt.formatted(date: .abbreviated, time: .omitted))")
    }
}

// MARK: - Details sheet

private struct PhotoDetailsSheet: View {
    let image: UIImage?
    let existing: ProgressPhoto?
    let onSave: (Date, PhotoPose, String?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var date = Date()
    @State private var pose: PhotoPose = .front
    @State private var notes = ""

    var body: some View {
        NavigationStack {
            Form {
                if let image {
                    Section {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(maxHeight: 260)
                            .frame(maxWidth: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .listRowBackground(Color.clear)
                    }
                }
                Section {
                    DatePicker("Date", selection: $date, in: ...Date(), displayedComponents: .date)
                    Picker("Pose", selection: $pose) {
                        ForEach(PhotoPose.allCases) { Text($0.displayName).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                Section("Notes") {
                    TextField("Optional", text: $notes, axis: .vertical)
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.fitBackground)
            .navigationTitle(existing == nil ? "New photo" : "Edit photo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let trimmed = notes.trimmingCharacters(in: .whitespacesAndNewlines)
                        onSave(date, pose, trimmed.isEmpty ? nil : trimmed)
                        dismiss()
                    }
                }
            }
            .onAppear {
                guard let existing else { return }
                date = existing.takenAt
                pose = existing.pose
                notes = existing.notes ?? ""
            }
        }
    }
}

// MARK: - Viewer

private struct PhotoViewer: View {
    let photos: [ProgressPhoto]
    let start: ProgressPhoto
    let onEdit: (ProgressPhoto) -> Void
    let onDelete: (ProgressPhoto) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var current: PersistentIdentifier?
    @State private var confirmDelete = false

    private var currentPhoto: ProgressPhoto? { photos.first { $0.persistentModelID == current } }

    var body: some View {
        NavigationStack {
            TabView(selection: $current) {
                ForEach(photos, id: \.persistentModelID) { photo in
                    VStack(spacing: 12) {
                        if let image = ProgressPhotoStore.load(fileName: photo.fileName) {
                            Image(uiImage: image)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                        } else {
                            Image(systemName: "photo").font(.largeTitle).foregroundStyle(.secondary)
                        }
                        VStack(spacing: 4) {
                            Text(photo.takenAt.formatted(date: .long, time: .omitted)).font(.subheadline.weight(.semibold))
                            Text(photo.pose.displayName).font(.caption).opacity(0.7)
                            if let notes = photo.notes {
                                Text(notes).font(.caption).multilineTextAlignment(.center).padding(.horizontal)
                            }
                        }
                        .foregroundStyle(.white)
                        .padding(.bottom, 24)
                    }
                    .tag(Optional(photo.persistentModelID))
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .background(Color.black.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Done") { dismiss() } }
                ToolbarItemGroup(placement: .primaryAction) {
                    Button { if let p = currentPhoto { dismiss(); onEdit(p) } } label: { Image(systemName: "pencil") }
                        .accessibilityLabel("Edit details")
                    Button(role: .destructive) { confirmDelete = true } label: { Image(systemName: "trash") }
                        .accessibilityLabel("Delete photo")
                }
            }
            .confirmationDialog("Delete this photo?", isPresented: $confirmDelete, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    if let p = currentPhoto { onDelete(p); dismiss() }
                }
            } message: { Text("The file is removed from this device. This can't be undone.") }
        }
        .onAppear { if current == nil { current = start.persistentModelID } }
    }
}

// MARK: - Compare

private struct ComparePhotosView: View {
    let first: ProgressPhoto
    let second: ProgressPhoto
    @Environment(\.dismiss) private var dismiss

    private var daysApart: Int {
        Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: first.takenAt),
                                        to: Calendar.current.startOfDay(for: second.takenAt)).day ?? 0
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                HStack(spacing: 8) {
                    side(first)
                    side(second)
                }
                Text(daysApart == 0 ? "Same day" : "\(daysApart) days apart")
                    .font(.subheadline.weight(.semibold))
                Spacer()
            }
            .padding()
            .fitScreenBackground()
            .navigationTitle("Compare")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }
    }

    private func side(_ photo: ProgressPhoto) -> some View {
        VStack(spacing: 6) {
            // The frame comes from the clear base; the image is an overlay so a
            // landscape photo can't push the column wider than its half.
            Color.fitSurfaceHigh
                .aspectRatio(3.0 / 4.0, contentMode: .fit)
                .overlay {
                    if let image = ProgressPhotoStore.load(fileName: photo.fileName) {
                        Image(uiImage: image).resizable().scaledToFill()
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            Text(photo.takenAt.formatted(date: .abbreviated, time: .omitted)).font(.caption.weight(.semibold))
            Text(photo.pose.displayName).font(.caption2).foregroundStyle(.secondary)
        }
    }
}

// MARK: - Camera

/// Thin wrapper so the system camera can be presented from SwiftUI.
private struct CameraPicker: UIViewControllerRepresentable {
    let onFinish: (UIImage?) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraCaptureMode = .photo
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onFinish: onFinish) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let onFinish: (UIImage?) -> Void
        init(onFinish: @escaping (UIImage?) -> Void) { self.onFinish = onFinish }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            onFinish(info[.originalImage] as? UIImage)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onFinish(nil)
        }
    }
}
