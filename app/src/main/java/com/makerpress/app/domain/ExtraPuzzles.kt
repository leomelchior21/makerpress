package com.makerpress.app.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable data class NumberBond(val numbers: List<Int>, val first: Int, val second: Int, val sum: Int)
@Serializable data class KakoomaPuzzle(val groups: List<NumberBond>, val seed: Long, val difficulty: Difficulty, val language: AppLanguage)

object KakoomaGenerator {
    /** Enumerate equations, rather than just answer values: even two pairs with the same sum are ambiguous. */
    fun solutions(numbers: List<Int>): List<Triple<Int,Int,Int>> = buildList {
        for(i in numbers.indices) for(j in i+1 until numbers.size) for(k in numbers.indices)
            if(k != i && k != j && numbers[i]+numbers[j] == numbers[k]) add(Triple(i,j,k))
    }
    fun generate(difficulty: Difficulty, count: Int, seed: Long, language: AppLanguage): KakoomaPuzzle {
        require(count in listOf(5,10,15))
        val random=Random(seed); val size=listOf(5,7,9)[difficulty.ordinal]; val max=listOf(30,70,150)[difficulty.ordinal]
        val groups=mutableListOf<NumberBond>(); val seen=mutableSetOf<List<Int>>()
        repeat(count) {
            var result: NumberBond? = null
            repeat(100) {
                if(result == null) {
                    val a=random.nextInt(2,max/3); var b=random.nextInt(2,max/3)
                    if(b==a) b++
                    val numbers=mutableListOf(a,b,a+b)
                    for(candidate in (2..max).shuffled(random)) {
                        if(numbers.size == size) break
                        if(candidate !in numbers && solutions(numbers+candidate).size == 1) numbers += candidate
                    }
                    if(numbers.size == size && seen.add(numbers.sorted())) result=NumberBond(numbers.shuffled(random),a,b,a+b)
                }
            }
            groups += checkNotNull(result) { "Could not generate a unique number puzzle." }
        }
        return KakoomaPuzzle(groups,seed,difficulty,language)
    }
}

@Serializable enum class DotSubject { SURPRISE, ROCKET, FISH, CAT, BUTTERFLY, HOUSE, BOAT;
    fun label(l:AppLanguage) = l.text(listOf("Surpresa", "Foguete", "Peixe", "Gato", "Borboleta", "Casa", "Barco")[ordinal],
        listOf("Surprise", "Rocket", "Fish", "Cat", "Butterfly", "House", "Boat")[ordinal])
}
@Serializable data class DotPoint(val x: Float, val y: Float)
@Serializable data class DotPuzzle(val points: List<DotPoint>, val subject: DotSubject, val seed: Long, val difficulty: Difficulty, val language: AppLanguage, val requestedSubject: DotSubject = subject)

object DotGenerator {
    // Original continuous silhouettes in a 300 x 400 design space. No imported artwork.
    private val outlines = mapOf(
        DotSubject.ROCKET to listOf(150 to 15,200 to 85,200 to 250,265 to 310,265 to 355,185 to 325,175 to 380,150 to 345,125 to 380,115 to 325,35 to 355,35 to 310,100 to 250,100 to 85),
        DotSubject.FISH to listOf(25 to 200,75 to 135,130 to 100,190 to 130,240 to 175,285 to 115,275 to 200,285 to 285,240 to 225,190 to 270,130 to 300,75 to 265),
        DotSubject.CAT to listOf(60 to 330,55 to 160,35 to 60,115 to 110,185 to 110,265 to 60,245 to 160,240 to 330,190 to 375,110 to 375),
        DotSubject.BUTTERFLY to listOf(150 to 165,95 to 50,30 to 35,15 to 150,75 to 210,30 to 290,55 to 370,115 to 325,150 to 250,185 to 325,245 to 370,270 to 290,225 to 210,285 to 150,270 to 35,205 to 50),
        DotSubject.HOUSE to listOf(25 to 170,65 to 125,65 to 45,105 to 45,105 to 90,150 to 40,275 to 170,245 to 170,245 to 350,185 to 350,185 to 255,115 to 255,115 to 350,55 to 350,55 to 170),
        DotSubject.BOAT to listOf(20 to 280,140 to 280,140 to 40,30 to 250,140 to 250,160 to 250,270 to 250,160 to 65,160 to 280,280 to 280,235 to 355,65 to 355)
    )
    fun generate(subject: DotSubject, difficulty: Difficulty, seed: Long, language: AppLanguage): DotPuzzle {
        val random=Random(seed)
        val chosen=if(subject==DotSubject.SURPRISE) outlines.keys.toList().random(random) else subject
        val outline=outlines.getValue(chosen)
        val flip=random.nextBoolean(); val sx=random.nextDouble(.88,1.0).toFloat(); val sy=random.nextDouble(.88,1.0).toFloat()
        val points=mutableListOf<DotPoint>()
        outline.forEachIndexed { i,(x,y) ->
            val end=outline[(i+1)%outline.size]
            val length=kotlin.math.hypot((end.first-x).toDouble(),(end.second-y).toDouble())
            val segments=kotlin.math.ceil(length/listOf(95.0,65.0,45.0)[difficulty.ordinal]).toInt().coerceAtLeast(1)
            repeat(segments) { k ->
                val px=x+(end.first-x)*k.toFloat()/segments; val py=y+(end.second-y)*k.toFloat()/segments
                points += DotPoint(180 + (if(flip) 150-px else px-150)*sx,230+(py-200)*sy)
            }
        }
        // Vary the starting point and traversal while retaining an ordered, closed silhouette.
        val offset=random.nextInt(points.size)
        val top=points.minOf { it.y } - 30f
        val shifted=(points.drop(offset)+points.take(offset)).map { it.copy(y=it.y-top) }
        return DotPuzzle(if(random.nextBoolean()) shifted.reversed() else shifted,chosen,seed,difficulty,language,subject)
    }
}
