import SwiftUI
import Shared

public struct ComposeView: UIViewControllerRepresentable {
    public init() {}

    public func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    public func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
