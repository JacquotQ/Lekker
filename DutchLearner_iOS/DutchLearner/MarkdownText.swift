import SwiftUI

/// Renders a Markdown string using AttributedString (iOS 15+).
/// Falls back to plain text if parsing fails.
struct MarkdownText: View {
    let text: String

    var body: some View {
        if let attr = try? AttributedString(
            markdown: text,
            options: .init(
                allowsExtendedAttributes: true,
                interpretedSyntax: .inlineOnlyPreservingWhitespace
            )
        ) {
            // Full markdown for inline elements; block elements via Text
            textView(attr)
        } else {
            Text(text)
                .font(.system(size: 15))
                .foregroundColor(.primary)
        }
    }

    @ViewBuilder
    private func textView(_ attr: AttributedString) -> some View {
        // SwiftUI Text + AttributedString handles bold, italic, code inline.
        // For full block-level markdown (tables, headers, blockquotes) we do a
        // simple manual split so it renders nicely in SwiftUI.
        let lines = text.components(separatedBy: "\n")
        VStack(alignment: .leading, spacing: 4) {
            ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                lineView(line)
            }
        }
    }

    @ViewBuilder
    private func lineView(_ line: String) -> some View {
        if line.hasPrefix("## ") {
            // ── H2: 主单词标题 + 发音按钮 ──
            let title = String(line.dropFirst(3))
            // 提取单词（取 · 或 / 之前的部分）
            let speakWord = title
                .components(separatedBy: "·").first?
                .components(separatedBy: "/").first?
                .trimmingCharacters(in: .whitespaces) ?? title
            HStack(alignment: .center, spacing: 8) {
                if let attr = try? AttributedString(
                    markdown: title,
                    options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)
                ) {
                    Text(attr)
                        .font(.system(size: 22, weight: .heavy))
                        .foregroundColor(Color.accentOrange)
                } else {
                    Text(title)
                        .font(.system(size: 22, weight: .heavy))
                        .foregroundColor(Color.accentOrange)
                }
                if !speakWord.isEmpty {
                    SpeakButton(text: speakWord, iconSize: 20)
                }
            }
            .padding(.bottom, 4)
        } else if line.hasPrefix("### ") {
            Text(String(line.dropFirst(4)))
                .font(.system(size: 15, weight: .bold))
                .padding(.top, 10)
        } else if line.hasPrefix("> ") {
            // ── Blockquote 概要行 + 发音按钮 ──
            let summary = String(line.dropFirst(2))
            HStack(alignment: .top, spacing: 6) {
                Text(summary)
                    .font(.system(size: 14))
                    .italic()
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                SpeakButton(text: summary, iconSize: 14)
            }
            .padding(.leading, 10)
            .overlay(Rectangle().frame(width: 3).foregroundColor(Color.accentOrange), alignment: .leading)
        } else if line.hasPrefix("---") || line.hasPrefix("===") {
            Divider().padding(.vertical, 4)
        } else if line.hasPrefix("| ") || line.hasPrefix("|---") {
            // Table row — simple rendering
            tableRowView(line)
        } else if line.hasPrefix("- **") || line.hasPrefix("- ") {
            HStack(alignment: .top, spacing: 6) {
                Text("•").foregroundColor(Color.accentOrange).font(.system(size: 15))
                renderInline(String(line.dropFirst(2)))
            }
        } else if line.trimmingCharacters(in: .whitespaces).isEmpty {
            Spacer().frame(height: 4)
        } else {
            renderInline(line)
        }
    }

    @ViewBuilder
    private func renderInline(_ line: String) -> some View {
        if let attr = try? AttributedString(
            markdown: line,
            options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        ) {
            Text(attr)
                .font(.system(size: 15))
                .foregroundColor(.primary)
                .fixedSize(horizontal: false, vertical: true)
        } else {
            Text(line)
                .font(.system(size: 15))
                .foregroundColor(.primary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    @ViewBuilder
    private func tableRowView(_ line: String) -> some View {
        // Skip header-separator rows (e.g. |---|---|)
        if line.contains("---") { EmptyView() } else {
            let cells = line
                .trimmingCharacters(in: .init(charactersIn: "| "))
                .components(separatedBy: "|")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }

            // Detect whether this is a table header row (all-bold cells)
            let isHeader = cells.allSatisfy { $0.hasPrefix("**") && $0.hasSuffix("**") }

            HStack(alignment: .top, spacing: 0) {
                ForEach(Array(cells.enumerated()), id: \.offset) { idx, cell in
                    // First column of data rows = Dutch text → add speak button
                    let isFirstDataCell = idx == 0 && !isHeader
                    // Strip markdown bold markers for plain speak text
                    let plainText = cell
                        .replacingOccurrences(of: "**", with: "")
                        .trimmingCharacters(in: .whitespaces)

                    HStack(spacing: 4) {
                        if let attr = try? AttributedString(
                            markdown: cell,
                            options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)
                        ) {
                            Text(attr)
                                .font(.system(size: 13, weight: isHeader ? .semibold : .regular))
                                .foregroundColor(isHeader ? Color.accentOrange : .primary)
                                .fixedSize(horizontal: false, vertical: true)
                        } else {
                            Text(cell)
                                .font(.system(size: 13, weight: isHeader ? .semibold : .regular))
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        if isFirstDataCell && !plainText.isEmpty {
                            SpeakButton(text: plainText, iconSize: 12)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 7)
                    .background(isHeader ? Color.accentOrange.opacity(0.12) : Color.clear)

                    if idx < cells.count - 1 {
                        Divider()
                    }
                }
            }
            .background(Color(.systemGray6))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(.systemGray4), lineWidth: 0.5))
        }
    }
}
