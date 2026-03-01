import SwiftUI

struct ContentView: View {
    @EnvironmentObject var store: AppStore
    @State private var selectedTab = 0
    @State private var showSettings = false

    var body: some View {
        TabView(selection: $selectedTab) {
            SearchView(showSettings: $showSettings)
                .tabItem { Label(String(localized: "tab.search"), systemImage: "magnifyingglass") }
                .tag(0)

            HistoryView()
                .tabItem { Label(String(localized: "tab.history"), systemImage: "clock") }
                .tag(1)

            FavoritesView()
                .tabItem { Label(String(localized: "tab.favorites"), systemImage: "star") }
                .tag(2)

            FlashcardView()
                .tabItem { Label(String(localized: "tab.flashcards"), systemImage: "rectangle.on.rectangle.angled") }
                .tag(3)
                .badge(store.dueCount > 0 ? store.dueCount : 0)
        }
        .tint(Color.accentOrange)
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }
}
