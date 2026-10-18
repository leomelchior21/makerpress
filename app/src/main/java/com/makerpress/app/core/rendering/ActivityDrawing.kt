package com.makerpress.app.core.rendering

import android.graphics.*
import com.makerpress.app.domain.*
import kotlin.math.*

/** Vector marks stay crisp when rasterized at the printer's native dot width. */
object ActivityDrawing {
    fun numberBond(c: Canvas, group: NumberBond, answers: Boolean) {
        val p=pen(); p.textSize=23f; p.textAlign=Paint.Align.CENTER
        group.numbers.forEachIndexed { i,n ->
            val angle=i*2*PI/group.numbers.size-PI/2
            val x=180+cos(angle).toFloat()*112; val y=145+sin(angle).toFloat()*112
            p.style=Paint.Style.STROKE; p.strokeWidth=if(answers && n==group.sum) 4f else 1.5f
            c.drawCircle(x,y,26f,p); p.style=Paint.Style.FILL
            c.drawText(n.toString(),x,y-(p.ascent()+p.descent())/2,p)
        }
        p.textSize=20f
        c.drawText(if(answers) "${group.first} + ${group.second}" else "? + ?",180f,142f,p)
        c.drawText(if(answers) "= ${group.sum}" else "= ?",180f,169f,p)
    }
    fun dots(c: Canvas, puzzle: DotPuzzle, answers: Boolean) {
        val p=pen(); p.textSize=15f
        if(answers) {
            p.style=Paint.Style.STROKE
            val path=Path()
            puzzle.points.forEachIndexed { i,pt -> if(i==0) path.moveTo(pt.x,pt.y) else path.lineTo(pt.x,pt.y) }
            path.close(); c.drawPath(path,p); p.style=Paint.Style.FILL
            return
        }
        val labels=mutableListOf<RectF>()
        val centerY=(puzzle.points.minOf { it.y }+puzzle.points.maxOf { it.y })/2
        val maxY=puzzle.points.maxOf { it.y }+38
        puzzle.points.forEachIndexed { i,pt ->
            c.drawCircle(pt.x,pt.y,2.5f,p)
            val number=(i+1).toString(); val w=p.measureText(number)
            val angle=atan2((pt.y-centerY).toDouble(),(pt.x-180).toDouble())
            val candidates=listOf(14f,22f,30f).flatMap { radius ->
                listOf(0.0,PI/4,-PI/4,PI/2,-PI/2,PI,-3*PI/4,3*PI/4).map { offset ->
                    val x=pt.x+cos(angle+offset).toFloat()*radius
                    val y=pt.y+sin(angle+offset).toFloat()*radius
                    RectF(x-w/2-2,y-9,x+w/2+2,y+9)
                }
            }
            val box=candidates.firstOrNull { r -> r.left>=2 && r.right<=358 && r.top>=0 && r.bottom<=maxY &&
                labels.none { RectF.intersects(it,r) } && puzzle.points.none { r.contains(it.x,it.y) || RectF.intersects(r,RectF(it.x-4,it.y-4,it.x+4,it.y+4)) }
            } ?: candidates.first()
            labels+=box; p.textAlign=Paint.Align.LEFT
            c.drawText(number,box.left+2,box.centerY()+5,p)
        }
    }
    private fun pen() = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 2f; textSize = 17f }
    private fun polygon(c: Canvas, p: Paint, x: Float, y: Float, radius: Float, sides: Int) {
        val path = Path()
        repeat(sides) { i -> val a = i * 2 * PI / sides - PI/2
            val px = x + cos(a).toFloat()*radius; val py = y + sin(a).toFloat()*radius
            if(i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }; path.close(); c.drawPath(path, p)
    }
    fun game(c: Canvas, level: PaperLevel) {
        val p = pen(); val step = 25f; val ox = 30f; val oy = 20f
        p.style = Paint.Style.STROKE; c.drawRoundRect(12f, 2f, 348f, 640f, 12f, 12f, p)
        for (y in 0 until level.rows) for(x in 0 until level.columns) {
            val cx = ox+x*step; val cy = oy+y*step
            p.style = Paint.Style.FILL
            when(level.cells[y*level.columns+x]) {
                0 -> c.drawCircle(cx, cy, 1.3f, p)
                1 -> if(level.game == PaperGame.GOLF) {
                    polygon(c,p,cx,cy-1,9f,3); c.drawRect(cx-1.5f,cy+4,cx+1.5f,cy+11,p)
                } else { p.style = Paint.Style.STROKE; polygon(c,p,cx,cy,10f,6) }
                2 -> if(level.game == PaperGame.GOLF) {
                    p.strokeWidth=1f
                    for(d in -6..6 step 6) c.drawLine(cx-8,cy+d+3,cx+8,cy+d-3,p)
                    p.strokeWidth=2f
                } else { polygon(c,p,cx,cy,9f,3); p.color=Color.WHITE; c.drawCircle(cx,cy,2f,p); p.color=Color.BLACK }
                3 -> if(level.game == PaperGame.GOLF) {
                    c.drawText("~",cx-6,cy+5,p); c.drawLine(cx-8,cy+8,cx+8,cy+8,p)
                } else { p.style=Paint.Style.STROKE; polygon(c,p,cx,cy,8f,4) }
            }
        }
        for((point,label) in listOf(level.start to "S", level.goal to "H")) {
            val x=ox+point.x*step; val y=oy+point.y*step
            p.style=Paint.Style.FILL; p.color=Color.WHITE; c.drawCircle(x,y,11f,p)
            p.color=Color.BLACK; p.style=Paint.Style.STROKE; c.drawCircle(x,y,11f,p)
            p.style=Paint.Style.FILL; p.typeface=Typeface.DEFAULT_BOLD; c.drawText(label,x-6,y+6,p)
        }
    }
    fun figure(c: Canvas, f: StudyFigure, answer: Boolean) {
        val p=pen()
        fun line(x1: Float,y1: Float,x2: Float,y2: Float) = c.drawLine(x1,y1,x2,y2,p)
        fun label(s:String,x:Float,y:Float) { p.style=Paint.Style.FILL; c.drawText(s,x,y,p) }
        fun rect(l:Float,t:Float,r:Float,b:Float) { p.style=Paint.Style.STROKE; c.drawRect(l,t,r,b,p); p.style=Paint.Style.FILL }
        if(f.kind == "graph") {
            val a=f.values[0]; val b=f.values[1]; val d=f.values[2]
            fun px(x:Double)= (180+x*36).toFloat()
            fun py(y:Double)= (164-y*11).toFloat()
            p.strokeWidth=1f
            for(x in -4..4) { p.pathEffect=DashPathEffect(floatArrayOf(1f,5f),0f); line(px(x.toDouble()),32f,px(x.toDouble()),296f); p.pathEffect=null; if(x!=0) label("$x",px(x.toDouble())-5,181f) }
            for(y in -12..12 step 2) { p.pathEffect=DashPathEffect(floatArrayOf(1f,5f),0f); line(36f,py(y.toDouble()),324f,py(y.toDouble())); p.pathEffect=null; if(y!=0) label("$y",4f,py(y.toDouble())+5) }
            p.strokeWidth=2f; line(36f,164f,338f,164f); line(180f,306f,180f,18f)
            label("x",339f,160f); label("y",187f,20f); label("0",166f,181f)
            if(f.plotted || answer) {
                c.save(); c.clipRect(36f,32f,324f,296f)
                val path=Path()
                for(i in 0..320) { val x = -4.0+i/40.0; val y=a*x*x+b*x+d
                    if(i==0) path.moveTo(px(x),py(y)) else path.lineTo(px(x),py(y)) }
                p.style=Paint.Style.STROKE; p.strokeWidth=2.5f; c.drawPath(path,p); p.style=Paint.Style.FILL
                val points=if(a==0.0) listOf(0.0 to d,1.0 to b+d) else {
                    val delta=b*b-4*a*d
                    listOf((-b-sqrt(delta))/(2*a) to 0.0,(-b+sqrt(delta))/(2*a) to 0.0,0.0 to d)
                }
                points.forEach { (x,y)->c.drawCircle(px(x),py(y),4f,p) }; c.restore()
                p.textSize=16f
                fun n(v:Double)=if(v==0.0) "0" else v.toInt().toString()
                label(if(a==0.0) "A(0; ${n(d)})   B(1; ${n(b+d)})" else "x₁=${n(points[0].first)}; x₂=${n(points[1].first)}; P(0; ${n(d)})",18f,332f)
            }
            return
        }
        when(f.kind) {
            "rectangle" -> rect(80f,30f,260f,105f)
            "triangle" -> { line(80f,105f,260f,105f); line(80f,105f,80f,25f); line(80f,25f,260f,105f); rect(80f,94f,91f,105f) }
            "box" -> { rect(80f,40f,240f,110f); line(80f,40f,110f,15f); line(240f,40f,270f,15f); line(110f,15f,270f,15f); line(270f,15f,270f,85f); line(270f,85f,240f,110f) }
            "car", "trains" -> {
                val positions=if(f.kind=="trains") listOf(35f,220f) else listOf(100f)
                positions.forEach { x -> rect(x,50f,x+90,82f); rect(x+15,30f,x+65,50f); c.drawCircle(x+20,86f,8f,p); c.drawCircle(x+70,86f,8f,p) }
                line(20f,98f,340f,98f)
            }
            "weight" -> { rect(115f,35f,220f,88f); line(30f,90f,330f,90f); line(220f,60f,310f,60f); line(310f,60f,300f,53f); line(310f,60f,300f,67f); label("F",290f,47f) }
            "ball" -> { p.style=Paint.Style.STROKE; c.drawCircle(150f,35f,14f,p); p.style=Paint.Style.FILL
                if(f.labels.any { it.startsWith("h =") }) { line(150f,55f,150f,95f); line(150f,95f,143f,85f); line(150f,95f,157f,85f) }
                else { line(170f,35f,250f,35f); line(250f,35f,240f,28f); line(250f,35f,240f,42f) }
                line(90f,108f,250f,108f) }
        }
        f.labels.forEachIndexed { i,s->label(s,16f,136f+i*23f) }
    }
}
