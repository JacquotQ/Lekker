import SwiftUI

@main
struct DutchLearnerApp: App {
    @StateObject private var store = AppStore()
    @State private var showSplash = true

    private var activeLocale: Locale {
        store.settings.language.locale ?? Locale.current
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                ContentView()
                    .environmentObject(store)

                if showSplash {
                    SplashView()
                        .transition(.opacity)
                        .zIndex(1)
                }
            }
            .environment(\.locale, activeLocale)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    withAnimation(.easeOut(duration: 0.6)) {
                        showSplash = false
                    }
                }
            }
        }
    }
}

// MARK: - Splash Screen
struct SplashView: View {
    @State private var opacity: Double = 0

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()

            Image("LaunchLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 260)
                .offset(x: 32)
                .opacity(opacity)
        }
        .onAppear {
            withAnimation(.easeIn(duration: 0.6)) {
                opacity = 1
            }
        }
    }
}
