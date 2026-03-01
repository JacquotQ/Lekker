import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) var dismiss

    @State private var useCache = true
    @State private var language: AppLanguage = .system
    @State private var showResetConfirm = false
    @State private var saved = false

    var body: some View {
        NavigationStack {
            Form {
                // Language
                Section {
                    Picker(String(localized: "settings.languagePicker"), selection: $language) {
                        ForEach(AppLanguage.allCases, id: \.self) { lang in
                            Text(lang.displayName).tag(lang)
                        }
                    }
                    .pickerStyle(.menu)
                } header: {
                    Text(String(localized: "settings.languageHeader"))
                } footer: {
                    Text(String(localized: "settings.languageFooter"))
                }

                // Cache
                Section {
                    Toggle(String(localized: "settings.cacheToggle"), isOn: $useCache)
                } header: {
                    Text(String(localized: "settings.performanceHeader"))
                } footer: {
                    Text(String(localized: "settings.cacheFooter"))
                }

                // Learning Profile
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        if store.profile.etymologyWeight == 1.0 &&
                           store.profile.storiesWeight == 1.0 &&
                           store.profile.connectionsWeight == 1.0 &&
                           store.profile.formalWeight == 1.0 {
                            Text(String(localized: "settings.noPreferences"))
                                .font(.callout)
                                .foregroundColor(.secondary)
                        } else {
                            ForEach(store.profile.summaryLines, id: \.self) { line in
                                Text(line).font(.callout)
                            }
                        }
                    }
                    .padding(.vertical, 4)

                    Button(role: .destructive) {
                        showResetConfirm = true
                    } label: {
                        Label(String(localized: "settings.resetProfile"), systemImage: "arrow.counterclockwise")
                    }
                } header: {
                    Text(String(localized: "settings.profileHeader"))
                } footer: {
                    Text(String(localized: "settings.profileFooter"))
                }

                // Stats
                Section(String(localized: "settings.statsHeader")) {
                    LabeledContent(String(localized: "settings.historyCount"), value: String(localized: "settings.historyValue \(store.history.count)"))
                    LabeledContent(String(localized: "settings.favoritesCount"), value: String(localized: "settings.favoritesValue \(store.favorites.count)"))
                }
            }
            .navigationTitle(String(localized: "settings.navTitle"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(String(localized: "settings.closeButton")) { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(saved ? String(localized: "settings.savedButton") : String(localized: "settings.saveButton")) {
                        saveAll()
                    }
                    .fontWeight(.semibold)
                    .tint(Color.accentOrange)
                }
            }
            .confirmationDialog(String(localized: "settings.resetConfirm"), isPresented: $showResetConfirm, titleVisibility: .visible) {
                Button(String(localized: "settings.resetButton"), role: .destructive) { store.resetProfile() }
                Button(String(localized: "settings.cancelButton"), role: .cancel) {}
            }
        }
        .onAppear {
            useCache      = store.settings.useCache
            language      = store.settings.language
        }
    }

    private func saveAll() {
        let s = AppSettings(
            useCache: useCache,
            language: language
        )
        store.saveSettings(s)
        saved = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            saved = false
            dismiss()
        }
    }
}
