import SwiftUI

struct MovementAnimationView: View {
    let pattern: MovementPattern
    @State private var phase: CGFloat = 0

    var body: some View {
        TimelineView(.animation(minimumInterval: 1/30)) { timeline in
            Canvas { context, size in
                let t = computePhase(timeline.date)
                drawPattern(context: context, size: size, phase: t, pattern: pattern)
            }
        }
        .overlay(alignment: .bottom) {
            Text(pattern.rawValue)
                .font(.caption2)
                .foregroundColor(.secondary)
                .padding(.bottom, 4)
        }
    }

    private func computePhase(_ date: Date) -> CGFloat {
        let seconds = date.timeIntervalSinceReferenceDate
        let cycle = seconds.truncatingRemainder(dividingBy: 2.0) / 2.0
        return cycle < 0.5 ? CGFloat(cycle * 2) : CGFloat(1 - (cycle - 0.5) * 2)
    }

    private func drawPattern(context: GraphicsContext, size: CGSize, phase: CGFloat, pattern: MovementPattern) {
        let body = Color.primary.opacity(0.6)
        let weight = Color.teal
        let accent = Color.accentColor

        switch pattern {
        case .horizontalPush:
            drawStickFigure(context: context, size: size, body: body, armAngle: phase)
            let wy = size.height * 0.25 + (1 - phase) * size.height * 0.15
            drawBar(context: context, cx: size.width * 0.4, y: wy, width: size.width * 0.3, color: weight)
            drawArrow(context: context, x: size.width * 0.4, y: size.height * 0.12, up: true, color: accent)

        case .horizontalPull:
            drawStickFigure(context: context, size: size, body: body, torsoAngle: 0.2, armAngle: phase, legBend: 0.1)
            let wy = size.height * 0.7 - phase * size.height * 0.15
            drawBar(context: context, cx: size.width * 0.5, y: wy, width: size.width * 0.3, color: weight)

        case .verticalPush:
            drawStickFigure(context: context, size: size, body: body, armAngle: phase)
            let wy = size.height * 0.35 - phase * size.height * 0.2
            drawBar(context: context, cx: size.width * 0.5, y: wy, width: size.width * 0.35, color: weight)
            drawArrow(context: context, x: size.width * 0.5, y: size.height * 0.08, up: true, color: accent)

        case .verticalPull:
            let bodyY = size.height * 0.9 - phase * size.height * 0.2
            drawStickFigure(context: context, size: size, body: body, baseY: bodyY, armAngle: phase)
            drawBar(context: context, cx: size.width * 0.5, y: size.height * 0.05, width: size.width * 0.5, color: weight)

        case .squat:
            let baseY = size.height * 0.9 + phase * size.height * 0.05
            drawStickFigure(context: context, size: size, body: body, baseY: baseY, legBend: phase)
            let wy = baseY - size.height * 0.55 + phase * size.height * 0.15
            drawBar(context: context, cx: size.width * 0.5, y: wy, width: size.width * 0.35, color: weight)

        case .hipHinge:
            drawStickFigure(context: context, size: size, body: body, torsoAngle: phase * 0.5, legBend: phase * 0.1)
            let wy = size.height * 0.5 + phase * size.height * 0.2
            drawBar(context: context, cx: size.width * 0.5, y: wy, width: size.width * 0.3, color: weight)

        case .curl:
            drawStickFigure(context: context, size: size, body: body, armAngle: phase)
            let wy = size.height * 0.65 - phase * size.height * 0.25
            drawDot(context: context, x: size.width * 0.35, y: wy, color: weight)
            drawDot(context: context, x: size.width * 0.65, y: wy, color: weight)

        case .ext:
            drawStickFigure(context: context, size: size, body: body, armAngle: 1 - phase)
            let wy = size.height * 0.25 + phase * size.height * 0.2
            drawBar(context: context, cx: size.width * 0.5, y: wy, width: size.width * 0.15, color: weight)

        case .lateralRaise:
            drawStickFigure(context: context, size: size, body: body)
            let spread = phase * size.width * 0.3
            let ay = size.height * 0.4 - phase * size.height * 0.1
            drawDot(context: context, x: size.width * 0.5 - spread - size.width * 0.1, y: ay, color: weight)
            drawDot(context: context, x: size.width * 0.5 + spread + size.width * 0.1, y: ay, color: weight)

        case .isolation:
            drawStickFigure(context: context, size: size, body: body, armAngle: phase)
            let wy = size.height * 0.65 - phase * size.height * 0.25
            drawDot(context: context, x: size.width * 0.35, y: wy, color: weight)
            drawDot(context: context, x: size.width * 0.65, y: wy, color: weight)

        case .staticHold:
            let y = size.height * 0.6
            let path = Path { p in
                p.move(to: CGPoint(x: size.width * 0.25, y: y))
                p.addLine(to: CGPoint(x: size.width * 0.75, y: y))
            }
            context.stroke(path, with: .color(body), lineWidth: 6)
            let pulseR = 20 + phase * 8
            context.stroke(Circle().path(in: CGRect(x: size.width * 0.5 - pulseR, y: y - pulseR, width: pulseR * 2, height: pulseR * 2)), with: .color(accent.opacity(0.4 + phase * 0.3)), lineWidth: 2)

        case .carry:
            let walkX = phase * size.width * 0.1
            drawStickFigure(context: context, size: size, body: body, cx: size.width * 0.4 + walkX, legBend: phase * 0.15)
            let cx = size.width * 0.4 + walkX
            drawDot(context: context, x: cx - size.width * 0.15, y: size.height * 0.65, color: weight, radius: 8)
            drawDot(context: context, x: cx + size.width * 0.15, y: size.height * 0.65, color: weight, radius: 8)
        }
    }

    // MARK: - Drawing helpers

    private func drawStickFigure(context: GraphicsContext, size: CGSize, body: Color,
                                  cx: CGFloat? = nil, baseY: CGFloat? = nil,
                                  torsoAngle: CGFloat = 0, armAngle: CGFloat = 0, legBend: CGFloat = 0) {
        let centerX = cx ?? size.width * 0.5
        let base = baseY ?? size.height * 0.85
        let headR: CGFloat = size.width * 0.04
        let torsoLen = size.height * 0.3
        let limbLen = size.height * 0.25
        let sw: CGFloat = 3

        let headY = base - torsoLen - headR * 2
        context.stroke(Circle().path(in: CGRect(x: centerX - headR, y: headY - headR, width: headR * 2, height: headR * 2)), with: .color(body), lineWidth: sw)

        let torsoTop = headY + headR
        let torsoBottom = torsoTop + torsoLen
        let torsoOffX = torsoAngle * size.width * 0.1
        drawLine(context: context, from: CGPoint(x: centerX, y: torsoTop), to: CGPoint(x: centerX + torsoOffX, y: torsoBottom), color: body, width: sw)

        let shoulderY = torsoTop + torsoLen * 0.15
        let armEndY = shoulderY + limbLen * (1 - armAngle * 0.7)
        let armSpread = limbLen * 0.6 * (1 - armAngle * 0.5)
        drawLine(context: context, from: CGPoint(x: centerX, y: shoulderY), to: CGPoint(x: centerX - armSpread, y: armEndY), color: body, width: sw)
        drawLine(context: context, from: CGPoint(x: centerX, y: shoulderY), to: CGPoint(x: centerX + armSpread, y: armEndY), color: body, width: sw)

        let kneeSpread = limbLen * 0.3
        let kneeY = torsoBottom + limbLen * (1 - legBend * 0.4)
        drawLine(context: context, from: CGPoint(x: centerX + torsoOffX, y: torsoBottom), to: CGPoint(x: centerX - kneeSpread + torsoOffX, y: kneeY), color: body, width: sw)
        drawLine(context: context, from: CGPoint(x: centerX + torsoOffX, y: torsoBottom), to: CGPoint(x: centerX + kneeSpread + torsoOffX, y: kneeY), color: body, width: sw)
    }

    private func drawLine(context: GraphicsContext, from: CGPoint, to: CGPoint, color: Color, width: CGFloat) {
        var path = Path()
        path.move(to: from)
        path.addLine(to: to)
        context.stroke(path, with: .color(color), style: StrokeStyle(lineWidth: width, lineCap: .round))
    }

    private func drawBar(context: GraphicsContext, cx: CGFloat, y: CGFloat, width: CGFloat, color: Color) {
        drawLine(context: context, from: CGPoint(x: cx - width / 2, y: y), to: CGPoint(x: cx + width / 2, y: y), color: color, width: 6)
    }

    private func drawDot(context: GraphicsContext, x: CGFloat, y: CGFloat, color: Color, radius: CGFloat = 6) {
        context.fill(Circle().path(in: CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)), with: .color(color))
    }

    private func drawArrow(context: GraphicsContext, x: CGFloat, y: CGFloat, up: Bool, color: Color) {
        let dir: CGFloat = up ? -1 : 1
        drawLine(context: context, from: CGPoint(x: x, y: y + dir * -10), to: CGPoint(x: x, y: y + dir * 10), color: color, width: 3)
        drawLine(context: context, from: CGPoint(x: x - 6, y: y + dir * 6), to: CGPoint(x: x, y: y + dir * 10), color: color, width: 3)
        drawLine(context: context, from: CGPoint(x: x + 6, y: y + dir * 6), to: CGPoint(x: x, y: y + dir * 10), color: color, width: 3)
    }
}
