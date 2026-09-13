import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Compose draws the whole screen and applies the safe area itself, so that
            // the top bar and the bottom navigation reach the edges of the display.
            .ignoresSafeArea()
    }
}
