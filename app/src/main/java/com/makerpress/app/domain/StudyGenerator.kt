package com.makerpress.app.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable enum class StudyTopic { ARITHMETIC, GEOMETRY, LINEAR, QUADRATIC, PHYSICS, MIXED;
    fun label(l: AppLanguage) = l.text(
        listOf("Aritmética", "Geometria", "Função afim", "Bhaskara / quadráticas", "Física", "Revisão mista")[ordinal],
        listOf("Arithmetic", "Geometry", "Linear functions", "Quadratic formula", "Physics", "Mixed review")[ordinal])
}
@Serializable enum class GraphMode { RANDOM, DRAW, READ;
    fun label(l: AppLanguage) = l.text(listOf("Alternar", "Desenhar gráfico", "Ler gráfico")[ordinal], listOf("Alternate", "Draw graph", "Read graph")[ordinal])
}
@Serializable data class StudyFigure(
    val kind: String, val values: List<Double> = emptyList(), val labels: List<String> = emptyList(), val plotted: Boolean = false
)
@Serializable data class StudyQuestion(
    val prompt: String, val solution: String, val skill: String, val figure: StudyFigure? = null
)
@Serializable data class StudySheet(
    val questions: List<StudyQuestion>, val seed: Long, val topic: StudyTopic,
    val difficulty: Difficulty, val graphMode: GraphMode, val language: AppLanguage,
    val operation: Operation = Operation.MIXED
)

object StudyGenerator {
    fun generate(topic: StudyTopic, difficulty: Difficulty, count: Int, seed: Long,
                 language: AppLanguage, mode: GraphMode = GraphMode.RANDOM, operation: Operation = Operation.MIXED): StudySheet {
        require(count in listOf(5, 10, 15))
        val r = Random(seed)
        fun t(pt: String, en: String) = language.text(pt, en)
        val max = listOf(6, 10, 16)[difficulty.ordinal]
        fun n() = r.nextInt(2, max + 1)
        fun num(v: Double): String = if (v == v.toInt().toDouble()) v.toInt().toString() else
            String.format(if (language == AppLanguage.PT_BR) java.util.Locale.forLanguageTag("pt-BR") else java.util.Locale.US, "%.2f", v)
        val cycle = listOf(StudyTopic.GEOMETRY, StudyTopic.LINEAR, StudyTopic.QUADRATIC, StudyTopic.PHYSICS, StudyTopic.ARITHMETIC).shuffled(r)
        val questions = (0 until count).map { i ->
            val chosen = if (topic == StudyTopic.MIXED) cycle[i % cycle.size] else topic
            val read = when (mode) { GraphMode.READ -> true; GraphMode.DRAW -> false; GraphMode.RANDOM -> i % 2 == 1 }
            when (chosen) {
                StudyTopic.LINEAR -> {
                    val a = (if (difficulty == Difficulty.EASY) listOf(1, 2) else listOf(-3, -2, -1, 1, 2, 3)).random(r)
                    val b = r.nextInt(-3, 4)
                    val equation = "y = ${a}x ${if (b < 0) "− ${-b}" else "+ $b"}"
                    StudyQuestion(if (read) t("A reta passa pelos pontos marcados. Determine a função y = ax + b e explique se é crescente ou decrescente.", "The line passes through the marked points. Find y = ax + b and state whether it increases or decreases.")
                        else t("Desenhe $equation. Marque os pontos com x = 0 e x = 1 e indique a inclinação.", "Draw $equation. Mark the points at x = 0 and x = 1 and give the slope."),
                        t("a = (y₂ − y₁)/(x₂ − x₁) = $a; b = $b. $equation. Pontos: (0; $b), (1; ${a+b}). ", "a = (y₂ − y₁)/(x₂ − x₁) = $a; b = $b. $equation. Points: (0, $b), (1, ${a+b}). ") +
                            t(if (a > 0) "Crescente." else "Decrescente.", if (a > 0) "Increasing." else "Decreasing."),
                        "EM13MAT401", StudyFigure("graph", listOf(0.0, a.toDouble(), b.toDouble()), plotted = read))
                }
                StudyTopic.QUADRATIC -> {
                    val x1 = r.nextInt(-3, 0); val x2 = r.nextInt(1, 4)
                    val a = if (difficulty == Difficulty.HARD && r.nextBoolean()) -1 else 1
                    val b = -a * (x1 + x2); val c = a * x1 * x2; val delta = b*b - 4*a*c
                    val vx = -b / (2.0*a); val vy = a*vx*vx + b*vx + c
                    val equation = "y = ${a}x² ${if (b < 0) "− ${-b}" else "+ $b"}x ${if (c < 0) "− ${-c}" else "+ $c"}"
                    StudyQuestion(if (read) t("A parábola tem as raízes marcadas e passa pelo ponto P. Determine y = ax² + bx + c e o vértice.", "The parabola has the marked roots and passes through P. Find y = ax² + bx + c and its vertex.")
                        else t("$equation. Use Bhaskara para achar as raízes e desenhe a parábola. Marque o vértice.", "$equation. Use the quadratic formula to find its roots and draw the parabola. Mark the vertex."),
                        "Δ = b² − 4ac = $delta\nx = (−b ± √Δ)/(2a) = $x1; $x2\n$equation\n" + t("Vértice", "Vertex") + ": (${num(vx)}; ${num(vy)}). " +
                            if (read) t("Use y = a(x − x₁)(x − x₂) e substitua P para obter a = $a.", "Use y = a(x − x₁)(x − x₂) and substitute P to obtain a = $a.") else "",
                        "EM13MAT302 / EM13MAT402", StudyFigure("graph", listOf(a.toDouble(), b.toDouble(), c.toDouble()), plotted = read))
                }
                StudyTopic.GEOMETRY -> when (i % 4) {
                    0 -> { val w = n(); val h = n()
                        StudyQuestion(t("Uma horta retangular mede $w m por $h m. Calcule a área e o perímetro.", "A rectangular garden measures $w m by $h m. Find its area and perimeter."),
                            "A = $w × $h = ${w*h} m²; P = 2($w + $h) = ${2*(w+h)} m.", "EM13MAT307", StudyFigure("rectangle", labels = listOf("$w m", "$h m"))) }
                    1 -> { val b = n()*2; val h = n()
                        StudyQuestion(t("Uma placa triangular tem base $b cm e altura perpendicular $h cm. Qual é a área?", "A triangular sign has base $b cm and perpendicular height $h cm. Find its area."),
                            "A = b × h / 2 = $b × $h / 2 = ${b*h/2} cm².", "EM13MAT307", StudyFigure("triangle", labels = listOf("$b cm", "$h cm"))) }
                    2 -> { val k = r.nextInt(1, 4); val a = 3*k; val b = 4*k
                        StudyQuestion(t("Uma rampa forma um triângulo retângulo com catetos $a m e $b m. Ache o comprimento da rampa.", "A ramp forms a right triangle with legs $a m and $b m. Find the ramp length."),
                            "c² = $a² + $b² = ${a*a+b*b}; c = ${5*k} m.", "EM13MAT308", StudyFigure("triangle", labels = listOf("$b m", "$a m"))) }
                    else -> { val w = n(); val h = n(); val d = n()
                        StudyQuestion(t("Uma caixa mede $w cm × $h cm × $d cm. Calcule o volume e a área total.", "A box measures $w cm × $h cm × $d cm. Find its volume and total surface area."),
                            "V = $w × $h × $d = ${w*h*d} cm³.\nA = 2($w × $h + $w × $d + $h × $d) = ${2*(w*h+w*d+h*d)} cm².", "EM13MAT309", StudyFigure("box", labels = listOf("$w cm", "$h cm", "$d cm"))) }
                }
                StudyTopic.PHYSICS -> when ((if(topic == StudyTopic.MIXED) i / 5 else i) % 5) {
                    0 -> { val v = n()*2; val time = n(); val start = n()*5
                        StudyQuestion(t("Um carro parte de s₀ = $start m com velocidade constante de $v m/s no sentido positivo. Determine s(t) e a posição após $time s.", "A car starts at s₀ = $start m with constant velocity $v m/s in the positive direction. Find s(t) and its position after $time s."),
                            "s(t) = $start + ${v}t (m); s($time) = ${start+v*time} m.", "EM13CNT101 / EM13MAT302", StudyFigure("car", labels = listOf("s₀ = $start m", "v = $v m/s →", "t = $time s"))) }
                    1 -> { val v1 = n(); val v2 = n(); val time = n(); val distance = (v1+v2)*time
                        StudyQuestion(t("Dois trens estão a $distance m um do outro e se aproximam com velocidades constantes de $v1 m/s e $v2 m/s. Trate-os como pontos. Em quanto tempo se encontram?", "Two trains are $distance m apart and approach each other at constant speeds of $v1 m/s and $v2 m/s. Treat them as points. When do they meet?"),
                            "t = d/(v₁ + v₂) = $distance/(${v1+v2}) = $time s.", "EM13CNT101 / EM13MAT302", StudyFigure("trains", labels = listOf("$v1 m/s →", "← $v2 m/s", "d = $distance m"))) }
                    2 -> { val time = r.nextInt(2, 5); val height = 5*time*time
                        StudyQuestion(t("Uma bola é solta do repouso a $height m do chão. Sem resistência do ar, com g = 10 m/s², calcule o tempo de queda e a velocidade ao tocar o chão.", "A ball is dropped from rest $height m above the ground. Ignore air resistance and use g = 10 m/s². Find fall time and impact speed."),
                            "h = gt²/2; t = √(2 × $height/10) = $time s. v = gt = ${10*time} m/s ↓.", "EM13CNT101", StudyFigure("ball", labels = listOf("h = $height m", "g = 10 m/s²", "v₀ = 0"))) }
                    3 -> { val mass = n(); val acceleration = n()
                        StudyQuestion(t("Um bloco de $mass kg está em uma mesa horizontal sem atrito. Uma força horizontal produz aceleração de $acceleration m/s². Ache a força e o peso (g = 10 m/s²).", "A $mass kg block is on a frictionless horizontal table. A horizontal force causes acceleration $acceleration m/s². Find the force and weight (g = 10 m/s²)."),
                            "F = ma = $mass × $acceleration = ${mass*acceleration} N →. P = mg = ${mass*10} N ↓.", "EM13CNT101", StudyFigure("weight", labels = listOf("m = $mass kg", "a = $acceleration m/s² →", "g = 10 m/s²"))) }
                    else -> { val mass = n()*2; val v = n()
                        StudyQuestion(t("Uma bola de $mass kg move-se a $v m/s. Determine a energia cinética e a quantidade de movimento.", "A $mass kg ball moves at $v m/s. Find kinetic energy and momentum."),
                            "E = mv²/2 = ${mass*v*v/2} J; p = mv = ${mass*v} kg·m/s.", "EM13CNT101", StudyFigure("ball", labels = listOf("m = $mass kg", "v = $v m/s →"))) }
                }
                else -> {
                    val q = MathGenerator.generate(operation, difficulty, 5, r.nextLong()).questions.first()
                    val symbol = when(q.operation) { Operation.ADD -> "+"; Operation.SUBTRACT -> "−"; Operation.MULTIPLY -> "×"; else -> "÷" }
                    StudyQuestion("${q.a} $symbol ${q.b} = ?", "${q.a} $symbol ${q.b} = ${q.answer}", t("Revisão de aritmética", "Arithmetic review"))
                }
            }
        }
        return StudySheet(questions, seed, topic, difficulty, mode, language, operation)
    }
}
